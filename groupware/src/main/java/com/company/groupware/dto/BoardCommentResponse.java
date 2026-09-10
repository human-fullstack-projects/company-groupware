package com.company.groupware.dto;

import com.company.groupware.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardCommentResponse {

    private Long comId;
    private String comContent;

    private Long emplId;
    private String emplName;

    private LocalDateTime createdAt;


    public static BoardCommentResponse from(Comment comment) {
        return BoardCommentResponse.builder()
                .comId(comment.getComId())
                .comContent(comment.getComContent())
                .emplId(comment.getEmployee().getEmplId())
                .emplName(comment.getEmployee().getEmplName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}