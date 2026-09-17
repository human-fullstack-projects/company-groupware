package com.company.groupware.repository;

import com.company.groupware.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByBoardBoardIdOrderByCreatedAtAscComIdAsc(Long boardId);

    boolean existsByParentComment_ComId(Long commentId);

    // 해당 게시글의 최상위 댓글 조회
    List<Comment> findByBoardBoardIdAndParentCommentIsNull(Long boardId);

    // 해당 댓글 바로 아래의 답글 조회
    List<Comment> findByParentComment_ComId(Long parentCommentId);
}