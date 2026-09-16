package com.company.groupware.controller;

import com.company.groupware.entity.ChatRoom;
import com.company.groupware.entity.ChatRoomAffiliation;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.ChatRoomAffiliationRepository;
import com.company.groupware.repository.ChatRoomRepository;
import com.company.groupware.service.ChatAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatPageController {

    private final ChatAccessService chatAccessService;
    private final ChatRoomAffiliationRepository chatRoomAffiliationRepository;
    private final ChatRoomRepository chatRoomRepository;

    // 메신저 목록 화면: 로그인한 직원이 소속된 채팅방 리스트 + 방 생성
    @GetMapping("/chat/list")
    public String chatList(Authentication authentication, Model model) {
        Employee loginEmpl = chatAccessService.getHttpEmployee(authentication);
        model.addAttribute("loginEmplId", loginEmpl.getEmplId());

        List<ChatRoomAffiliation> roomList =
                chatRoomAffiliationRepository.findByEmployee_EmplIdAndRoomOutDateIsNull(loginEmpl.getEmplId());
        model.addAttribute("chatRoomList", roomList);

        // 필요한 데이터만 가져와서 model에 담아 html로 보내는 역할 + 화면 연결

        return "chat/list";
    }

    // 채팅방 상세 화면
    @GetMapping("/chat/detail")
    public String chatDetail(@RequestParam Long roomId, Authentication authentication, Model model) {
        Employee loginEmpl = chatAccessService.getHttpEmployee(authentication);
        model.addAttribute("loginEmplId", loginEmpl.getEmplId());

        chatAccessService.requireActiveMember(loginEmpl.getEmplId(), roomId);
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow();
        model.addAttribute("chatRoom", chatRoom);

        return "chat/detail";
    }

    // 동료 초대 팝업 화면 (roomId 없으면 새 채팅방 생성용, 있으면 기존 방 초대용)
    @GetMapping("/chat/invite-popup")
    public String invitePopup(
            @RequestParam(required = false) Long roomId,
            Authentication authentication, Model model) {
        Employee loginEmpl = chatAccessService.getHttpEmployee(authentication);
        model.addAttribute("loginEmplId", loginEmpl.getEmplId());

        if (roomId != null) {
            chatAccessService.requireActiveMember(loginEmpl.getEmplId(), roomId);
        }
        model.addAttribute("roomId", roomId);

        return "chat/invite-popup";
    }
}
