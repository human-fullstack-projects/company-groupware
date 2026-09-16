package com.company.groupware.dto;

import lombok.Getter;

/**
 * 특정 직원이 방의 어느 메시지까지 읽었는지를 같은 방의 다른 접속자들에게 실시간으로 알리는 이벤트
 */
@Getter
public class ChatReadEvent {
    private final Long emplId;
    private final Long lastMessageId;

    public ChatReadEvent(Long emplId, Long lastMessageId) {
        this.emplId = emplId;
        this.lastMessageId = lastMessageId;
    }
}
