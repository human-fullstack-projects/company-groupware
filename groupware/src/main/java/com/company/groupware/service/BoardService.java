package com.company.groupware.service;


import com.company.groupware.entity.Board;
import com.company.groupware.entity.BoardCategory;
import com.company.groupware.entity.BoardFile;
import com.company.groupware.repository.BoardCategoryRepository;
import com.company.groupware.repository.BoardFileRepository;
import com.company.groupware.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     */
    public Page<Board> findAll(
            String keyword,
            int page,
            int size) {

        Pageable pageable =
                PageRequest.of(page, size);

        if (keyword == null ||
                keyword.trim().isEmpty()) {

            return boardRepository.findAll(pageable);
        }

        return boardRepository
                .findByBoardTitleContainingOrBoardContentContaining(
                        keyword,
                        keyword,
                        pageable
                );
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

        } else {

            board.setBoardCategory(
                    null
            );
        }
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