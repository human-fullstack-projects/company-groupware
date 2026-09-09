package com.company.groupware.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시판 첨부파일
 */
//@Entity
//@Table(name = "Board_file")
@Getter
@Setter
@NoArgsConstructor
public class BoardFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_file_id")
    private Integer boardFileId; // 파일번호

    @Column(name = "board_file_link", length = 200)
    private String boardFileLink; // 파일저장경로

    @Column(name = "board_file_size")
    private Integer boardFileSize; // 파일크기 (CHECK: >-1)

    @Column(name = "board_file_origin_name", length = 200)
    private String boardFileOriginName; // 파일원본이름

    @Column(name = "board_file_saved_name", length = 200)
    private String boardFileSavedName; // 파일저장이름

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board; // 게시글
}
