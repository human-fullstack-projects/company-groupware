package com.company.groupware.controller;

import com.company.groupware.domain.Board;
import com.company.groupware.domain.BoardCategory;
import com.company.groupware.domain.BoardFile;
import com.company.groupware.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    /**
     * 게시글 목록
     */
    @GetMapping("/boards")
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        if (page < 0) {
            page = 0;
        }

        int size = 5;

        Page<Board> boardPage =
                boardService.findAll(
                        keyword,
                        page,
                        size
                );

        model.addAttribute(
                "boardPage",
                boardPage
        );

        model.addAttribute(
                "keyword",
                keyword
        );

        model.addAttribute(
                "currentPage",
                page
        );

        return "board/list";
    }

    /**
     * 게시글 작성 화면
     */
    @GetMapping("/boards/create")
    public String createForm(
            Model model) {

        List<BoardCategory> categories =
                boardService.findAllCategories();

        model.addAttribute(
                "categories",
                categories
        );

        return "board/create";
    }

    /**
     * 게시글 작성
     *
     * 게시글과 첨부파일을 함께 저장한다.
     */
    @PostMapping("/boards")
    public String create(
            @RequestParam String boardTitle,
            @RequestParam String boardContent,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) List<MultipartFile> files) {

        Board board =
                new Board();

        board.setBoardTitle(
                boardTitle
        );

        board.setBoardContent(
                boardContent
        );

        if (categoryId != null) {

            BoardCategory category =
                    boardService.findCategoryById(
                            categoryId
                    );

            board.setBoardCategory(
                    category
            );
        }

        boardService.save(
                board,
                files
        );

        return "redirect:/boards";
    }

    /**
     * 게시글 상세
     */
    @GetMapping("/boards/{boardId}")
    public String detail(
            @PathVariable Integer boardId,
            Model model) {

        boardService.increaseReadCount(
                boardId
        );

        Board board =
                boardService.findById(
                        boardId
                );

        /*
         * 게시글에 연결된 첨부파일 조회
         */
        List<BoardFile> boardFiles =
                boardService.findBoardFiles(
                        boardId
                );

        model.addAttribute(
                "board",
                board
        );

        model.addAttribute(
                "boardFiles",
                boardFiles
        );

        return "board/detail";
    }

    /**
     * 첨부파일 다운로드
     */
    @GetMapping("/boards/files/{boardFileId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Integer boardFileId) {

        BoardFile boardFile =
                boardService.findBoardFile(
                        boardFileId
                );

        Resource resource =
                boardService.loadFileAsResource(
                        boardFileId
                );

        String originalName =
                boardFile.getBoardFileOriginName();

        ContentDisposition contentDisposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                originalName,
                                StandardCharsets.UTF_8
                        )
                        .build();

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentDisposition(
                contentDisposition
        );

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .body(resource);
    }

    /**
     * 게시글 수정 화면
     */
    @GetMapping("/boards/{boardId}/edit")
    public String editForm(
            @PathVariable Integer boardId,
            Model model) {

        Board board =
                boardService.findById(
                        boardId
                );

        List<BoardCategory> categories =
                boardService.findAllCategories();

        model.addAttribute(
                "board",
                board
        );

        model.addAttribute(
                "categories",
                categories
        );

        return "board/edit";
    }

    /**
     * 게시글 수정
     */
    @PostMapping("/boards/{boardId}/edit")
    public String edit(
            @PathVariable Integer boardId,
            @RequestParam String boardTitle,
            @RequestParam String boardContent,
            @RequestParam(required = false) Integer categoryId) {

        boardService.update(
                boardId,
                boardTitle,
                boardContent,
                categoryId
        );

        return "redirect:/boards/" + boardId;
    }

    /**
     * 게시글 삭제
     */
    @PostMapping("/boards/{boardId}/delete")
    public String delete(
            @PathVariable Integer boardId) {

        boardService.delete(
                boardId
        );

        return "redirect:/boards";
    }
}