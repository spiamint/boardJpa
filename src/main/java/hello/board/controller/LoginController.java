package hello.board.controller;

import hello.board.auth.PrincipalDetails;
import hello.board.form.LoginForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    // 로그인 로직을 실행할 loginService -> spring security 에서 처리
//    private final LoginService loginService;

    // /login GET, 사용자가 login 버튼 누름
    // 왜 /loginForm 과 분리? => 사용자의 요청과 스프링 내부요청 구분 및 로그인 피드백. 메모참고 
    @GetMapping("/login")
    public String login(@ModelAttribute("loginForm") LoginForm loginForm) {
        return "login/loginForm";
    }

    // spring security 에서 잡은 로그인(임시)
    @RequestMapping("/loginForm")
    public String loginForm(@ModelAttribute("loginForm") LoginForm loginForm,
                            @RequestParam(value = "error", required = false) String error,
                            HttpServletRequest request, Model model) {
        log.info("SpringSecurity 에서 로그인 요청 catch");
        
        // 포워딩을 통해 들어옴
        String loginId = (String) request.getAttribute("loginId");
        String errorMessage = (String) request.getAttribute("errorMessage");

        // 로그인 에러 처리
        if ("true".equals(error)) {
            model.addAttribute("error", error);
            model.addAttribute("loginId", loginId);
            model.addAttribute("springSecurityErrorMessage", errorMessage);
        } else {
            model.addAttribute("isCatched", "true");
        }

        return "login/loginForm";
    }

    /**
     * OAuth 로그인 의 default success url,
     * 현재 Authentication 객체에 담긴 PrincipalDetails.Member.role 이 "ROLE_TEMP" 이면 회원가입요청
     * @return
     */
    @RequestMapping("/login/check")
    public String loginCheck(@AuthenticationPrincipal PrincipalDetails principalDetails,
                             RedirectAttributes redirectAttributes) {

        String role = principalDetails.getMember().getRole();

        if (role.equals("ROLE_TEMP")) {
            redirectAttributes.addFlashAttribute("alertMessage", "oauth2_add");
        } else {
            redirectAttributes.addFlashAttribute("alertMessage", "로그인 되었습니다.");
        }

        return new UrlBuilder().redirectHome();
    }


    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("/logout 로그아웃");
            // 세션 삭제
            session.invalidate();
        }
        return "redirect:/board/list";
    }


}
