package com.company.groupware.controller;

import com.company.groupware.entity.Board;
import com.company.groupware.entity.BoardCategory;
import com.company.groupware.entity.BoardFile;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    private final EmployeeRepository employeeRepository;

    /**
     * 세션에 저장된, 로그인한 사용자가 읽은 게시글 ID 목록을 가져온다.
     * 없으면 새로 만들어서 세션에 저장한다.
     */
    private Set<Long> getReadBoardIds(HttpSession session) {

        @SuppressWarnings("unchecked")
        Set<Long> readIds =
                (Set<Long>) session.getAttribute("readBoardIds");

        if (readIds == null) {

            readIds = new HashSet<>();

            session.setAttribute(
                    "readBoardIds",
                    readIds
            );
        }

        return readIds;
    }

    /**
     * 게시글 목록
     */
    @GetMapping("/boards")
    public String list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "title") String searchType,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        if (page < 0) {
            page = 0;
        }

        int size = 5;

        Page<Board> boardPage =
                boardService.findAll(
                        categoryId,
                        searchType,
                        keyword,
                        page,
                        size
                );

        model.addAttribute(
                "boardPage",
                boardPage
        );

        model.addAttribute(
                "categoryId",
                categoryId
        );

        model.addAttribute(
                "categories",
                boardService.findAllCategories()
        );

        model.addAttribute(
                "searchType",
                searchType
        );

        model.addAttribute(
                "keyword",
                keyword
        );

        model.addAttribute(
                "currentPage",
                page
        );

        int blockSize = 5;
        int currentBlock = page / blockSize;
        int startPage = currentBlock * blockSize;
        int endPage = Math.min(startPage + blockSize - 1, boardPage.getTotalPages() - 1);

        model.addAttribute(
                "startPage",
                startPage
        );

        model.addAttribute(
                "endPage",
                endPage
        );

        model.addAttribute(
                "readBoardIds",
                getReadBoardIds(session)
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
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<MultipartFile> files,
            Authentication authentication) {

        Board board =
                new Board();

        board.setBoardTitle(
                boardTitle
        );

        board.setBoardContent(
                boardContent
        );

        Employee employee =
                employeeRepository.findByLoginId(
                        authentication.getName()
                ).orElse(null);

        board.setEmployee(
                employee
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
            @PathVariable Long boardId,
            HttpSession session,
            Model model) {

        boardService.increaseReadCount(
                boardId
        );

        getReadBoardIds(session).add(
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
            @PathVariable Long boardId,
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
            @PathVariable Long boardId,
            @RequestParam String boardTitle,
            @RequestParam String boardContent,
            @RequestParam(required = false) Long categoryId) {

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
            @PathVariable Long boardId) {

        boardService.delete(
                boardId
        );

        return "redirect:/boards";
    }
}