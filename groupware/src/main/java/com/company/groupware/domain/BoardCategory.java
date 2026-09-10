package com.company.groupware.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시판카테고리
 */
//@Entity
//@Table(name = "Board_category")
@Getter
@Setter
@NoArgsConstructor
public class BoardCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_category_id")
    private Integer boardCategoryId; // 카테고리번호

    @Column(name = "board_category_name", length = 50)
    private String boardCategoryName; // 카테고리이름
}
