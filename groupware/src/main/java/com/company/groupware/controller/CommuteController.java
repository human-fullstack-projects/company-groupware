package com.company.groupware.controller;

import com.company.groupware.Exception.InvalidCommuteStateException;
import com.company.groupware.Exception.ResourceNotFoundException;
import com.company.groupware.dto.CommuteResponse;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.CommuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class CommuteController {

    private final CommuteService commuteService;
    private final EmployeeRepository employeeRepository;

    @GetMapping(path = "/commute")
    public String main(Model model, Principal principal){
        Employee employee = employeeRepository.findByLoginId(principal.getName()).orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다."));
        Long emplId = employee.getEmplId();

        CommuteResponse today = commuteService.getToday(emplId);

        model.addAttribute("emplId", emplId);
        model.addAttribute("today", today);
        model.addAttribute("commuteState", resolveState(today));

        return "commute/main";
    }

    @PostMapping("/commute/check-in")
    public String checkIn(@RequestParam Long emplId, RedirectAttributes redirectAttributes) {
        try {
            commuteService.checkIn(emplId);
        } catch (InvalidCommuteStateException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/commute";
    }

    @PostMapping("/commute/check-out")
    public String checkOut(@RequestParam Long emplId, RedirectAttributes redirectAttributes) {
        try {
            commuteService.checkOut(emplId);
        } catch (InvalidCommuteStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/commute";
    }

    private String resolveState(CommuteResponse today) {
        if (today == null) {
            return "NOT_CHECKED_IN"; // 출근 전
        }
        if (today.getFinishTime() == null) {
            return "CHECKED_IN"; // 출근했고 아직 퇴근 전
        }
        return "CHECKED_OUT"; // 오늘 근무 종료
    }
}
