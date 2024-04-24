package hello.board.domain.comment;

import hello.board.domain.board.Board;
import hello.board.domain.enums.Category;
import lombok.*;

import javax.persistence.*;
import javax.persistence.FetchType;
import java.time.LocalDateTime;

@Getter @ToString @EqualsAndHashCode(of = {"writer", "regDate"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    @ToString.Exclude // toString 순환참조 방지
    private Board board;       // board.id 를 참조하는 fk
//    private Long boardId;

    private Long memberId;      // board.memberId 를 참조, 글쓴이의 id
    private Long targetId;      // 대댓글의 target 의 id (없으면 0)

    private String  writer;     // 글쓴이의 member.username
    private String target;      // 대댓글의 target 의 member.username (없으면 null)

    private String content;

    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    // 계층 답글
    private Long groupId;       // 모댓글은 자기자신, 대댓글은 모댓글, 대-대댓글은 모댓글의 commentId
    private Integer groupOrder;     // 대댓글일때, 대댓글 순서
    private Integer groupDepth;     // 모댓글 0, 대댓글 1

    @Enumerated(EnumType.STRING)
    private Category category;      // 댓글 카테고리 조회를 위한 카테고리


    /**
     * 테스트용 밀리세컨드 절삭
     */
    public void cutNanoSecond() {
        this.regDate = LocalDateTime.now().withNano(0);
    }

    // 얜 왜안돼지?
    public void setComment_id(Long id) {
        this.id = id;
    }

    /**
     * Comment.groupId 지정
     * @param commentId
     */
    public void setGroupId(Long commentId) {
        this.groupId = commentId;
    }

    public void updateComment(Comment param) {
        this.content = param.getContent(); // 빈칸가능
        this.updateDate = param.getUpdateDate();
    }


}
