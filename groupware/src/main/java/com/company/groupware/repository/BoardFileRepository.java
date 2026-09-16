package com.company.groupware.repository;


import com.company.groupware.entity.Board;
import com.company.groupware.entity.BoardFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
//public interface BoardFileRepository extends JpaRepository<BoardFile, Integer> {

public interface BoardFileRepository extends JpaRepository<BoardFile, Long> {
    /**
     * 특정 게시글의 첨부파일 조회
     */
    List<BoardFile> findByBoard(Board board);
}