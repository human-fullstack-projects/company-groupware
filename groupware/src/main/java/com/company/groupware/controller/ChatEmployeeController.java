package com.company.groupware.controller;

import com.company.groupware.dto.ChatRoomEmployeeSearchResponse;
import com.company.groupware.service.ChatAccessService;
import com.company.groupware.service.ChatRoomEmployeeService;
import com.company.groupware.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class ChatEmployeeController {

    private final ChatRoomEmployeeService employeeService;
    private final ChatAccessService chatAccessService;

    // 이름 일부로 직원 검색 (채팅방 초대 대상 검색 등에 사용)
    @GetMapping("/search")
    public ResponseEntity<List<ChatRoomEmployeeSearchResponse>> search(@RequestParam String name) {
        return ResponseEntity.ok(employeeService.searchByName(name));
    }

    // 초대 팝업용 전체 직원 목록 (본인 제외, roomId가 있으면 해당 방의 현재 참여자도 제외)
    @GetMapping
    public ResponseEntity<List<ChatRoomEmployeeSearchResponse>> list(
            @RequestParam(required = false) Long roomId,
            Principal principal) {
        Long emplId = chatAccessService.getHttpEmployee(principal).getEmplId();
        if (roomId != null) {
            chatAccessService.requireActiveMember(emplId, roomId);
        }
        return ResponseEntity.ok(employeeService.listInvitable(roomId, emplId));
    }
}
