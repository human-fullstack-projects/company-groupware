package com.company.groupware.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 연결(핸드셰이크) 시점에 요청 쿼리 파라미터로 넘어온 emplId 를
 * 세션 attributes 에 저장합니다. 이후 StompPrincipalHandshakeHandler 가 이 값을
 * 읽어 STOMP Principal 을 만듭니다.
 *
 * 클라이언트 연결 예: new SockJS('/ws?emplId=1')
 *
 * TODO: Spring Security 연동 후에는 쿼리 파라미터 대신 인증 토큰(JWT 등)에서
 *       emplId 를 추출하도록 교체해야 합니다. 지금 방식은 로그인 검증 없이
 *       emplId 값을 그대로 신뢰하므로 개발/테스트 단계에서만 사용하세요.
 */
public class StompHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String emplId = servletRequest.getServletRequest().getParameter("emplId");
            if (emplId == null || emplId.isBlank()) {
                // emplId 가 없으면 연결 자체를 거부합니다.
                return false;
            }
            attributes.put("emplId", emplId);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // 별도 처리 없음
    }
}
