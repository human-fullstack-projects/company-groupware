package com.company.groupware.controller;

import com.company.groupware.dto.ApprovalActionRequest;
import com.company.groupware.dto.ApprovalDocumentRequest;
import com.company.groupware.dto.ApprovalDocumentResponse;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

@Controller
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalDocumentController {

    private final ApprovalLineService approvalLineService;
    private final EmployeeRepository employeeRepository;
    private final ApprovalDocumentService approvalDocumentService;
    private final ApprovalSignatureService approvalSignatureService;
    private final ApprovalDocumentAttachmentService approvalDocumentAttachmentService;

//    public ApprovalDocumentController(ApprovalLineService approvalLineService, EmployeeRepository employeeRepository, ApprovalDocumentService approvalDocumentService) {
//        this.approvalLineService = approvalLineService;
//        this.employeeRepository = employeeRepository;
//        this.approvalDocumentService = approvalDocumentService;
//    }

    /**
     * 전자결재 메인 화면
     * - 내가 상신한 문서
     * - 내가 결재할 문서
     */
    @GetMapping
    public String approvalList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication authentication,
            Model model) {

        Long emplId = getLoginEmplId(authentication);

        // 페이지당 표시 건수 제한
        if (pageSize != 5 && pageSize != 10 && pageSize != 20) {
            pageSize = 10;
        }

        // 현재 내가 결재해야 하는 문서
        model.addAttribute(
                "pendingDocuments",
                approvalDocumentService.getPendingDocuments(emplId)
        );

        // 내가 작성한 문서
        var docPage =
                approvalDocumentService.getMyDocuments(
                        emplId,
                        page,
                        pageSize
                );
        model.addAttribute(
                "myDocuments",
                docPage.getContent()
        );
        model.addAttribute(
                "page",
                docPage
        );
        model.addAttribute(
                "pageSize",
                pageSize
        );

        return "approval/document/list";
    }
