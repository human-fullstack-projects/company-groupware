package com.company.groupware.repository;

import com.company.groupware.entity.ApprovalDocumentMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalDocumentMemberRepository
        extends JpaRepository<ApprovalDocumentMember, Long> {

    List<ApprovalDocumentMember>
    findByDocument_DocumentIdOrderByApprovalOrderAsc(Long documentId);

    List<ApprovalDocumentMember>
    findByApprover_EmplIdAndStatusOrderByDocument_CreatedAtDesc(
            Long emplId,
            String status
    );

    Optional<ApprovalDocumentMember>
    findByDocument_DocumentIdAndApprover_EmplId(
            Long documentId,
            Long emplId
    );

    Optional<ApprovalDocumentMember>
    findByDocument_DocumentIdAndApprover_EmplIdAndStatus(
            Long documentId,
            Long emplId,
            String status
    );

    Optional<ApprovalDocumentMember>
    findFirstByDocument_DocumentIdAndStatusOrderByApprovalOrderAsc(
            Long documentId,
            String status
    );

    void deleteByDocument_DocumentId(Long documentId);
}
