package com.company.groupware.controller;

import com.company.groupware.service.DepartmentService;
import com.company.groupware.service.EmployeeService;
import com.company.groupware.service.GradeService;
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
    private final DepartmentService departmentService;
    private final GradeService gradeService;

    // 회원가입 화면
    @GetMapping("/register")
    public String registerForm(Model model) {
        addSelectOptions(model);
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
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "gradeId", required = false) Long gradeId,
            Model model) {

        // 실패 시 입력값과 선택 목록 유지
        model.addAttribute("loginId", loginId);
        model.addAttribute("emplName", emplName);
        model.addAttribute("emplEmail", emplEmail);
        model.addAttribute("emplPhone", emplPhone);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("gradeId", gradeId);
        addSelectOptions(model);

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
                    emplPhone,
                    departmentId,
                    gradeId
            );
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute(
                    "errorMessage",
                    "저장하지 못했습니다. 아이디 중복 및 부서·직급 정보를 확인해주세요."
            );
            return "auth/register";
        }

        return "redirect:/login";
    }

    // DB에 등록된 부서·직급 목록 전달
    private void addSelectOptions(Model model) {
        model.addAttribute(
                "departments",
                departmentService.getDepartments()
        );
        model.addAttribute(
                "grades",
                gradeService.getGrades()
        );
    }
}