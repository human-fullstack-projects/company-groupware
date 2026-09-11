package com.company.groupware.repository;


import com.company.groupware.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    /**
     * 카테고리(선택) + 검색타입/키워드(선택)로 게시글 조회
     *
     * categoryId가 null이면 카테고리 조건은 무시(전체 카테고리),
     * keyword가 비어있으면 검색 조건도 무시(전체 게시글)한다.
     * readableCategoryIds에 없는 카테고리의 글은 제외한다.
     * 카테고리 없는(과거) 글은 관리자(isAdmin=true)에게만 보인다.
     */
    @Query("""
            SELECT b FROM Board b
            WHERE (:categoryId IS NULL OR b.boardCategory.boardCategoryId = :categoryId)
              AND (
                    (b.boardCategory IS NULL AND :isAdmin = true)
                    OR b.boardCategory.boardCategoryId IN :readableCategoryIds
                  )
              AND (
                    :keyword IS NULL OR :keyword = ''
                    OR (:searchType = 'title'   AND b.boardTitle   LIKE CONCAT('%', :keyword, '%'))
                    OR (:searchType = 'content' AND b.boardContent LIKE CONCAT('%', :keyword, '%'))
                    OR (:searchType = 'author'  AND b.employee.emplName LIKE CONCAT('%', :keyword, '%'))
                    OR (:searchType = 'all'     AND (b.boardTitle LIKE CONCAT('%', :keyword, '%')
                                                  OR b.boardContent LIKE CONCAT('%', :keyword, '%')))
                  )
            """)
    Page<Board> search(
            @Param("categoryId") Long categoryId,
            @Param("readableCategoryIds") List<Long> readableCategoryIds,
            @Param("isAdmin") boolean isAdmin,
            @Param("searchType") String searchType,
            @Param("keyword") String keyword,
            Pageable pageable
    );

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

