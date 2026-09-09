package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시판댓글
 */
@Entity
@Table(name = "Comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "com_id")
    private Long comId; // 댓글번호

    @Column(name = "com_content", length = 200)
    private String comContent; // 댓글내용

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 작성날짜 (DB DEFAULT NOW())

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empl_id", foreignKey = @ForeignKey(name = "FK_employee_TO_Comment"))
    private Employee employee; // 작성자(사번)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", foreignKey = @ForeignKey(name = "FK_Board_TO_Comment"))
    private Board board; // 게시글

    // 대댓글(답글) 구조: 자기 자신을 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id2", foreignKey = @ForeignKey(name = "FK_Comment_TO_Comment"))
    private Comment parentComment; // 원댓글(댓글번호2)
}
