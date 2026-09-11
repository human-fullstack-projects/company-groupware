package com.company.groupware.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // 비밀번호 해싱
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 로그인 및 페이지 접근 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // 로그인하지 않아도 접근 가능한 주소
                        .requestMatchers(
                                "/",
                                "/home",
                                "/login",
                                "/register",
                                "/css/**",
                                "/js/**",
                                "/image/**",
                                "/error"
                        ).permitAll()

                        // 관리자만 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 나머지는 로그인 필요
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        // 로그아웃 성공 시 메인 화면으로 이동
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }
}