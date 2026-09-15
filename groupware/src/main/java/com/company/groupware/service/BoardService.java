package com.company.groupware.service;


import com.company.groupware.entity.Board;
import com.company.groupware.entity.BoardCategory;
import com.company.groupware.entity.BoardFile;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.BoardCategoryRepository;
import com.company.groupware.repository.BoardFileRepository;
import com.company.groupware.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardFileRepository boardFileRepository;

    /**
     * 게시글 전체 조회
     */
    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    /**
     * 게시글 검색 및 페이지네이션
     *
     * searchType에 따라 제목/내용/작성자 중 하나를 기준으로 검색한다.
     */
    public Page<Board> findAll(
            Long categoryId,
            String searchType,
            String keyword,
            int page,
            int size,
            Employee viewer) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "boardId")
                );

        List<Long> readableCategoryIds =
                visibleCategories(viewer).stream()
                        .map(BoardCategory::getBoardCategoryId)
                        .toList();

        return boardRepository.search(
                categoryId,
                readableCategoryIds,
                isAdmin(viewer),
                searchType,
                keyword,
                pageable
        );
    }

    /**
     * 부서공지 카테고리 이름 접미사. "개발공지" 카테고리는 이름에서 이 접미사를 뗀
     * "개발" + "팀" = "개발팀" 소속 직원만 대상이 된다 (부서명 자체가 "OO팀" 형태이므로
     * 접미사만 떼면 바로 부서명이 됨).
     */
    private static final String DEPT_NOTICE_SUFFIX = "공지";

    private boolean isAdmin(Employee viewer) {
        return viewer != null && Boolean.TRUE.equals(viewer.getEmplStat());
    }

    private boolean isGlobalNotice(BoardCategory category) {
        return category != null && "공지".equals(category.getBoardCategoryName());
    }

    private boolean isFreeBoard(BoardCategory category) {
        return category != null && "자유게시판".equals(category.getBoardCategoryName());
    }

    /**
     * 부서공지 카테고리인지 여부. 전사 공지("공지")는 제외하고,
     * 이름이 "OO공지" 형태(접미사로 끝남)면 부서공지로 판단한다.
     */
    private boolean isDeptNotice(BoardCategory category) {
        if (category == null || isGlobalNotice(category)) {
            return false;
        }

        String name = category.getBoardCategoryName();

        return name != null
                && name.length() > DEPT_NOTICE_SUFFIX.length()
                && name.endsWith(DEPT_NOTICE_SUFFIX);
    }

    /**
     * 이 카테고리를 보거나 쓰려면 어느 부서 소속이어야 하는지.
     * null이면 부서 제한이 없는 카테고리(공지/자유게시판/카테고리 없음)임을 의미한다.
     */
    private String requiredDepartmentName(BoardCategory category) {

        if (category == null
                || isGlobalNotice(category)
                || isFreeBoard(category)) {

            return null;
        }

        String name = category.getBoardCategoryName();

        if (isDeptNotice(category)) {
            return name.substring(0, name.length() - DEPT_NOTICE_SUFFIX.length());
        }

        return name;
    }

    private boolean matchesDepartment(String requiredDeptName, Employee viewer) {

        return requiredDeptName != null
                && viewer.getDepartment() != null
                && requiredDeptName.equals(viewer.getDepartment().getDeptName());
    }

    /**
     * 게시글 읽기(조회) 가능 여부
     * - 관리자 → 항상 가능
     * - 카테고리 없음(과거 데이터) → 관리자만 가능
     * - 공지, 자유게시판 → 전 직원 가능
     * - 부서 게시판, 부서공지 → 그 부서 소속 직원만 가능
     */
    public boolean canRead(BoardCategory category, Employee viewer) {

        if (viewer == null) {
            return false;
        }

        if (isAdmin(viewer)) {
            return true;
        }

        if (category == null) {
            return false;
        }

        String requiredDept = requiredDepartmentName(category);

        if (requiredDept == null) {
            return true;
        }

        return matchesDepartment(requiredDept, viewer);
    }

    /**
     * 게시글 쓰기(작성) 가능 여부
     * - 카테고리 없음 / 관리자 → 항상 가능
     * - 공지, 부서공지 → 관리자만 가능
     * - 자유게시판 → 전 직원 가능
     * - 부서 게시판 → 그 부서 소속 직원만 가능
     */
    public boolean canWrite(BoardCategory category, Employee viewer) {

        if (category == null) {
            return true;
        }

        if (viewer == null) {
            return false;
        }

        if (isAdmin(viewer)) {
            return true;
        }

        if (isGlobalNotice(category) || isDeptNotice(category)) {
            return false;
        }

        if (isFreeBoard(category)) {
            return true;
        }

        return matchesDepartment(requiredDepartmentName(category), viewer);
    }

    /**
     * 게시글 수정/삭제 가능 여부
     * - 관리자 → 항상 가능
     * - 자유게시판 → 가능 (기존 동작 유지)
     * - 그 외(공지, 부서 게시판, 부서공지) → 관리자만 가능
     */
    public boolean canModify(BoardCategory category, Employee viewer) {

        if (isAdmin(viewer)) {
            return true;
        }

        return isFreeBoard(category);
    }

    /**
     * 쓰기 권한이 없을 때 사용자에게 보여줄 안내 메시지
     */
    public String writeDenialMessage(BoardCategory category) {

        if (isGlobalNotice(category) || isDeptNotice(category)) {
            return "관리자만 작성할 수 있는 카테고리입니다.";
        }

        return "소속 부서의 게시판에만 글을 작성할 수 있습니다.";
    }

    /**
     * 목록/필터 드롭다운에 보여줄, 이 직원이 읽을 수 있는 카테고리만 걸러서 반환
     */
    public List<BoardCategory> visibleCategories(Employee viewer) {

        return findAllCategories().stream()
                .filter(category -> canRead(category, viewer))
                .toList();
    }

    /**
     * 글쓰기 폼 드롭다운에 보여줄, 이 직원이 쓸 수 있는 카테고리만 걸러서 반환
     */
    public List<BoardCategory> writableCategories(Employee viewer) {

        return findAllCategories().stream()
                .filter(category -> canWrite(category, viewer))
                .toList();
    }

    /**
     * 글쓰기 시 카테고리를 선택하지 않았을 때 대신 넣어줄 기본 카테고리 (자유게시판)
     */
    public BoardCategory defaultCategory() {

        return findAllCategories().stream()
                .filter(this::isFreeBoard)
                .findFirst()
                .orElse(null);
    }

    /**
     * 게시글 저장
     */
    public Board save(Board board) {

        if (board.getCreatedAt() == null) {
            board.setCreatedAt(
                    LocalDateTime.now()
            );
        }

        if (board.getUpdatedAt() == null) {
            board.setUpdatedAt(
                    LocalDateTime.now()
            );
        }

        if (board.getReadCount() == null) {
            board.setReadCount(0L);
        }

        if (board.getBoardStatus() == null) {
            board.setBoardStatus(true);
        }

        return boardRepository.save(board);
    }

    /**
     * 게시글과 첨부파일 저장
     */
    @Transactional
    public Board save(
            Board board,
            List<MultipartFile> files) {

        Board savedBoard =
                save(board);

        if (files == null ||
                files.isEmpty()) {

            return savedBoard;
        }

        for (MultipartFile file : files) {

            if (file == null ||
                    file.isEmpty()) {

                continue;
            }

            saveBoardFile(
                    savedBoard,
                    file
            );
        }

        return savedBoard;
    }

    /**
     * 첨부파일 저장
     */
    private void saveBoardFile(
            Board board,
            MultipartFile file) {

        try {

            Path uploadPath =
                    Paths.get(
                                    "uploads",
                                    "board"
                            )
                            .toAbsolutePath()
                            .normalize();

            Files.createDirectories(
                    uploadPath
            );

            String originalName =
                    file.getOriginalFilename();

            if (originalName == null ||
                    originalName.trim().isEmpty()) {

                originalName = "unknown";
            }

            originalName =
                    Paths.get(originalName)
                            .getFileName()
                            .toString();

            String savedName =
                    UUID.randomUUID()
                            + "_"
                            + originalName;

            Path targetPath =
                    uploadPath
                            .resolve(savedName)
                            .normalize();

            if (!targetPath.startsWith(
                    uploadPath)) {

                throw new IllegalArgumentException(
                        "잘못된 파일 경로입니다."
                );
            }

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            BoardFile boardFile =
                    new BoardFile();

            boardFile.setBoard(board);

            boardFile.setBoardFileLink(
                    targetPath.toString()
            );

            boardFile.setBoardFileSize(
                    file.getSize()
            );

            boardFile.setBoardFileOriginName(
                    originalName
            );

            boardFile.setBoardFileSavedName(
                    savedName
            );

            boardFileRepository.save(
                    boardFile
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "첨부파일 저장 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 게시글 상세 조회
     */
    public Board findById(
            Long boardId) {

        return boardRepository.findById(boardId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "게시글을 찾을 수 없습니다."
                        ));
    }

    /**
     * 특정 게시글의 첨부파일 조회
     */
    public List<BoardFile> findBoardFiles(
            Long boardId) {

        Board board =
                findById(boardId);

        return boardFileRepository
                .findByBoard(board);
    }

    /**
     * 첨부파일 조회
     */
    public BoardFile findBoardFile(
            Integer boardFileId) {

        return boardFileRepository
                .findById(boardFileId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "첨부파일을 찾을 수 없습니다."
                        ));
    }

    /**
     * 첨부파일 Resource 조회
     */
    public Resource loadFileAsResource(
            Integer boardFileId) {

        BoardFile boardFile =
                findBoardFile(boardFileId);

        try {

            Path filePath =
                    Paths.get(
                                    boardFile.getBoardFileLink()
                            )
                            .toAbsolutePath()
                            .normalize();

            if (!Files.exists(filePath) ||
                    !Files.isRegularFile(filePath)) {

                throw new IllegalArgumentException(
                        "첨부파일을 찾을 수 없습니다."
                );
            }

            Resource resource =
                    new UrlResource(
                            filePath.toUri()
                    );

            if (!resource.exists() ||
                    !resource.isReadable()) {

                throw new IllegalArgumentException(
                        "첨부파일을 읽을 수 없습니다."
                );
            }

            return resource;

        } catch (MalformedURLException e) {

            throw new IllegalArgumentException(
                    "첨부파일 경로가 잘못되었습니다.",
                    e
            );
        }
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public void update(
            Long boardId,
            String boardTitle,
            String boardContent) {

        Board board =
                findById(boardId);

        board.setBoardTitle(
                boardTitle
        );

        board.setBoardContent(
                boardContent
        );

        board.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    /**
     * 게시글 수정
     *
     * 제목, 내용, 카테고리를 수정한다.
     */
    @Transactional
    public void update(
            Long boardId,
            String boardTitle,
            String boardContent,
            Long categoryId) {

        Board board =
                findById(boardId);

        board.setBoardTitle(
                boardTitle
        );

        board.setBoardContent(
                boardContent
        );

        board.setUpdatedAt(
                LocalDateTime.now()
        );

        if (categoryId != null) {

            BoardCategory category =
                    findCategoryById(
                            categoryId
                    );

            board.setBoardCategory(
                    category
            );
        }

        // categoryId가 안 넘어오면(예: edit.html에 카테고리 변경 UI가 없는 경우)
        // 기존 카테고리를 그대로 유지한다 — 지우는 것으로 해석하지 않는다.
    }

    /**
     * 게시글 삭제
     */
    public void delete(
            Long boardId) {

        Board board =
                findById(boardId);

        boardRepository.delete(
                board
        );
    }

    /**
     * 조회수 증가
     */
    @Transactional
    public void increaseReadCount(
            Long boardId) {

        Board board =
                findById(boardId);

        Long currentReadCount =
                board.getReadCount();

        if (currentReadCount == null) {
            currentReadCount = 0L;
        }

        board.setReadCount(
                currentReadCount + 1
        );
    }

    /**
     * 카테고리 전체 조회
     */
    public List<BoardCategory> findAllCategories() {

        return boardCategoryRepository.findAll();
    }

    /**
     * 카테고리 조회
     */
    public BoardCategory findCategoryById(
            Long categoryId) {

        return boardCategoryRepository
                .findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "카테고리를 찾을 수 없습니다."
                        ));
    }
}