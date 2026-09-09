package com.company.groupware.repository;

import com.company.groupware.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCommentRepository extends JpaRepository<Comment, Long> {
}
