package com.company.groupware.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    /**
     * 관리자 대시보드
     *
     * SecurityConfig의 "/admin/**" 규칙에 의해 ROLE_ADMIN 계정만 접근 가능하다.
     */
    @GetMapping("/admin")
    public String dashboard() {
        return "admin/dashboard";
    }
}
