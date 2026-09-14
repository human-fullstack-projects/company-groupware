package com.company.groupware.controller;

import com.company.groupware.entity.ApprovalSignature;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.service.ApprovalSignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/approval-signatures")
public class ApprovalSignatureController {

    private final ApprovalSignatureService signatureService;
    private final EmployeeRepository employeeRepository;


    /*
     * 현재 로그인 사용자의 사인 정보
     */
    @GetMapping("/api")
    public ResponseEntity<?> getSignature(
            Authentication authentication) {

        Long emplId =
                getLoginEmplId(authentication);


        return signatureService
                .getSignature(emplId)
                .<ResponseEntity<?>>map(signature ->
                        ResponseEntity.ok(
                                Map.of(
                                        "exists", true,
                                        "fileName",
                                        signature.getFileName()
                                )
                        )
                )
                .orElseGet(() ->
                        ResponseEntity.ok(
                                Map.of(
                                        "exists", false
                                )
                        )
                );
    }


    /*
     * 신규 등록 또는 교체
     */
    @PostMapping(
            value = "/api",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> saveSignature(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Long emplId =
                getLoginEmplId(authentication);


        ApprovalSignature signature =
                signatureService.saveSignature(
                        emplId,
                        file
                );


        return ResponseEntity.ok(
                Map.of(
                        "fileName",
                        signature.getFileName()
                )
        );
    }


    /*
     * 사인 이미지 표시
     */
    @GetMapping("/image")
    public ResponseEntity<Resource> image(
            Authentication authentication) {

        Long emplId =
                getLoginEmplId(authentication);

        Resource resource =
                signatureService.loadSignature(
                        emplId
                );


        return ResponseEntity.ok()
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .body(resource);
    }


    /*
     * 삭제
     */
    @DeleteMapping("/api")
    public ResponseEntity<Void> deleteSignature(
            Authentication authentication) {

        Long emplId =
                getLoginEmplId(authentication);

        signatureService.deleteSignature(
                emplId
        );

        return ResponseEntity.noContent()
                .build();
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
}