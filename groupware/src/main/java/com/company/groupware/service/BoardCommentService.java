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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardCommentService {

    private final BoardCommentRepository commentRepository;
    private final EmployeeRepository employeeRepository;
    private final BoardRepository boardRepository;

    //댓글 입력
    @Transactional
    public Comment insertComment(BoardCommentRequest request) {

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

        return commentRepository.save(comment);
    }


    //전체 댓글 조회
    public List<Comment> getComments(Long boardId) {
//        Board board = boardRepository.getReferenceById(boardId);
//        return boardRepository.findAllByBoardOrderByCreatedAtAsc(board);
        return commentRepository.findByBoardBoardIdOrderByCreatedAtAscComIdAsc(boardId);
    }

    //댓글 수정
    @Transactional
    public void updateComment(Long commentId, String content) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow();

        comment.setComContent(content);
    }

    //댓글 삭제
    @Transactional
    public void deleteComment(Long commentId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow();

        // 해당 댓글을 부모로 가진 답글이 있는지 확인
        if (commentRepository.existsByParentComment_ComId(commentId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "답글이 있는 댓글은 삭제할 수 없습니다."
            );
        }

        commentRepository.delete(comment);
    }

}
