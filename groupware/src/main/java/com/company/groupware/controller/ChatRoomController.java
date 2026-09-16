package com.company.groupware.controller;


import com.company.groupware.dto.ChatMessageResponse;
import com.company.groupware.dto.ChatReadEvent;
import com.company.groupware.dto.ChatReadRequest;
import com.company.groupware.dto.ChatRoomCreateRequest;
import com.company.groupware.dto.ChatRoomJoinRequest;
import com.company.groupware.dto.ChatRoomMemberResponse;
import com.company.groupware.dto.ChatRoomResponse;
import com.company.groupware.entity.ChatRoomFile;
import com.company.groupware.service.ChatMessageService;
import com.company.groupware.service.ChatRoomService;
import com.company.groupware.service.ChatAccessService;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;
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

    private final ChatAccessService chatAccessService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    // 채팅방 생성 (개설자 + 초대 멤버 한 번에 참여 처리) - 참여자 전원에게 방 목록 실시간 알림
    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(@RequestBody ChatRoomCreateRequest request, Principal principal) {
        Long actorId = chatAccessService.getHttpEmployee(principal).getEmplId();
        // 요청의 creatorEmplId는 무시하고, 로그인한 본인을 개설자로 사용합니다.
        ChatRoomResponse response = chatRoomService.createRoom(request, actorId);

        notifyRoomListUpdate(actorId, response);
        if (request.getMemberEmplIds() != null) {
            request.getMemberEmplIds().forEach(emplId -> notifyRoomListUpdate(emplId, response));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 로그인한(요청한) 직원이 현재 참여 중인 방 목록
    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(Principal principal) {
        Long emplId = chatAccessService.getHttpEmployee(principal).getEmplId();
        return ResponseEntity.ok(chatRoomService.getMyRooms(emplId));
    }

    // 특정 방의 현재 활성 참여자 목록
    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<ChatRoomMemberResponse>> getMembers(@PathVariable Long roomId, Principal principal) {
        Long emplId = chatAccessService.getHttpEmployee(principal).getEmplId();
        chatAccessService.requireActiveMember(emplId, roomId);
        return ResponseEntity.ok(chatRoomService.getActiveMembers(roomId));
    }

    // 방 입장 (신규 참여 또는 재입장) - 입장 처리 후 입장 시스템 메시지를 저장/브로드캐스트하고
    // 새로 들어온 당사자에게 방 목록 실시간 알림
    @PostMapping("/{roomId}/members")
    public ResponseEntity<ChatRoomMemberResponse> joinRoom(
            @PathVariable Long roomId,
            @RequestBody ChatRoomJoinRequest request, Principal principal) {
        Long actorId = chatAccessService.getHttpEmployee(principal).getEmplId();
        chatAccessService.requireActiveMember(actorId, roomId);
        ChatRoomMemberResponse response = chatRoomService.joinRoom(roomId, request.getEmplId());

        ChatMessageResponse systemMessage = chatMessageService.saveSystemMessage(
                roomId, response.getEmplId(), response.getEmplName() + "님이 입장했습니다");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, systemMessage);

        notifyRoomListUpdate(response.getEmplId(), chatRoomService.getRoomInfo(roomId));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 메시지 읽음 처리 - 읽음 위치 갱신 후 같은 방을 보고 있는 다른 접속자들에게 실시간 알림
    @PostMapping("/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @RequestBody ChatReadRequest request,
            Principal principal) {
        Long emplId = chatAccessService.getHttpEmployee(principal).getEmplId();
        chatAccessService.requireActiveMember(emplId, roomId);
        chatRoomService.markAsRead(roomId, emplId, request.getLastMessageId());

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/read",
                new ChatReadEvent(emplId, request.getLastMessageId()));

        return ResponseEntity.noContent().build();
    }

    // 방의 활성 참여자 전원의 읽음 위치 (emplId -> lastReadMessageId) - 안읽은 인원 수 계산용
    @GetMapping("/{roomId}/read-status")
    public ResponseEntity<Map<Long, Long>> getReadStatus(@PathVariable Long roomId, Principal principal) {
        Long emplId = chatAccessService.getHttpEmployee(principal).getEmplId();
        chatAccessService.requireActiveMember(emplId, roomId);
        return ResponseEntity.ok(chatRoomService.getReadStatus(roomId));
    }

    // 특정 직원의 방 목록 화면(list.html)에 새 방이 생겼음을 개인 큐로 알림
    private void notifyRoomListUpdate(Long emplId, ChatRoomResponse room) {
        messagingTemplate.convertAndSendToUser(String.valueOf(emplId), "/queue/rooms", room);
    }

    // 특정 방의 전체 대화 이력 (오래된 순)
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long roomId, Principal principal) {
        Long emplId = chatAccessService.getHttpEmployee(principal).getEmplId();
        chatAccessService.requireActiveMember(emplId, roomId);
        return ResponseEntity.ok(chatMessageService.getHistory(roomId, emplId));
    }

    // 파일 첨부 메시지 업로드 (방 활성 참여자만 가능) - 저장 후 실시간 브로드캐스트까지 처리
    @PostMapping("/{roomId}/files")
    public ResponseEntity<ChatMessageResponse> uploadFile(
            @PathVariable Long roomId,
            Principal principal,
            @RequestParam("files") List<MultipartFile> files) {
        Long emplId = chatAccessService.getHttpEmployee(principal).getEmplId();
        chatAccessService.requireActiveMember(emplId, roomId);
        // uploadFile로 파일들을 업로드, messagingTemplate로 출력
        ChatMessageResponse response = chatMessageService.uploadFile(roomId, emplId, files);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 첨부파일 다운로드 (방 활성 참여자만 가능)
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            Principal principal) {
        Long emplId = chatAccessService.getHttpEmployee(principal).getEmplId();
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

    // 방 퇴장 - 아직 활성 참여자인 상태에서 퇴장 시스템 메시지를 먼저 저장/브로드캐스트한 뒤 실제 퇴장 처리
    @DeleteMapping("/{roomId}/members/{emplId}")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            @PathVariable Long emplId, Principal principal) {
        Long actorId = chatAccessService.getHttpEmployee(principal).getEmplId();
        if (!Objects.equals(actorId, emplId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인만 채팅방에서 나갈 수 있습니다.");
        }
        chatAccessService.requireActiveMember(actorId, roomId);
        String emplName = chatRoomService.getEmployeeName(emplId);
        ChatMessageResponse systemMessage = chatMessageService.saveSystemMessage(
                roomId, emplId, emplName + "님이 퇴장했습니다");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, systemMessage);

        chatRoomService.leaveRoom(roomId, emplId);

        if(chatRoomService.checkTotalMember(roomId) <= 0) {
            chatRoomService.closeRoom(roomId);
        }

        return ResponseEntity.noContent().build();
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleAccessError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(Map.of("message", error.getReason() == null
                        ? "채팅 요청을 처리할 수 없습니다." : error.getReason()));
    }
}
