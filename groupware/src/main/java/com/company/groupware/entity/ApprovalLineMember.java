package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_line_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalLineMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_member_id")
    private Long lineMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", nullable = false)
    private ApprovalLine approvalLine;

    // 실제 결재자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Column(name = "approval_order", nullable = false)
    private int approvalOrder;
}