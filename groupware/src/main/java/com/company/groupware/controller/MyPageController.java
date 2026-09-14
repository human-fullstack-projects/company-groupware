package com.company.groupware.controller;

import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    @GetMapping("/mypage")
    public String mypage(Authentication authentication, Model model) {
        Employee employee = employeeRepository.findByLoginId(authentication.getName())
                .orElseThrow();

        model.addAttribute("employee", employee);
        return "empl/mypage";
    }

    @PostMapping("/mypage")
    public String update(
            Authentication authentication,
            @RequestParam String emplPhone,
            @RequestParam String emplEmail,
            @RequestParam(required = false) String address,
            RedirectAttributes redirectAttributes) {

        try {
            employeeService.updateContactInfo(authentication.getName(), emplPhone, emplEmail, address);
            redirectAttributes.addFlashAttribute("message", "내 정보를 수정했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/mypage";
    }
}
