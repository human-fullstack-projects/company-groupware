package com.company.groupware.repository;

import com.company.groupware.entity.ApprovalLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalLineRepository
        extends JpaRepository<ApprovalLine, Long> {

    List<ApprovalLine> findByOwner_EmplId(Long emplId);
}