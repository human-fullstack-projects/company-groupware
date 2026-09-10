package com.company.groupware.repository;

import com.company.groupware.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<Comment, Long> {
    // select * from comment where board_id = {넘겨준 파라미터} order by created_at and com_id asc
//    List<Comment> findByBoardIdOrderByCreatedAtAscComIdAsc(Long boardId);
    List<Comment> findByBoardBoardIdOrderByCreatedAtAscComIdAsc(Long boardId);
    boolean existsByParentComment_ComId(Long commentId);
}
