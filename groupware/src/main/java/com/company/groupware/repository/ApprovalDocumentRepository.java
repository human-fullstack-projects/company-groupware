package com.company.groupware.repository;

import com.company.groupware.entity.ApprovalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

//public interface ApprovalDocumentRepository
//        extends JpaRepository<ApprovalDocument, Long> {
//
//    List<ApprovalDocument> findByWriter_EmplIdOrderByCreatedAtDesc(Long emplId);
//}

public interface ApprovalDocumentRepository
        extends JpaRepository<ApprovalDocument, Long> {

    Page<ApprovalDocument> findByWriter_EmplIdOrderByCreatedAtDesc(
            Long emplId,
            Pageable pageable
    );
}