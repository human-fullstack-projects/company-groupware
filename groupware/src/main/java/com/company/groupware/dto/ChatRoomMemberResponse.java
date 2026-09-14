package com.company.groupware.dto;

import com.company.groupware.entity.ChatRoomAffiliation;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ChatRoomMemberResponse {

    private final Long emplId;
    private final String emplName;
    private final LocalDateTime roomInDate;
    private final LocalDateTime roomOutDate;

    public ChatRoomMemberResponse(ChatRoomAffiliation affiliation) {
        this.emplId = affiliation.getEmployee().getEmplId();
        this.emplName = affiliation.getEmployee().getEmplName();
        this.roomInDate = affiliation.getRoomInDate();
        this.roomOutDate = affiliation.getRoomOutDate();
    }
}
