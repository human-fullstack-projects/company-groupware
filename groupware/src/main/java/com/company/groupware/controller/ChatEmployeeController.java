package com.company.groupware.controller;

import com.company.groupware.dto.ChatRoomEmployeeSearchResponse;
import com.company.groupware.service.ChatRoomEmployeeService;
import com.company.groupware.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class ChatEmployeeController {

    private final ChatRoomEmployeeService employeeService;

    // 이름 일부로 직원 검색 (채팅방 초대 대상 검색 등에 사용)
    @GetMapping("/search")
    public ResponseEntity<List<ChatRoomEmployeeSearchResponse>> search(@RequestParam String name) {
        return ResponseEntity.ok(employeeService.searchByName(name));
    }
}
