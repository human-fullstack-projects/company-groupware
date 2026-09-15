package com.company.groupware.controller;

import com.company.groupware.entity.ApprovalDocumentAttachment;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.ApprovalDocumentAttachmentService;
import com.company.groupware.service.ApprovalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/approvals")
public class ApprovalDocumentAttachmentController {

    private final ApprovalDocumentAttachmentService
            attachmentService;

    private final ApprovalDocumentService
            approvalDocumentService;

    private final EmployeeRepository
            employeeRepository;


    /*
     * 여러 첨부파일 업로드
     */
    @PostMapping(
            value = "/api/{documentId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> uploadAttachments(
            @PathVariable Long documentId,
            @RequestParam("files")
            List<MultipartFile> files,
            Authentication authentication) {

        Long emplId =
                getLoginEmplId(authentication);


        attachmentService.saveAttachments(
                documentId,
                emplId,
                files
        );


        return ResponseEntity.ok()
                .build();
    }


    /*
     * 첨부파일 하나 다운로드
     */
    @GetMapping(
            "/{documentId}/attachments/{attachmentId}/download"
    )
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long documentId,
            @PathVariable Long attachmentId,
            Authentication authentication) {

        Long emplId =
                getLoginEmplId(authentication);


        /*
         * 현재 사용자가 해당 문서를
         * 볼 권한이 있는지 기존 로직 사용
         */
        approvalDocumentService.getDocument(
                documentId,
                emplId
        );


        ApprovalDocumentAttachment attachment =
                attachmentService.getAttachment(
                        documentId,
                        attachmentId
                );


        Resource resource =
                attachmentService
                        .loadAttachment(
                                attachment
                        );


        MediaType mediaType =
                MediaTypeFactory
                        .getMediaType(
                                attachment.getOriginalName()
                        )
                        .orElse(
                                MediaType.APPLICATION_OCTET_STREAM
                        );


        ContentDisposition disposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                attachment.getOriginalName(),
                                StandardCharsets.UTF_8
                        )
                        .build();


        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(resource);
    }


    private Long getLoginEmplId(
            Authentication authentication) {

        Employee employee =
                employeeRepository
                        .findByLoginId(
                                authentication.getName()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "로그인 사용자 정보를 찾을 수 없습니다."
                                )
                        );


        return employee.getEmplId();
    }

    @DeleteMapping(
            "/api/{documentId}/attachments/{attachmentId}"
    )
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long documentId,
            @PathVariable Long attachmentId,
            Authentication authentication) {

        Long emplId =
                getLoginEmplId(authentication);


        attachmentService.deleteAttachment(
                documentId,
                emplId,
                attachmentId
        );


        return ResponseEntity
                .noContent()
                .build();
    }
}