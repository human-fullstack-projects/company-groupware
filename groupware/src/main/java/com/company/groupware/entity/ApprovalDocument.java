package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_empl_id", nullable = false)
    private Employee writer;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /*
     * 문서 종류
     *
     * GENERAL  : 일반 문서
     * VACATION : 휴가 신청서
     * WORKLOG  : 업무일지
     * PROPOSAL : 품의서
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    @Builder.Default
    private ApprovalDocumentType documentType =
            ApprovalDocumentType.GENERAL;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    // 상신 당시 선택한 결재라인 이름을 스냅샷으로 보관
    @Column(name = "approval_line_na", length = 100)
    private String approvalLineName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (documentType == null) {
            documentType = ApprovalDocumentType.GENERAL;
        }
    }
}