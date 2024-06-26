package hello.board.repository.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.board.domain.board.Board;
import hello.board.domain.comment.QComment;
import hello.board.domain.criteria.Criteria;
import hello.board.domain.enums.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static hello.board.domain.board.QBoard.*;
import static hello.board.domain.comment.QComment.*;
import static hello.board.domain.member.QMember.member;

@Repository
@Slf4j
@RequiredArgsConstructor
public class BoardQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Long countTotalBoard(Criteria criteria) {
        return queryFactory.select(board.count())
                .from(board)
                .where(
                        categoryEq(criteria.getCategory()),
                        keywordEq(criteria.getKeyword(), criteria.getOption())
                ).fetchFirst();
    }

    public Long countTotalBoardWithMemberId(Criteria criteria, Long memberId) {
        return queryFactory.select(board.count())
                .from(board)
                .where(
                        memberIdEq(memberId),
                        categoryEq(criteria.getCategory()),
                        keywordEq(criteria.getKeyword(), criteria.getOption())
                ).fetchFirst();
    }

    // board 와 comment 를 페치조인으로 한번에 끌어오기 (distinct() 사용가능)
    public Board findByIdWithComment(Long id) {
        return queryFactory.select(board)
                .from(board)
                .leftJoin(board.commentList, comment).fetchJoin()
                .where(board.id.eq(id))
                .fetchOne();// 인메모리 패치조인 방지
        }

    public List<Board> findPagedBoard(Criteria criteria) {
        return queryFactory.select(board)
                .from(board)
                .where(
                        categoryEq(criteria.getCategory()),
                        keywordEq(criteria.getKeyword(), criteria.getOption())
                )
                .orderBy(board.id.desc())
                .offset(criteria.getStartRowNum())
                .limit(criteria.getContentPerPage())
                .fetch();
    }

    public List<Board> findPagedBoardWithMemberId(Criteria criteria, Long memberId) {
        return queryFactory.select(board)
                .from(board)
                .join(board.member, member)
                .where(
                        memberIdEq(memberId),
                        categoryEq(criteria.getCategory()),
                        keywordEq(criteria.getKeyword(), criteria.getOption())
                )
                .orderBy(board.id.desc())
                .offset(criteria.getStartRowNum())
                .limit(criteria.getContentPerPage())
                .fetch();
    }

    private BooleanExpression memberIdEq(Long memberId) {
        return board.member.id.eq(memberId);
    }

    private BooleanExpression categoryEq(Category category) {
        return category != null && category != Category.ALL ? board.category.eq(category) : null;
    }

    private BooleanExpression keywordEq(String keyword, String option) {
        if (!StringUtils.hasText(keyword)) return null; // keyword 없음
        return option.equals("title") ? board.title.contains(keyword) : board.content.contains(keyword);
    }
}
