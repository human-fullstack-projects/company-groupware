package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    // 이 결재라인을 만든 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_empl_id", nullable = false)
    private Employee owner;

    @Column(name = "line_name", nullable = false, length = 100)
    private String lineName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(
            mappedBy = "approvalLine",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ApprovalLineMember> members = new ArrayList<>();


    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}