package com.company.groupware.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {

    private Long roomId;   // 메시지를 보낼 채팅방번호
    private String content; // 메시지 내용
}
