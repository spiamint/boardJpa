package hello.board.domain.repository.mybatis;

import hello.board.domain.comment.Comment;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
@Transactional
/**
 * 이 테스트가 제일 나은듯?
 */
class CommentMapperTest {

    @Autowired
    CommentMapper commentMapper;
    Comment testComment = new Comment(98L, "asdf", "댓글");
    Comment testComment2 = new Comment(98L, "asdfasdf", "댓글2");

    @Test
    void findByBoardId() {
        commentMapper.save(testComment);
        commentMapper.save(testComment2);

        List<Comment> savedCommentList = new ArrayList<Comment>();
        savedCommentList.add(testComment);
        savedCommentList.add(testComment2);

        List<Comment> findCommentList = commentMapper.findByBoardId(testComment.getBoardId());
        //                          from Index, To Index(size 넣으면 last Index)
        List<Comment> lastTwoCommentList = findCommentList.subList(findCommentList.size() - 2, findCommentList.size());

        log.info("size = {}", lastTwoCommentList.size());
        findCommentList.forEach(comment -> log.info("comment = {}", comment));

        // 기존 DB 에 있던 comment 는 test 결과에 영향주면 X 이므로, 끝에 2개 잘라서 확인
        Assertions.assertThat(lastTwoCommentList).isEqualTo(savedCommentList);

    }

    @Test
    void findByCommentId() {
    }

    @Test
    // 사실상 save + findByCommentId 동시 테스트
    void save() {
        // save 시 mybatis 에 의해 testComment.commentId 가 자동으로 DB 에 저장되는 commentId 값으로 설정됨
       commentMapper.save(testComment);

       // Optional<Comment> 로 찾아 .get() 하면 값 뽑아옴. (없으면 Exception)
       Comment findComment = commentMapper.findByCommentId(testComment.getCommentId()).get();

       log.info("findComment = {}", findComment);
       log.info("testComment = {}", testComment.toString());

       // DB 에서 찾은 findComment 와 저장하려고 만든 testComment 가 같은지 확인
       Assertions.assertThat(findComment).isEqualTo(testComment);

       // 문제1. LocalDateTime.now()
        // LocalDateTime.now() 에서 시간이 소수점(밀리세컨드)까지 밀려나오는 현상 발생
        // DB 에 저장된 regDate 는 (아마도) 밀리세컨드에서 반올림하여 저장됨
        // testComment(java.LocalDateTime.now()) 와 findComment(MySQL.DateTime) 이 다르게 나옴.
        // 따라서 java.LocalDateTime.now().withNano(0) 을 통해 밀리세컨드 절삭.
        // 다만 실행 속도상 1초 이상 차이나면 에러가 다시 날걸로 예상됨.
    }

    @Test
    void update() {
        commentMapper.save(testComment);

        Comment updateParam = new Comment(98L, "asdf", "updated");

        commentMapper.update(testComment.getCommentId(), updateParam);

        Comment findComment = commentMapper.findByCommentId(testComment.getCommentId()).get();

        log.info("testComment = {}", testComment.toString());
        log.info("findComment = {}", findComment);
        log.info("updateParam = {}", updateParam);

        // update 되는 값만 테스트
        Assertions.assertThat(findComment.getWriter()).isEqualTo(updateParam.getWriter());
        Assertions.assertThat(findComment.getContent()).isEqualTo(updateParam.getContent());
        Assertions.assertThat(findComment.getRegDate()).isEqualTo(updateParam.getRegDate());
    }

    @Test
    void delete() {
        commentMapper.save(testComment);
        commentMapper.delete(testComment.getCommentId());

        Assertions.assertThat(commentMapper.findByCommentId(testComment.getCommentId())).isEmpty();
    }
}