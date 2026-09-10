package com.company.groupware.config;

import java.security.Principal;

/**
 * WebSocket(STOMP) 세션에서 사용할 최소한의 Principal 구현체.
 * getName() 이 반환하는 값은 empl_id(문자열) 입니다.
 */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
