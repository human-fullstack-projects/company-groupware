package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 게시판 첨부파일
 */
@Entity
@Table(name = "Board_file", indexes = {
        @Index(name = "IX_Board_file_origin_name", columnList = "board_file_origin_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_file_id")
    private Long boardFileId; // 파일번호

    @Column(name = "board_file_link", length = 200)
    private String boardFileLink; // 파일저장경로

    @Column(name = "board_file_size")
    private Long boardFileSize; // 파일크기 (CHECK: > -1)

    @Column(name = "board_file_origin_name", length = 200)
    private String boardFileOriginName; // 파일원본이름

    @Column(name = "board_file_saved_name", length = 200)
    private String boardFileSavedName; // 파일저장이름

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", foreignKey = @ForeignKey(name = "FK_Board_TO_Board_file"))
    private Board board; // 게시글
}
