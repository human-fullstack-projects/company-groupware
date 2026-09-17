package com.company.groupware.controller;

import com.company.groupware.service.HomeDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final HomeDashboardService homeDashboardService;

    @GetMapping(path = {"/", "/home"})
    public String home(Principal principal, Model model) {
        // 브라우저에서 직원 ID를 받지 않고 로그인한 사람의 신원을 사용합니다.
        if (principal == null) return "redirect:/login";
        model.addAttribute("dashboard", homeDashboardService.getDashboard(principal.getName()));
        return "home";
    }
}
