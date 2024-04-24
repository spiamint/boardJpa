package hello.board.controller;

import hello.board.auth.PrincipalDetails;
import hello.board.domain.board.Board;
import hello.board.domain.criteria.Criteria;
import hello.board.domain.enums.Category;
import hello.board.domain.paging.PageMaker;
import hello.board.form.BoardEditForm;
import hello.board.form.BoardSaveForm;
import hello.board.service.BoardService;
import hello.board.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final ImageService imageService;

    @RequestMapping("/")
    public String index() {
        return "redirect:/boards";
    }

    @GetMapping("/boards/ex") // 에러 테스트용 
    public String errorTest(@RequestParam(required = false) String param) {
        throw new IllegalStateException("Example Exception with Param");
    }
    
    @RequestMapping("/boards/healthcheck") // 아마존 로드밸런서 응답용
    @ResponseBody
    public ResponseEntity<String> healthCheck() {
        return new ResponseEntity<>("ok", HttpStatus.OK);
    }

    @GetMapping("/boards")
    public String pagedList(Model model, @ModelAttribute("criteria") Criteria criteria) {

        // 글 가져오기
        List<Board> pagedBoard = boardService.findPagedBoard(criteria);

        // 총 글갯수 가져오기
        long countTotalBoard = boardService.countTotalBoard(criteria);

        // 페이징 할 정보 설정하기
        PageMaker pageMaker = new PageMaker(criteria, countTotalBoard);

        // 페이지메이커, 글 목록 모델에 넣기
        model.addAttribute("pageMaker", pageMaker);
        model.addAttribute("boardList", pagedBoard);

        return "board/list";
    }

    @GetMapping("/board/{boardId}")
    public String read(@PathVariable Long boardId, Model model,
                       @AuthenticationPrincipal PrincipalDetails principalDetails,
                       @RequestParam(required = false, value = "selected") String commentId,
                       @ModelAttribute("criteria") Criteria criteria) {

        Map result = boardService.readBoard(boardId);

//        log.info("{}{}{}{}", criteria.getCurrentPage(), criteria.getCategory(), criteria.getOption(), criteria.getKeyword());

        model.addAttribute("board", result.get("board"));
        model.addAttribute("commentList", result.get("commentList"));

        if (commentId != null) {
            // 내 댓글 클릭으로 요청
            model.addAttribute("selectedCommentId", commentId);
        }

        // comment 에서 member.username 사용해야됨.
        if (principalDetails != null) {
            model.addAttribute("member", principalDetails.getMember());
        }

        return "board/read";
    }

    @GetMapping("/board/write")
    public String writeForm(Model model, @ModelAttribute Criteria criteria) {
        model.addAttribute("board", Board.builder().build()); // 커맨드 객체 용 빈객체
        return "board/writeForm";
    }

    @PostMapping("/board/write")
    public String writeBoard(@AuthenticationPrincipal PrincipalDetails principalDetails,
                        @Validated @ModelAttribute("board") BoardSaveForm form,
                        BindingResult bindingResult, HttpServletRequest request,
                        @ModelAttribute("criteria") Criteria criteria,
                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // 검증 오류 발견
            log.info("/write POST bindingResult.hasErrors {}", bindingResult);
            return "board/writeForm";
        }

        // form -> board 맵핑
        Board board = Board.builder()
                .title(form.getTitle())
                .writer(form.getWriter())
                .content(form.getContent())
                .category(form.getCategory())
                .member(principalDetails.getMember())
                .regDate(LocalDateTime.now()).updateDate(LocalDateTime.now())
                .build();

        // 저장
        Board savedBoard = boardService.saveBoard(board);
        
        // 폼을 통해 실제로 들어온 이미지 확인
        String[] splittedImageName = form.getImageName().split(",");
