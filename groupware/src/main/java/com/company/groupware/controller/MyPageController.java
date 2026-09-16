package com.company.groupware.controller;

import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.ApprovalSignatureService;
import com.company.groupware.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final ApprovalSignatureService signatureService;


    /**
     * 마이페이지 화면
     */
    @GetMapping("/mypage")
    public String mypage(
            Authentication authentication,
            Model model) {

        Employee employee = employeeRepository
                .findByLoginId(authentication.getName())
                .orElseThrow();

        boolean hasSignature = signatureService
                .getSignature(employee.getEmplId())
                .isPresent();

        model.addAttribute("employee", employee);
        model.addAttribute("hasSignature", hasSignature);

        return "empl/mypage";
    }


    /**
     * 마이페이지 개인정보 수정
     */
    @PostMapping("/mypage")
    public String updateMyPage(
            Authentication authentication,
            @RequestParam String emplPhone,
            @RequestParam String emplEmail,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) MultipartFile signatureFile,
            @RequestParam(defaultValue = "false") boolean signatureDelete) {

        Employee employee = employeeRepository
                .findByLoginId(authentication.getName())
                .orElseThrow();

        // 개인정보 수정
        employeeService.updateContactInfo(
                authentication.getName(),
                emplPhone,
                emplEmail,
                address
        );

        // 서명 삭제
        if (signatureDelete) {
            signatureService.deleteSignature(
                    employee.getEmplId()
            );
        }

        // 새로운 서명 등록
        else if (signatureFile != null
                && !signatureFile.isEmpty()) {

            signatureService.saveSignature(
                    employee.getEmplId(),
                    signatureFile
            );
        }

        return "redirect:/mypage";
    }


    /**
     * 마이페이지 - 비밀번호 변경
     */
    @PostMapping("/mypage/password")
    public String changePassword(
            Authentication authentication,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String newPasswordConfirm,
            RedirectAttributes redirectAttributes) {

        try {

            // 현재 로그인한 사용자의 비밀번호 변경
            employeeService.changePassword(
                    authentication.getName(),
                    currentPassword,
                    newPassword,
                    newPasswordConfirm
            );

            // 성공 메시지
            redirectAttributes.addFlashAttribute(
                    "passwordMessage",
                    "비밀번호가 성공적으로 변경되었습니다."
            );

        } catch (IllegalArgumentException e) {

            // 실패 메시지
            redirectAttributes.addFlashAttribute(
                    "passwordError",
                    e.getMessage()
            );
        }

        return "redirect:/mypage";
    }


//    @GetMapping("/mypage")
//    public String mypage(Authentication authentication, Model model) {
//        Employee employee = employeeRepository.findByLoginId(authentication.getName())
//                .orElseThrow();
//
//        model.addAttribute("employee", employee);
//        return "empl/mypage";
//    }
//
//    @PostMapping("/mypage")
//    public String update(
//            Authentication authentication,
//            @RequestParam String emplPhone,
//            @RequestParam String emplEmail,
//            @RequestParam(required = false) String address,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            employeeService.updateContactInfo(
//                    authentication.getName(),
//                    emplPhone,
//                    emplEmail,
//                    address
//            );
//
//            redirectAttributes.addFlashAttribute(
//                    "message",
//                    "내 정보를 수정했습니다."
//            );
//
//        } catch (IllegalArgumentException e) {
//
//            redirectAttributes.addFlashAttribute(
//                    "error",
//                    e.getMessage()
//            );
//        }
//
//        return "redirect:/mypage";
//    }
}