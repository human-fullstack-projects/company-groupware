package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_document_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDocumentMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_member_id")
    private Long documentMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private ApprovalDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Column(name = "approval_order", nullable = false)
    private int approvalOrder;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "WAITING";

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "comment", length = 500)
    private String comment;
}
