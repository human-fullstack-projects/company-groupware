package com.company.groupware.controller;

import com.company.groupware.Exception.InvalidCommuteStateException;
import com.company.groupware.Exception.ResourceNotFoundException;
import com.company.groupware.dto.AnnualLeaveHistoryResponse;
import com.company.groupware.dto.AnnualLeaveSummaryResponse;
import com.company.groupware.dto.CommuteResponse;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.AnnualLeaveService;
import com.company.groupware.service.CommuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CommuteController {

    private final CommuteService commuteService;
    private final AnnualLeaveService annualLeaveService;
    private final EmployeeRepository employeeRepository;


    /**
     * 근태 메인
     */
    @GetMapping(path = "/commute")
    public String main(
            Model model,
            Principal principal) {

        Employee employee =
                getLoginEmployee(principal);

        Long emplId =
                employee.getEmplId();

        CommuteResponse today =
                commuteService.getToday(emplId);

        AnnualLeaveSummaryResponse annualLeaveSummary =
                annualLeaveService.getSummary(emplId);

        model.addAttribute(
                "emplId",
                emplId
        );

        model.addAttribute(
                "today",
                today
        );

        model.addAttribute(
                "commuteState",
                resolveState(today)
        );

        model.addAttribute(
                "annualLeaveSummary",
                annualLeaveSummary
        );

        return "commute/main";
    }


    /**
     * 연차 관리
     */
    @GetMapping("/commute/leave")
    public String annualLeave(
            Model model,
            Principal principal) {

        Employee employee =
                getLoginEmployee(principal);

        Long emplId =
                employee.getEmplId();

        AnnualLeaveSummaryResponse summary =
                annualLeaveService.getSummary(emplId);

        List<AnnualLeaveHistoryResponse> history =
                annualLeaveService.getHistory(emplId);

        model.addAttribute(
                "annualLeaveSummary",
                summary
        );

        model.addAttribute(
                "annualLeaveHistory",
                history
        );

        return "commute/leave";
    }


    /**
     * 출근
     */
    @PostMapping("/commute/check-in")
    public String checkIn(
            @RequestParam Long emplId,
            RedirectAttributes redirectAttributes) {

        try {

            commuteService.checkIn(emplId);

        } catch (InvalidCommuteStateException
                 | ResourceNotFoundException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/commute";
    }


    /**
     * 퇴근
     */
    @PostMapping("/commute/check-out")
    public String checkOut(
            @RequestParam Long emplId,
            RedirectAttributes redirectAttributes) {

        try {

            commuteService.checkOut(emplId);

        } catch (InvalidCommuteStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/commute";
    }


    /**
     * 현재 로그인 직원 조회
     */
    private Employee getLoginEmployee(
            Principal principal) {

        return employeeRepository
                .findByLoginId(
                        principal.getName()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "직원을 찾을 수 없습니다."
                        )
                );
    }


    /**
     * 오늘 출퇴근 상태
     */
    private String resolveState(
            CommuteResponse today) {

        if (today == null || today.getStartTime() == null) {
            return "NOT_CHECKED_IN";
        }

        if (today.getFinishTime() == null) {
            return "CHECKED_IN";
        }

        return "CHECKED_OUT";
    }
}