package com.company.groupware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BoardCommentRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 200, message = "댓글은 200자 이내로 입력해주세요.")
    private String comContent;
    private Long parentCommentId;
    private Long emplId;
    private Long boardId;

}

//
//@Getter
//@Setter
//public class BoardCommentRequest
//{
//    private int com_id;
//
//    @NotBlank(message = "댓글 내용을 입력해주세요.")
//    @Size(max=300, message = "제목은 300자 이내로 입력해주세요.")
//    private String com_content;
//
//    private LocalDateTime created_at;
//    private int empl_id;
//    private int board_id;
//    private int com_id2;
//}
