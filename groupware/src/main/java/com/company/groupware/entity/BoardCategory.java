package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 게시판카테고리
 */
@Entity
@Table(name = "Board_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_category_id")
    private Long boardCategoryId; // 카테고리번호

    @Column(name = "board_category_name", length = 50)
    private String boardCategoryName; // 카테고리이름
}
