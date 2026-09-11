package com.company.groupware.dto;

import com.company.groupware.entity.ChatRoomFile;
import com.company.groupware.entity.ChatRoomMessage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {

    private final Long messageId;
    private final Long roomId;
    private final Long emplId;
    private final String emplName;
    private final String content;
    private final LocalDateTime createdAt;

    private final ChatFileResponse file;

    public ChatMessageResponse(ChatRoomMessage message) {
        this(message, null);
    }

    public ChatMessageResponse(ChatRoomMessage message, ChatRoomFile chatRoomFile) {
        this.messageId = message.getMessageId();
        this.roomId = message.getChatRoom().getRoomId();
        this.emplId = message.getEmployee().getEmplId();
        this.emplName = message.getEmployee().getEmplName();
        this.content = message.getMessageContent();
        this.createdAt = message.getCreatedAt();
        this.file = chatRoomFile != null ? new ChatFileResponse(chatRoomFile) : null;
    }
}
