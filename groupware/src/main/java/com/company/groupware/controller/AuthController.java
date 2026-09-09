package com.company.groupware.controller;

import com.company.groupware.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final EmployeeService employeeService;

    // 회원가입 화면
    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    // 로그인 화면
    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    // 회원가입 처리
    @PostMapping("/register")
    public String register(
            @RequestParam(name = "loginId", defaultValue = "") String loginId,
            @RequestParam(name = "password", defaultValue = "") String password,
            @RequestParam(name = "passwordConfirm", defaultValue = "") String passwordConfirm,
            @RequestParam(name = "emplName", defaultValue = "") String emplName,
            @RequestParam(name = "emplEmail", defaultValue = "") String emplEmail,
            @RequestParam(name = "emplPhone", defaultValue = "") String emplPhone,
            Model model) {

        // 오류 발생 시 비밀번호를 제외한 입력값 유지
        model.addAttribute("loginId", loginId);
        model.addAttribute("emplName", emplName);
        model.addAttribute("emplEmail", emplEmail);
        model.addAttribute("emplPhone", emplPhone);

        if (!password.equals(passwordConfirm)) {
            model.addAttribute(
                    "errorMessage",
                    "비밀번호가 일치하지 않습니다."
            );
            return "auth/register";
        }

        try {
            employeeService.register(
                    loginId,
                    password,
                    emplName,
                    emplEmail,
                    emplPhone
            );
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute(
                    "errorMessage",
                    "등록 정보를 확인해주세요. 아이디 중복 등으로 저장하지 못했습니다."
            );
            return "auth/register";
        }

        // 회원가입 완료 후 로그인 화면으로 이동
        return "redirect:/login";
    }
}