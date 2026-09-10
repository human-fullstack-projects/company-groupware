package com.company.groupware.controller;

import com.company.groupware.dto.BoardCommentRequest;
import com.company.groupware.service.BoardCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class BoardCommentController {
    //이 컨트롤러에서 처리하는 것들은 기본적으로 페이지 이동 없음. 자바스크립트에서 페이지 일부 요소 수정처리

    private final BoardCommentService boardCommentService;


    //add
    @PostMapping("/api/boards/{boardId}/comments")
    @ResponseBody
    public void addComment(
            @PathVariable Long boardId,
            @Valid BoardCommentRequest request,
            Model model){
        long emplId=1; //세션 또는 어딘가 저장된 아이디로 변경 필요
        request.setEmplId(emplId);
        request.setBoardId(boardId);
        boardCommentService.createComment(request);
        return;
    }
    //edit
    //delete
    //add(대댓. 근데 대댓이라고 다르게 들어가진 않을걸용)
}
