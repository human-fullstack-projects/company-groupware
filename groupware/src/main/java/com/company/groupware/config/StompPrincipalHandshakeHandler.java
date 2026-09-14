package com.company.groupware.config;

import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * StompHandshakeInterceptor 가 attributes 에 저장해둔 emplId 를 꺼내
 * 이후 @MessageMapping 메서드에서 Principal 로 주입받을 수 있게 해줍니다.
 */

@Component
@RequiredArgsConstructor
public class StompPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    private final EmployeeRepository employeeRepository;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }

        Principal userPrincipal = servletRequest.getServletRequest().getUserPrincipal();
        if (userPrincipal == null) {
            throw new IllegalStateException("인증되지 않은 WebSocket 접속입니다.");
        }

        Employee employee = employeeRepository.findByLoginId(userPrincipal.getName())
                .orElseThrow(() -> new IllegalStateException("직원을 찾을 수 없습니다. loginId=" + userPrincipal.getName()));

        return new StompPrincipal(String.valueOf(employee.getEmplId()));
    }
}
