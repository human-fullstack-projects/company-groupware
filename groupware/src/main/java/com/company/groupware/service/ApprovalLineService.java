package com.company.groupware.service;

import com.company.groupware.dto.ApprovalEmployeeResponse;
import com.company.groupware.dto.ApprovalLineRequest;
import com.company.groupware.dto.ApprovalLineResponse;
import com.company.groupware.dto.ApproverResponse;
import com.company.groupware.entity.ApprovalLine;
import com.company.groupware.entity.ApprovalLineMember;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.ApprovalLineMemberRepository;
import com.company.groupware.repository.ApprovalLineRepository;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApprovalLineService {

    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalLineMemberRepository approvalLineMemberRepository;
    private final EmployeeRepository employeeRepository;


    /**
     * 내 결재라인 전체 조회
     */
    @Transactional(readOnly = true)
    public List<ApprovalLineResponse> getMyApprovalLines(Long emplId) {

        List<ApprovalLine> lines =
                approvalLineRepository.findByOwner_EmplId(emplId);

        return lines.stream()
                .map(this::toResponse)
                .toList();
    }


    /**
     * 결재라인 한 건 조회
     */
    @Transactional(readOnly = true)
    public ApprovalLineResponse getApprovalLine(Long lineId, Long emplId) {

        ApprovalLine line = getMyLine(lineId, emplId);

        return toResponse(line);
    }


    /**
     * 결재라인 생성
     */
    @Transactional
    public ApprovalLineResponse createApprovalLine(
            Long emplId,
            ApprovalLineRequest request) {

        validateRequest(emplId, request);

        Employee owner = employeeRepository.findById(emplId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ApprovalLine line = ApprovalLine.builder()
                .owner(owner)
                .lineName(request.getLineName())
                .build();

        approvalLineRepository.save(line);

        saveMembers(line, request.getApproverIds());

        return toResponse(line);
    }


    /**
     * 결재라인 수정
     */
    @Transactional
    public ApprovalLineResponse updateApprovalLine(
            Long lineId,
            Long emplId,
            ApprovalLineRequest request) {

        validateRequest(emplId, request);

        // 본인이 만든 결재라인인지 확인
        ApprovalLine line = getMyLine(lineId, emplId);

        // 이름 수정
        line.setLineName(request.getLineName());

        /*
         * 기존 멤버를 하나하나 비교하지 않고
         * 전부 삭제한 뒤 현재 상태로 다시 저장
         */
        approvalLineMemberRepository
                .deleteByApprovalLine_LineId(lineId);

        approvalLineMemberRepository.flush();

        saveMembers(line, request.getApproverIds());

        return toResponse(line);
    }


    /**
     * 결재라인 삭제
     */
    @Transactional
    public void deleteApprovalLine(Long lineId, Long emplId) {

        ApprovalLine line = getMyLine(lineId, emplId);

        approvalLineMemberRepository
                .deleteByApprovalLine_LineId(lineId);

        approvalLineRepository.delete(line);
    }


    /**
     * 결재자로 선택 가능한 사원 목록
     *
     * 본인 제외
     */
    @Transactional(readOnly = true)
    public List<ApprovalEmployeeResponse> getEmployees(Long emplId) {

        return employeeRepository.findAll()
                .stream()
                .filter(employee ->
                        !Objects.equals(employee.getEmplId(), emplId))
                .map(employee ->
                        ApprovalEmployeeResponse.builder()
                                .emplId(employee.getEmplId())
                                .emplName(employee.getEmplName())
                                .build())
                .toList();
    }


    /**
     * 결재자 저장
     */
    private void saveMembers(
            ApprovalLine line,
            List<Long> approverIds) {

        int order = 1;

        for (Long approverId : approverIds) {

            Employee approver =
                    employeeRepository.findById(approverId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "결재자를 찾을 수 없습니다."
                                    ));

            ApprovalLineMember member =
                    ApprovalLineMember.builder()
                            .approvalLine(line)
                            .approver(approver)
                            .approvalOrder(order++)
                            .build();

            approvalLineMemberRepository.save(member);
        }
    }


    /**
     * 본인이 만든 결재라인인지 확인
     */
    private ApprovalLine getMyLine(
            Long lineId,
            Long emplId) {

        ApprovalLine line =
                approvalLineRepository.findById(lineId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "결재라인이 존재하지 않습니다."
                                ));

        if (!Objects.equals(
                line.getOwner().getEmplId(),
                emplId)) {

            throw new IllegalArgumentException(
                    "본인의 결재라인만 접근할 수 있습니다."
            );
        }

        return line;
    }


    /**
     * 저장 데이터 검증
     */
    private void validateRequest(
            Long emplId,
            ApprovalLineRequest request) {

        if (request.getLineName() == null
                || request.getLineName().isBlank()) {

            throw new IllegalArgumentException(
                    "결재라인 이름을 입력해주세요."
            );
        }

        if (request.getApproverIds() == null
                || request.getApproverIds().isEmpty()) {

            throw new IllegalArgumentException(
                    "결재자를 한 명 이상 선택해주세요."
            );
        }

        // 본인 등록 방지
        if (request.getApproverIds().contains(emplId)) {

            throw new IllegalArgumentException(
                    "본인은 결재자로 등록할 수 없습니다."
            );
        }

        // 같은 사람 중복 등록 방지
        if (new HashSet<>(request.getApproverIds()).size()
                != request.getApproverIds().size()) {

            throw new IllegalArgumentException(
                    "같은 결재자를 중복 등록할 수 없습니다."
            );
        }
    }


    /**
     * Entity -> Response DTO
     */
    private ApprovalLineResponse toResponse(
            ApprovalLine line) {

        List<ApprovalLineMember> members =
                approvalLineMemberRepository
                        .findByApprovalLine_LineIdOrderByApprovalOrderAsc(
                                line.getLineId()
                        );

        List<ApproverResponse> approvers =
                members.stream()
                        .map(member ->
                                ApproverResponse.builder()
                                        .emplId(
                                                member.getApprover()
                                                        .getEmplId()
                                        )
                                        .emplName(
                                                member.getApprover()
                                                        .getEmplName()
                                        )
                                        .approvalOrder(
                                                member.getApprovalOrder()
                                        )
                                        .build())
                        .toList();

        return ApprovalLineResponse.builder()
                .lineId(line.getLineId())
                .lineName(line.getLineName())
                .approvers(approvers)
                .build();
    }
}