//        for (String imageName : splittedImageName) {
//            log.info("/write imageName input by form = "+ imageName);
//        }

        // 이미지 동기화
        imageService.syncImage(savedBoard.getMember().getId(), savedBoard.getId(), splittedImageName);

        redirectAttributes.addFlashAttribute("alertMessage", "게시글이 등록되었습니다.");

        return new UrlBuilder("/board")
                .id(savedBoard.getId()).queryString(request.getQueryString()).buildRedirectUrl();
    }

    @GetMapping("/board/{boardId}/edit")
    public String editForm(@PathVariable Long boardId, HttpServletRequest request, Model model,
                           RedirectAttributes redirectAttributes,
                           @AuthenticationPrincipal PrincipalDetails principalDetails,
                           @ModelAttribute Criteria criteria) {
        Board findBoard = boardService.findById(boardId);

        if (!boardService.isSameWriter(principalDetails.getMember(), findBoard)) {
            // 현재 로그인한 유저 != 글 작성자
            redirectAttributes.addFlashAttribute("alertMessage", "수정하려는 글과 작성자가 다릅니다.");

            return new UrlBuilder("/board")
                    .id(boardId).queryString(request.getQueryString()).buildRedirectUrl();
        }

        model.addAttribute("board",findBoard);
        return "board/editForm";
    }

    // boardId 를 바꾸어 POST 할 수있음
    @PostMapping("/board/{boardId}/edit")
    public String editBoard(@PathVariable Long boardId,@Validated @ModelAttribute("board") BoardEditForm form,
                       BindingResult bindingResult, HttpServletRequest request, Model model,
                       @ModelAttribute Criteria criteria,
                       RedirectAttributes redirectAttributes) {

        log.info("isNotice {}", form.getIsNotice());

        // 공지사항 edit 의 경우, write 와 다르게 글에서 직접 /edit 로 들어올 수 있어 처리('admin' 닉으로 한번 검사하긴 함)
        if (form.getIsNotice() && !request.isUserInRole("ROLE_ADMIN")) {
            // 공지사항 수정 요청이 ADMIN 권한 없이 들어온 경우
            form.setCategory(Category.NOTICE); // 카테고리 복원
            model.addAttribute("board", form);
            bindingResult.rejectValue("category", "Unauthorized.category"); // 예외 발생
        }

        if (bindingResult.hasErrors()) {
            // 검증 오류 발생
            log.info("/edit POST bindingResult.hasErrors = {}", bindingResult);
            return "board/editForm";
        }

        log.info("board.categoryString = {}, criteria.categoryString = {}", form.getCategory(), criteria.getCategoryCode());

        Board updateParam = Board.builder()
                .title(form.getTitle())
                .writer(form.getWriter())
                .content(form.getContent())
                .category(form.getCategory())
                .updateDate(LocalDateTime.now()).build();


        // 업데이트
        Board updateBoard = boardService.updateBoard(boardId, updateParam);

        // 들어온 이미지 확인
        String[] splittedImageName = form.getImageName().split(",");
        
//        for (String imageName : splittedImageName) {
//            log.info("/edit imageName input by form = "+ imageName);
//        }

        // 이미지 동기화
        imageService.syncImage(updateBoard.getMember().getId(), updateBoard.getId(), splittedImageName);

        redirectAttributes.addFlashAttribute("alertMessage", "게시글이 수정되었습니다.");

        return new UrlBuilder("/board")
                .id(updateBoard.getId()).queryString(request.getQueryString()).buildRedirectUrl();
    }

    @PostMapping("/board/{boardId}/delete")
    public String delete(@PathVariable Long boardId,
                         @AuthenticationPrincipal PrincipalDetails principalDetails,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes,
                         @ModelAttribute Criteria criteria) {
        Board findBoard = boardService.findById(boardId);

        // 삭제 전 동일인물인지 검증
        if (!boardService.isSameWriter(principalDetails.getMember(), findBoard)) {
            redirectAttributes.addFlashAttribute("alertMessage", "삭제하려는 글과 작성자가 다릅니다.");
            return new UrlBuilder("/board")
                    .id(boardId).queryString(request.getQueryString()).buildRedirectUrl();

        }

        // board 삭제
        boardService.deleteBoard(boardId);

        redirectAttributes.addFlashAttribute("alertMessage", "글이 삭제 되었습니다.");

        return new UrlBuilder().uri("/boards").queryString(request.getQueryString()).buildRedirectUrl();

    }

}
