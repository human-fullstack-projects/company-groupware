package com.company.groupware.controller;

import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면(topinfo 등)에서 현재 로그인한 직원 정보를 쓸 수 있도록 Model에 공통으로 넣어준다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final EmployeeRepository employeeRepository;

    @ModelAttribute("loginEmployee")
    public Employee loginEmployee(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return employeeRepository.findByLoginId(authentication.getName())
                .orElse(null);
    }
}
