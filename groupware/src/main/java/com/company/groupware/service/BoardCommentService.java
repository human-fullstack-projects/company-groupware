package com.company.groupware.service;

import com.company.groupware.dto.BoardCommentRequest;
import com.company.groupware.entity.Board;
import com.company.groupware.entity.Comment;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.BoardCommentRepository;
import com.company.groupware.repository.BoardRepository;
import com.company.groupware.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardCommentService {

    private final BoardCommentRepository commentRepository;
    private final EmployeeRepository employeeRepository;
    private final BoardRepository boardRepository;

    @Transactional
    public void createComment(BoardCommentRequest request) {

        Employee employee = employeeRepository.findById(request.getEmplId())
                .orElseThrow();

        Board board = boardRepository.getReferenceById(request.getBoardId());

        Comment parent = null;
        if (request.getParentCommentId() != null) {
            parent = commentRepository.getReferenceById(request.getParentCommentId());
        }

        Comment comment = Comment.create(
                request.getComContent(),
                employee,
                board,
                parent
        );

        commentRepository.save(comment);
    }





}
