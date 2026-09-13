package com.company.groupware.service;


import com.company.groupware.Exception.InvalidChatRoomStateException;
import com.company.groupware.Exception.ResourceNotFoundException;
import com.company.groupware.dto.ChatMessageRequest;
import com.company.groupware.dto.ChatMessageResponse;
import com.company.groupware.entity.ChatRoom;
import com.company.groupware.entity.ChatRoomFile;
import com.company.groupware.entity.ChatRoomMessage;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomEmployeeRepository employeeRepository;
    private final ChatRoomMessageRepository chatRoomMessageRepository;
    private final ChatRoomAffiliationRepository affiliationRepository;
    private final ChatRoomFileRepository chatRoomFileRepository;

    /**
     * 메시지를 저장합니다. 방에 활성 참여 중인 직원만 메시지를 보낼 수 있도록 검증합니다.
     */
    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageRequest request, Long emplId) {
        ChatRoom chatRoom = getChatRoomOrThrow(request.getRoomId());
        Employee employee = getEmployeeOrThrow(emplId);
        validateActiveMember(emplId, request.getRoomId());

        ChatRoomMessage message = new ChatRoomMessage();
        message.setChatRoom(chatRoom);
        message.setEmployee(employee);
        message.setMessageContent(request.getContent());
        message.setCreatedAt(LocalDateTime.now());
        chatRoomMessageRepository.save(message);

        return new ChatMessageResponse(message);
    }

    /**
     * 파일 첨부 메시지를 저장합니다. 방에 활성 참여 중인 직원만 업로드할 수 있습니다.
     */
    @Transactional
    public ChatMessageResponse uploadFile(Long roomId, Long emplId, List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new IllegalArgumentException("첨부할 파일이 비어 있습니다.");
        }

        ChatRoom chatRoom = getChatRoomOrThrow(roomId);
        Employee employee = getEmployeeOrThrow(emplId);
        validateActiveMember(emplId, roomId);

        ChatRoomMessage message = new ChatRoomMessage();
        message.setChatRoom(chatRoom);
        message.setEmployee(employee);
        message.setCreatedAt(LocalDateTime.now());
        chatRoomMessageRepository.save(message);

        List<ChatRoomFile> chatRoomFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                chatRoomFiles.add(saveChatFile(message, file));
            }
        }

        return new ChatMessageResponse(message, chatRoomFiles);
    }

    private ChatRoomFile saveChatFile(ChatRoomMessage message, MultipartFile file) {
        try {
            Path uploadPath = Paths.get("uploads", "chat").toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "unknown";
            }
            originalName = Paths.get(originalName).getFileName().toString();

            String savedName = UUID.randomUUID() + "_" + originalName;
            Path targetPath = uploadPath.resolve(savedName).normalize();

            if (!targetPath.startsWith(uploadPath)) {
                throw new IllegalArgumentException("잘못된 파일 경로입니다.");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            ChatRoomFile chatRoomFile = new ChatRoomFile();
            chatRoomFile.setChatRoomMessage(message);
            chatRoomFile.setMessageFileLink(targetPath.toString());
            chatRoomFile.setMessageFileSize(file.getSize());
            chatRoomFile.setMessageFileOriginName(originalName);
            chatRoomFile.setMessageFileSavedName(savedName);

            return chatRoomFileRepository.save(chatRoomFile);
        } catch (IOException e) {
            throw new IllegalStateException("첨부파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 첨부파일 다운로드 대상 조회. 파일이 속한 방의 활성 참여자만 다운로드할 수 있도록 검증합니다.
     */
    public ChatRoomFile findChatRoomFile(Long fileId, Long emplId) {
        ChatRoomFile chatRoomFile = chatRoomFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("첨부파일을 찾을 수 없습니다. file_id=" + fileId));

        Long roomId = chatRoomFile.getChatRoomMessage().getChatRoom().getRoomId();
        validateActiveMember(emplId, roomId);

        return chatRoomFile;
    }

    /**
     * 첨부파일을 디스크에서 읽어 Resource 로 반환합니다.
     */
    public Resource loadFileAsResource(ChatRoomFile chatRoomFile) {
        try {
            Path filePath = Paths.get(chatRoomFile.getMessageFileLink()).toAbsolutePath().normalize();

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new IllegalArgumentException("첨부파일을 찾을 수 없습니다.");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("첨부파일을 읽을 수 없습니다.");
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("첨부파일 경로가 잘못되었습니다.", e);
        }
    }

    /**
     * 특정 방의 전체 대화 이력 (오래된 순)
     */
    public List<ChatMessageResponse> getHistory(Long roomId) {
        getChatRoomOrThrow(roomId);

        List<ChatRoomMessage> messages = chatRoomMessageRepository.findByChatRoom_RoomIdOrderByCreatedAtAsc(roomId);

        Map<Long, List<ChatRoomFile>> filesByMessageId = chatRoomFileRepository
                .findByChatRoomMessage_ChatRoom_RoomId(roomId).stream()
                .collect(Collectors.groupingBy(f -> f.getChatRoomMessage().getMessageId()));

        return messages.stream()
                .map(message -> new ChatMessageResponse(message,
                        filesByMessageId.getOrDefault(message.getMessageId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    private void validateActiveMember(Long emplId, Long roomId) {
        boolean isActiveMember = affiliationRepository
                .findByEmployee_EmplIdAndChatRoom_RoomId(emplId, roomId)
                .filter(affiliation -> affiliation.getRoomOutDate() == null)
                .isPresent();

        if (!isActiveMember) {
            throw new InvalidChatRoomStateException(
                    "해당 방에 참여 중이 아닙니다. empl_id=" + emplId + ", room_id=" + roomId);
        }
    }

    private ChatRoom getChatRoomOrThrow(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다. room_id=" + roomId));
    }

    private Employee getEmployeeOrThrow(Long emplId) {
        return employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다. empl_id=" + emplId));
    }
}
