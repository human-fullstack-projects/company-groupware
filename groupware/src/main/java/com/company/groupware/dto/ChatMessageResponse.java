package com.company.groupware.dto;

import com.company.groupware.entity.ChatRoomFile;
import com.company.groupware.entity.ChatRoomMessage;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ChatMessageResponse {

    private final Long messageId;
    private final Long roomId;
    private final Long emplId;
    private final String emplName;
    private final String content;
    private final LocalDateTime createdAt;

    private final List<ChatFileResponse> files;

    public ChatMessageResponse(ChatRoomMessage message) {
        this(message, Collections.emptyList());
    }

    public ChatMessageResponse(ChatRoomMessage message, List<ChatRoomFile> chatRoomFiles) {
        this.messageId = message.getMessageId();
        this.roomId = message.getChatRoom().getRoomId();
        this.emplId = message.getEmployee().getEmplId();
        this.emplName = message.getEmployee().getEmplName();
        this.content = message.getMessageContent();
        this.createdAt = message.getCreatedAt();
        this.files = chatRoomFiles.stream().map(ChatFileResponse::new).collect(Collectors.toList());
    }
}
