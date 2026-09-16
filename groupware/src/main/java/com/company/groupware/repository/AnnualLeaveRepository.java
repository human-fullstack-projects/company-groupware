package com.company.groupware.repository;

import com.company.groupware.entity.AnnualLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnualLeaveRepository
        extends JpaRepository<AnnualLeave, Long> {

    Optional<AnnualLeave> findByDocument_DocumentId(Long documentId);

    boolean existsByDocument_DocumentId(Long documentId);
}