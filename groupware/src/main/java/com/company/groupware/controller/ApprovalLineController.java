package com.company.groupware.controller;

import com.company.groupware.dto.ApprovalEmployeeResponse;
import com.company.groupware.dto.ApprovalLineRequest;
import com.company.groupware.dto.ApprovalLineResponse;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.ApprovalLineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/approval-lines")
public class ApprovalLineController {

    private final ApprovalLineService approvalLineService;
    private final EmployeeRepository employeeRepository;


    /**
     * 결재라인 관리 화면
     */
    @GetMapping
    public String approvalLinePage() {

        return "approval/line";
    }


    /**
     * 내 결재라인 전체
     */
    @GetMapping("/api")
    @ResponseBody
    public List<ApprovalLineResponse> getMyLines(
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalLineService
                .getMyApprovalLines(emplId);
    }


    /**
     * 결재라인 상세
     */
    @GetMapping("/api/{lineId}")
    @ResponseBody
    public ApprovalLineResponse getLine(
            @PathVariable Long lineId,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalLineService
                .getApprovalLine(lineId, emplId);
    }


    /**
     * 결재자 선택용 사원 목록
     */
    @GetMapping("/api/employees")
    @ResponseBody
    public List<ApprovalEmployeeResponse> getEmployees(
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalLineService
                .getEmployees(emplId);
    }


    /**
     * 결재라인 생성
     */
    @PostMapping("/api")
    @ResponseBody
    public ApprovalLineResponse createLine(
            @RequestBody ApprovalLineRequest request,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalLineService
                .createApprovalLine(emplId, request);
    }


    /**
     * 결재라인 수정
     */
    @PutMapping("/api/{lineId}")
    @ResponseBody
    public ApprovalLineResponse updateLine(
            @PathVariable Long lineId,
            @RequestBody ApprovalLineRequest request,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalLineService
                .updateApprovalLine(
                        lineId,
                        emplId,
                        request
                );
    }


    /**
     * 결재라인 삭제
     */
    @DeleteMapping("/api/{lineId}")
    @ResponseBody
    public ResponseEntity<Void> deleteLine(
            @PathVariable Long lineId,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        approvalLineService
                .deleteApprovalLine(lineId, emplId);

        return ResponseEntity.noContent().build();
    }


    /**
     * 로그인 사용자 emplId
     */
    private Long getLoginEmplId(Authentication authentication) {

        Employee employee = employeeRepository
                .findByLoginId(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "로그인 사용자 정보를 찾을 수 없습니다."
                        )
                );

        return employee.getEmplId();
    }
}