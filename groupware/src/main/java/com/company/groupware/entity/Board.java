package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시판
 */
@Entity
@Table(name = "Board", indexes = {
        @Index(name = "IX_Board_title", columnList = "board_title"),
        @Index(name = "IX_Board_content", columnList = "board_content")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long boardId; // 게시글번호

    @Column(name = "board_title", length = 50, nullable = false)
    private String boardTitle; // 게시글제목

    @Lob
    @Column(name = "board_content", nullable = false)
    private String boardContent; // 게시글본문

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 게시글작성날짜 (DB DEFAULT NOW())

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 게시글수정날짜 (DB DEFAULT NOW())

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_category_id", foreignKey = @ForeignKey(name = "FK_Board_category_TO_Board"))
    private BoardCategory boardCategory; // 카테고리

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empl_id", foreignKey = @ForeignKey(name = "FK_employee_TO_Board"))
    private Employee employee; // 작성자(사번)

    @Column(name = "read_count")
    private Long readCount; // 조회수 (CHECK: >= 0)

    @Column(name = "board_status")
    @Builder.Default
    private Boolean boardStatus = true; // 게시글상태
}
