package com.company.groupware.dto;

import lombok.Getter;

/**
 * 관리자가 채팅방을 강제 삭제(비활성화)했을 때, 그 방을 보고 있던 접속자들에게
 * 실시간으로 알리는 이벤트. 채팅 메시지(ChatMessageResponse)와 구분하기 위해
 * 별도 타입으로 /topic/room/{roomId} 에 브로드캐스트한다.
 */
@Getter
public class ChatRoomClosedEvent {
    private final String type = "ROOM_CLOSED";
    private final Long roomId;
    private final String message;

    public ChatRoomClosedEvent(Long roomId, String message) {
        this.roomId = roomId;
        this.message = message;
    }
}
