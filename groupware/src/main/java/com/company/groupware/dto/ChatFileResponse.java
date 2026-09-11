package com.company.groupware.dto;

import com.company.groupware.entity.ChatRoomFile;
import lombok.Getter;

@Getter
public class ChatFileResponse {

    private final Long fileId;
    private final String fileName;
    private final Long fileSize;
    private final String downloadUrl;

    public ChatFileResponse(ChatRoomFile chatRoomFile) {
        this.fileId = chatRoomFile.getMessageFileId();
        this.fileName = chatRoomFile.getMessageFileOriginName();
        this.fileSize = chatRoomFile.getMessageFileSize();
        this.downloadUrl = "/api/chat/rooms/files/" + chatRoomFile.getMessageFileId();
    }
}
