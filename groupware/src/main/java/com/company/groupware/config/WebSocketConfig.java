package com.company.groupware.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompPrincipalHandshakeHandler stompPrincipalHandshakeHandler;

    // STOMP 접속 엔드포인트 등록 (클라이언트가 SockJS로 연결하는 지점)
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // TODO: 운영 환경에서는 실제 프론트엔드 도메인으로 제한
                .setHandshakeHandler(stompPrincipalHandshakeHandler)
                .withSockJS();
    }

    // 구독(수신) 경로 / 발행(송신) 경로 프리픽스 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트 브로드캐스트 구독 경로: /topic/room/{roomId}
        // 서버 -> 특정 사용자 전용 전송 경로(에러 알림 등): /user/queue/**
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");

        // 클라이언트 -> 서버 발행 경로 프리픽스: /app/chat.send 등
        registry.setApplicationDestinationPrefixes("/app");
    }
}
