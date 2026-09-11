package com.company.groupware.repository;

import com.company.groupware.entity.ApprovalLineMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalLineMemberRepository
        extends JpaRepository<ApprovalLineMember, Long> {

    List<ApprovalLineMember>
    findByApprovalLine_LineIdOrderByApprovalOrderAsc(Long lineId);

    void deleteByApprovalLine_LineId(Long lineId);
}