//    public String approvalList(
//            Authentication authentication,
//            Model model) {
//
//        Long emplId = getLoginEmplId(authentication);
//
//        // 현재 내가 결재해야 하는 문서
//        model.addAttribute(
//                "pendingDocuments",
//                approvalDocumentService.getPendingDocuments(emplId)
//        );
//
//        // 내가 작성한 문서
//        // DRAFT / IN_PROGRESS / APPROVED / REJECTED 전부
//        model.addAttribute(
//                "myDocuments",
//                approvalDocumentService.getMyDocuments(emplId)
//        );
//
//        return "approval/document/list";
//    }

    /**
     * 문서 상신 화면
     */
    @GetMapping("/write")
    public String approvalWrite(
            Authentication authentication,
            Model model) {

        Long emplId = getLoginEmplId(authentication);

        model.addAttribute(
                "approvalLines",
                approvalLineService.getMyApprovalLines(emplId)
        );

        return "approval/document/write";
    }

    /**
     * 임시저장
     */
    @PostMapping("/api/draft")
    @ResponseBody
    public ApprovalDocumentResponse saveDraft(
            @RequestBody ApprovalDocumentRequest request,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalDocumentService.saveDraft(
                emplId,
                request
        );
    }


    /**
     * 문서 상신
     */
    @PostMapping("/api/submit")
    @ResponseBody
    public ApprovalDocumentResponse submitDocument(
            @Valid @RequestBody ApprovalDocumentRequest request,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalDocumentService.submitDocument(
                emplId,
                request
        );
    }

    /**
     * 문서 상세 / 결재 화면
     * 아직 DB 연결 전이므로 documentId만 화면에 전달합니다.
     */
    @GetMapping("/{documentId}")
    public String approvalDetail(
            @PathVariable Long documentId,
            Authentication authentication,
            Model model) {

        Long emplId =
                getLoginEmplId(authentication);

        ApprovalDocumentResponse document =
                approvalDocumentService
                        .getDocument(documentId, emplId);


        boolean isWriter =
                Objects.equals(
                        document.getWriterEmplId(),
                        emplId
                );


        boolean hasAction =
                document.getApprovers()
                        .stream()
                        .anyMatch(step ->
                                "APPROVED".equals(step.getStatus())
                                        || "REJECTED".equals(step.getStatus())
                        );

        /* 작성자 + 아직 아무도 결재 안 함 */
        boolean canEdit = isWriter
                        &&!hasAction
                        &&("DRAFT".equals(document.getStatus())|| "IN_PROGRESS".equals(document.getStatus()));


        /* 현재 내가 PENDING인 결재자인지 확인*/
        boolean canApprove =!isWriter && "IN_PROGRESS".equals(document.getStatus())
                                      && document.getApprovers()
                                        .stream()
                                        .anyMatch(step ->
                                                Objects.equals(
                                                        step.getApproverId(),
                                                        emplId
                                                )
                                                        && "PENDING".equals(
                                                        step.getStatus()
                                                )
                                        );




        model.addAttribute("document",document);

        model.addAttribute("attachments",approvalDocumentAttachmentService.getAttachments(documentId));

        model.addAttribute("isWriter",isWriter);
        model.addAttribute("canEdit",canEdit);
        model.addAttribute("canApprove",canApprove);
        return "approval/document/detail";
    }

    // 결재자 사인 조회하는 부분 추가
    @GetMapping("/{documentId}/signature/{approverId}")
    @ResponseBody
    public ResponseEntity<Resource> approverSignature(
            @PathVariable Long documentId,
            @PathVariable Long approverId,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);


        // 현재 로그인 사용자가 이 문서를 조회할 수 있는지 먼저 확인
        ApprovalDocumentResponse document = approvalDocumentService.getDocument(documentId, emplId);

        // 해당 사람이 이 문서의 결재자인지 + 실제 승인까지 완료한 사람인지 확인
        boolean approvedApprover =
                document.getApprovers()
                        .stream()
                        .anyMatch(step ->
                                Objects.equals(step.getApproverId(), approverId)
                                        && "APPROVED".equals(step.getStatus()));

        if (!approvedApprover) {
            return ResponseEntity.notFound().build();
        }

        //등록된 사인 자체가 없는 경우
        if (approvalSignatureService.getSignature(approverId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = approvalSignatureService.loadSignature(approverId);

            MediaType mediaType = MediaTypeFactory
                                  .getMediaType(resource)
                                  .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok().contentType(mediaType).body(resource);
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /* 문서 수정 */
    @PutMapping("/api/{documentId}")
    @ResponseBody
    public ApprovalDocumentResponse updateDocument(
            @PathVariable Long documentId,
            @RequestBody ApprovalDocumentRequest request,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalDocumentService.updateDocument(
                documentId,
                emplId,
                request
        );
    }

    /* 올린 문서 삭제(결제 전에만 가능) */
    @DeleteMapping("/api/{documentId}")
    @ResponseBody
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication) {
        Long emplId = getLoginEmplId(authentication);
        approvalDocumentService.deleteDocument(documentId, emplId);
        return ResponseEntity.noContent().build();
    }


    /*  문서 승인 */
    @PostMapping("/api/{documentId}/approve")
    @ResponseBody
    public ApprovalDocumentResponse approveDocument(
            @PathVariable Long documentId,
            @RequestBody ApprovalActionRequest request,
            Authentication authentication) {

        Long emplId = getLoginEmplId(authentication);

        return approvalDocumentService
                .approveDocument(documentId, emplId, request.getComment());
    }

    /* 문서 반려 */
    @PostMapping("/api/{documentId}/reject")
    @ResponseBody
    public ApprovalDocumentResponse rejectDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody ApprovalActionRequest request,
            Authentication authentication) {

        Long emplId =
                getLoginEmplId(authentication);

        return approvalDocumentService
                .rejectDocument(documentId, emplId, request.getComment());
    }


    private Long getLoginEmplId(Authentication authentication) {
        Employee employee = employeeRepository
                .findByLoginId(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("로그인 사용자 정보를 찾을 수 없습니다."));
        return employee.getEmplId();
    }
}
