package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

//@Entity
//@Table(
//        name = "approval_document_attachment",
//        uniqueConstraints = {
//                @UniqueConstraint(
//                        name = "UK_approval_document_attachment_document",
//                        columnNames = "document_id"
//                )
//        }
//)
@Entity
@Table(name = "approval_document_attachment")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDocumentAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            nullable = false
//            unique = true
    )
    private ApprovalDocument document;

    // 사용자가 올렸던 원래 파일명
    @Column(
            name = "original_name",
            nullable = false,
            length = 255
    )
    private String originalName;

    // 실제 서버에 저장하는 파일명(UUID 사용)
    @Column(
            name = "stored_name",
            nullable = false,
            length = 255
    )
    private String storedName;

    // 상대경로 (uploads/approval-document)
    @Column(
            name = "file_path",
            nullable = false,
            length = 500
    )
    private String filePath;
}