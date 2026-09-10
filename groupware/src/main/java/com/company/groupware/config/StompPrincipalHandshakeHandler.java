package com.company.groupware.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * StompHandshakeInterceptor 가 attributes 에 저장해둔 emplId 를 꺼내
 * 이후 @MessageMapping 메서드에서 Principal 로 주입받을 수 있게 해줍니다.
 */
public class StompPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        String emplId = (String) attributes.get("emplId");
        return new StompPrincipal(emplId);
    }
}
