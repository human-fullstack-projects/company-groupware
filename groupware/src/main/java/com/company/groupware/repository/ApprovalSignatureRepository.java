package com.company.groupware.repository;

import com.company.groupware.entity.ApprovalSignature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalSignatureRepository
        extends JpaRepository<ApprovalSignature, Long> {

    Optional<ApprovalSignature> findByEmplId(Long emplId);
}