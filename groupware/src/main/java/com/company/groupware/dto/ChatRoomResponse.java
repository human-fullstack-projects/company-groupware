package com.company.groupware.dto;

import com.company.groupware.entity.ChatRoom;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatRoomResponse {

    private final Long roomId;
    private final String roomName;
    private final LocalDateTime createdAt;
    private final Boolean roomStat;

    public ChatRoomResponse(ChatRoom chatRoom) {
        this.roomId = chatRoom.getRoomId();
        this.roomName = chatRoom.getRoomName();
        this.createdAt = chatRoom.getCreatedAt();
        this.roomStat = chatRoom.getRoomStat();
    }
}
