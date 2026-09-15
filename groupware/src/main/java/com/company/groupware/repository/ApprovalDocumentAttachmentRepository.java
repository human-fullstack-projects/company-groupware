package com.company.groupware.repository;

import com.company.groupware.entity.ApprovalDocumentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalDocumentAttachmentRepository
        extends JpaRepository<ApprovalDocumentAttachment, Long> {

//    Optional<ApprovalDocumentAttachment>

    List<ApprovalDocumentAttachment>
    findByDocument_DocumentId(Long documentId);

    // 다운로드 시 해당 문서 소속 파일인지까지 확인
    Optional<ApprovalDocumentAttachment>
    findByAttachmentIdAndDocument_DocumentId(
            Long attachmentId,
            Long documentId
    );


    void deleteByDocument_DocumentId(
            Long documentId
    );


    List<ApprovalDocumentAttachment>
    findByDocument_DocumentIdOrderByAttachmentIdAsc(
            Long documentId
    );

    long countByDocument_DocumentId(
            Long documentId
    );


}