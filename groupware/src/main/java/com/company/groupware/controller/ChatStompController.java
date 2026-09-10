package com.company.groupware.controller;


import com.company.groupware.dto.ChatMessageRequest;
import com.company.groupware.dto.ChatMessageResponse;
import com.company.groupware.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 /app/chat.send 로 발행한 메시지를 저장한 뒤
     * 해당 방을 구독 중인 모든 클라이언트(/topic/room/{roomId})에게 브로드캐스트합니다.
     *
     * 클라이언트 전송 예 (STOMP):
     *   destination: /app/chat.send
     *   body: {"roomId": 1, "content": "안녕하세요"}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest request, Principal principal) {
        Long emplId = Long.valueOf(principal.getName());
        ChatMessageResponse response = chatMessageService.saveMessage(request, emplId);
        messagingTemplate.convertAndSend("/topic/room/" + response.getRoomId(), response);
    }

    /**
     * sendMessage 처리 중 예외(방 없음, 참여 중이 아님 등)가 발생하면
     * 연결을 끊지 않고 보낸 사람에게만 /user/queue/errors 로 에러를 알려줍니다.
     */
    @MessageExceptionHandler
    public void handleException(Exception exception, Principal principal) {
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                Map.of("message", exception.getMessage())
        );
    }
}
