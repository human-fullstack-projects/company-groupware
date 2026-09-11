package com.company.groupware.controller;

import com.company.groupware.dto.BoardCommentRequest;
import com.company.groupware.dto.BoardCommentResponse;
import com.company.groupware.entity.Board;
import com.company.groupware.entity.Comment;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.BoardCommentService;
import com.company.groupware.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

//@Controller
@RestController
@RequiredArgsConstructor
public class BoardCommentController {
    //이 컨트롤러에서 처리하는 것들은 기본적으로 페이지 이동 없음. 자바스크립트에서 페이지 일부 요소 수정처리
    private final BoardCommentService boardCommentService;

    private final BoardService boardService;

    private final EmployeeRepository employeeRepository;


    private Employee getLoginEmployee(String loginId) {
        return employeeRepository
                .findByLoginId(loginId)
                .orElseThrow();
    }

    @GetMapping("/api/boards/{boardId}/comments")
    public List<BoardCommentResponse> getComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long boardId) {

        Employee employee = getLoginEmployee(userDetails.getUsername());

        Board board = boardService.findById(boardId);

        if (!boardService.canRead(board.getBoardCategory(), employee)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 게시글의 댓글을 볼 권한이 없습니다."
            );
        }

        List<Comment> comments = boardCommentService.getComments(boardId);

        List<BoardCommentResponse> responseList = comments.stream()
                .map(comment -> BoardCommentResponse.from(comment, employee.getEmplId()))
                .toList();

        return responseList;
        //response 변환 작업
    }

    @PostMapping("/api/boards/{boardId}/comments/add")
//    @ResponseBody RestController로 변경하면서 불필요해짐
    public BoardCommentResponse addComment(
            @PathVariable Long boardId,
            @Valid BoardCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model){

//        //테스트용. build(); 까지 주석처리 및 아래꺼 주석 풀고 사용
//        long testId = 1;
//        return BoardCommentResponse.builder()
//                .comId(testId)
//                .comContent("테스트 댓글")
//                .emplId(testId)
//                .emplName("테스트")
//                .createdAt(LocalDateTime.now())
//                .build();

//        long emplId = Long.parseLong(userDetails.getUsername());


//        long emplId=20; //testest 계정 아이디
//        request.setEmplId(emplId);

        Employee employee = getLoginEmployee(userDetails.getUsername());

        Board board = boardService.findById(boardId);

        if (!boardService.canRead(board.getBoardCategory(), employee)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 게시글에 댓글을 작성할 권한이 없습니다."
            );
        }

        request.setEmplId(employee.getEmplId());
        request.setBoardId(boardId);

        Comment comment = boardCommentService.insertComment(request);

        return BoardCommentResponse.from(comment, employee.getEmplId());
    }

    //edit
    @PostMapping("/api/boards/{commentId}/comments/edit")
    public ResponseEntity<Void> editComment(
            @PathVariable Long commentId,
            @Valid BoardCommentRequest request){

        boardCommentService.updateComment(commentId, request.getComContent());
        return ResponseEntity.noContent().build();
//        return boardCommentService.updateComment(commentId, request.getComContent());
    }

    //delete
    @PostMapping("/api/boards/{commentId}/comments/remove")
    public void removeComment(
            @PathVariable Long commentId){
        boardCommentService.deleteComment(commentId);
    }

}
