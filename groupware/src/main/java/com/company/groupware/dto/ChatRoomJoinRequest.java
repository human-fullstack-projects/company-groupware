package com.company.groupware.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomJoinRequest {
    private Long emplId; // 참여할 직원 사번
}
