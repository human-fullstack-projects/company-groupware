package com.company.groupware.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatRoomCreateRequest {

    private String roomName; // 채팅방 이름

    private Long creatorEmplId; // 방 개설한 직원 사번
    // 후에 security 적용 후 principle로 대체

    private List<Long> memberEmplIds; // 초대할 직원 사번 목록
}
