package com.company.groupware.security;

import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // 로그인 화면에서 입력한 아이디로 직원 조회
        Employee employee = employeeRepository.findByLoginId(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "등록되지 않은 아이디입니다."
                        )
                );

        // 현재 Entity의 관리자 여부를 기준으로 권한 구분
        String role = Boolean.TRUE.equals(employee.getEmplStat())
                ? "ADMIN"
                : "USER";

        // Security가 인증에 사용할 정보 반환
        return User.withUsername(employee.getLoginId())
                .password(employee.getPasswordHash())
                .roles(role)
                .build();
    }
}