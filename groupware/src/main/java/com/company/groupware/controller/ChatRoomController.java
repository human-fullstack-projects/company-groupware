package com.company.groupware.controller;


import com.company.groupware.dto.ChatMessageResponse;
import com.company.groupware.dto.ChatRoomCreateRequest;
import com.company.groupware.dto.ChatRoomJoinRequest;
import com.company.groupware.dto.ChatRoomMemberResponse;
import com.company.groupware.dto.ChatRoomResponse;
import com.company.groupware.entity.ChatRoomFile;
import com.company.groupware.service.ChatMessageService;
import com.company.groupware.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    // 채팅방 생성 (개설자 + 초대 멤버 한 번에 참여 처리)
    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(@RequestBody ChatRoomCreateRequest request) {
        ChatRoomResponse response = chatRoomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 로그인한(요청한) 직원이 현재 참여 중인 방 목록
    // TODO: Spring Security 연동 후 emplId 파라미터 대신 인증 principal 사용
    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(@RequestParam Long emplId) {
        return ResponseEntity.ok(chatRoomService.getMyRooms(emplId));
    }

    // 특정 방의 현재 활성 참여자 목록
    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<ChatRoomMemberResponse>> getMembers(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getActiveMembers(roomId));
    }

    // 방 입장 (신규 참여 또는 재입장)
    @PostMapping("/{roomId}/members")
    public ResponseEntity<ChatRoomMemberResponse> joinRoom(
            @PathVariable Long roomId,
            @RequestBody ChatRoomJoinRequest request) {
        ChatRoomMemberResponse response = chatRoomService.joinRoom(roomId, request.getEmplId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 특정 방의 전체 대화 이력 (오래된 순)
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long roomId, @RequestParam Long emplId) {
        return ResponseEntity.ok(chatMessageService.getHistory(roomId, emplId));
    }

    // 파일 첨부 메시지 업로드 (방 활성 참여자만 가능) - 저장 후 실시간 브로드캐스트까지 처리
    // TODO: Spring Security 연동 후 emplId 파라미터 대신 인증 principal 사용
    @PostMapping("/{roomId}/files")
    public ResponseEntity<ChatMessageResponse> uploadFile(
            @PathVariable Long roomId,
            @RequestParam Long emplId,
            @RequestParam("files") List<MultipartFile> files) {
        // uploadFile로 파일들을 업로드, messagingTemplate로 출력
        ChatMessageResponse response = chatMessageService.uploadFile(roomId, emplId, files);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 첨부파일 다운로드 (방 활성 참여자만 가능)
    // TODO: Spring Security 연동 후 emplId 파라미터 대신 인증 principal 사용
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @RequestParam Long emplId) {
        ChatRoomFile chatRoomFile = chatMessageService.findChatRoomFile(fileId, emplId);
        Resource resource = chatMessageService.loadFileAsResource(chatRoomFile);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(chatRoomFile.getMessageFileOriginName(), StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // 방 퇴장
    @DeleteMapping("/{roomId}/members/{emplId}")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            @PathVariable Long emplId) {
        chatRoomService.leaveRoom(roomId, emplId);

        if(chatRoomService.checkTotalMember(roomId) <= 0) {
            chatRoomService.closeRoom(roomId);
        }

        return ResponseEntity.noContent().build();
    }
}
