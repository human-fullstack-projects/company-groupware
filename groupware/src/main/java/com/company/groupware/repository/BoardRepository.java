package com.company.groupware.repository;


import com.company.groupware.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

    /**
     * 게시글 제목에 검색어가 포함된 게시글 조회
     */
    Page<Board> findByBoardTitleContaining(
            String keyword,
            Pageable pageable
    );

    /**
     * 게시글 내용에 검색어가 포함된 게시글 조회
     */
    Page<Board> findByBoardContentContaining(
            String keyword,
            Pageable pageable
    );

    /**
     * 게시글 제목 또는 내용에 검색어가 포함된 게시글 조회
     */
    Page<Board> findByBoardTitleContainingOrBoardContentContaining(
            String titleKeyword,
            String contentKeyword,
            Pageable pageable
    );

    /**
     * 작성자 이름에 검색어가 포함된 게시글 조회
     */
    Page<Board> findByEmployee_EmplNameContaining(
            String nameKeyword,
            Pageable pageable
    );
}

