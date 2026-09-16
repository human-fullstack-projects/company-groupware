package com.company.groupware.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 관리자 권한이 없는 계정이 /admin/** 에 접근했을 때 기본 whitelabel 403 대신
 * 홈 화면으로 리다이렉트한다. home.html이 ?error=admin_forbidden 파라미터를 감지해
 * "관리자만 접속이 가능합니다" 토스트를 띄운다.
 */
@Component
public class AdminAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.sendRedirect(request.getContextPath() + "/?error=admin_forbidden");
    }
}
