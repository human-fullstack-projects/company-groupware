package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "approval_signature",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_approval_signature_employee",
                        columnNames = "empl_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signature_id")
    private Long signatureId;

    @Column(
            name = "empl_id",
            nullable = false,
            unique = true
    )
    private Long emplId;

    @Column(
            name = "file_path",
            nullable = false,
            length = 500
    )
    private String filePath;

    @Column(
            name = "file_name",
            nullable = false
    )
    private String fileName;
}