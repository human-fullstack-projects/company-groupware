package com.company.groupware.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시판
 */
@Entity
@Table(name = "Board")
@Getter
@Setter
@NoArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Integer boardId; // 게시글번호

    @Column(name = "board_title", nullable = false, length = 50)
    private String boardTitle; // 게시글제목

    @Lob
    @Column(name = "board_content", nullable = false)
    private String boardContent; // 게시글본문

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(); // 게시글작성날짜

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now(); // 게시글수정날짜

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_category_id")
    private BoardCategory boardCategory; // 카테고리

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empl_id")
    private Employee employee; // 작성자(직원)

    @Column(name = "read_count")
    private Integer readCount = 0; // 조회수

    @Column(name = "board_status")
    private Boolean boardStatus = true; // 게시글상태
}