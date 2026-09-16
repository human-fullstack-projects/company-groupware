package com.company.groupware.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatReadRequest {
    private Long lastMessageId; // 현재까지 읽은 메시지 중 가장 최신 messageId
}
