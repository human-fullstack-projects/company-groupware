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
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

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
     * 로그인한 사용자의 Employee 조회 (권한 판별용)
     */
    private Employee getLoginEmployee(Authentication authentication) {

        return employeeRepository.findByLoginId(
                authentication.getName()
        ).orElse(null);
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
            Authentication authentication,
            HttpSession session,
            Model model) {

        if (page < 0) {
            page = 0;
        }

        int size = 5;

        Employee loginEmployee = getLoginEmployee(authentication);

        Page<Board> boardPage =
                boardService.findAll(
                        categoryId,
                        searchType,
                        keyword,
                        page,
                        size,
                        loginEmployee
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
                boardService.visibleCategories(loginEmployee)
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
            Authentication authentication,
            Model model) {

        Employee employee = getLoginEmployee(authentication);

        List<BoardCategory> categories =
                boardService.writableCategories(employee);

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
            Authentication authentication,
            Model model) {

        Employee employee = getLoginEmployee(authentication);

        BoardCategory category;

        if (categoryId != null) {
            category = boardService.findCategoryById(categoryId);
        } else {
            category = boardService.defaultCategory();
        }

        if (!boardService.canWrite(category, employee)) {

            model.addAttribute(
                    "errorMessage",
                    boardService.writeDenialMessage(category)
            );

            model.addAttribute(
                    "categories",
                    boardService.writableCategories(employee)
            );

            return "board/create";
        }

        Board board =
                new Board();

        board.setBoardTitle(
                boardTitle
        );

        board.setBoardContent(
                boardContent
        );

        board.setEmployee(
                employee
        );

        board.setBoardCategory(
                category
        );

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
            Authentication authentication,
            HttpSession session,
            Model model) {

        Board board =
                boardService.findById(
                        boardId
                );

        Employee employee = getLoginEmployee(authentication);

        if (!boardService.canRead(board.getBoardCategory(), employee)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 게시글을 볼 권한이 없습니다."
            );
        }

        boardService.increaseReadCount(
                boardId
        );

        getReadBoardIds(session).add(
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
                "canModify",
                boardService.canModify(board.getBoardCategory(), employee)
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
            @PathVariable Integer boardFileId,
            Authentication authentication) {

        BoardFile boardFile =
                boardService.findBoardFile(
                        boardFileId
                );

        Employee employee = getLoginEmployee(authentication);

        if (!boardService.canRead(boardFile.getBoard().getBoardCategory(), employee)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 첨부파일을 다운로드할 권한이 없습니다."
            );
        }

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
            Authentication authentication,
            Model model) {

        Board board =
                boardService.findById(
                        boardId
                );

        Employee employee = getLoginEmployee(authentication);

        if (!boardService.canModify(board.getBoardCategory(), employee)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 게시글을 수정할 권한이 없습니다."
            );
        }

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
            @RequestParam(required = false) Long categoryId,
            Authentication authentication) {

        Board board =
                boardService.findById(
                        boardId
                );

        Employee employee = getLoginEmployee(authentication);

        if (!boardService.canModify(board.getBoardCategory(), employee)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 게시글을 수정할 권한이 없습니다."
            );
        }

        if (categoryId != null) {

            BoardCategory newCategory = boardService.findCategoryById(categoryId);

            if (!boardService.canWrite(newCategory, employee)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "이 카테고리로는 변경할 권한이 없습니다."
                );
            }
        }

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
            @PathVariable Long boardId,
            Authentication authentication) {

        Board board =
                boardService.findById(
                        boardId
                );

        Employee employee = getLoginEmployee(authentication);

        if (!boardService.canModify(board.getBoardCategory(), employee)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 게시글을 삭제할 권한이 없습니다."
            );
        }

        boardService.delete(
                boardId
        );

        return "redirect:/boards";
    }
}