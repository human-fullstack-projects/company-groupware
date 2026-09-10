package com.company.groupware.service;


import com.company.groupware.Exception.InvalidChatRoomStateException;
import com.company.groupware.Exception.ResourceNotFoundException;
import com.company.groupware.dto.ChatMessageRequest;
import com.company.groupware.dto.ChatMessageResponse;
import com.company.groupware.entity.ChatRoom;
import com.company.groupware.entity.ChatRoomMessage;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.ChatRoomAffiliationRepository;
import com.company.groupware.repository.ChatRoomMessageRepository;
import com.company.groupware.repository.ChatRoomRepository;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final EmployeeRepository employeeRepository;
    private final ChatRoomMessageRepository chatRoomMessageRepository;
    private final ChatRoomAffiliationRepository affiliationRepository;

    /**
     * 메시지를 저장합니다. 방에 활성 참여 중인 직원만 메시지를 보낼 수 있도록 검증합니다.
     */
    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageRequest request, Long emplId) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "채팅방을 찾을 수 없습니다. room_id=" + request.getRoomId()));

        Employee employee = employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "직원을 찾을 수 없습니다. empl_id=" + emplId));

        boolean isActiveMember = affiliationRepository
                .findByEmployee_EmplIdAndChatRoom_RoomId(emplId, request.getRoomId())
                .filter(affiliation -> affiliation.getRoomOutDate() == null)
                .isPresent();

        if (!isActiveMember) {
            throw new InvalidChatRoomStateException(
                    "해당 방에 참여 중이 아닙니다. empl_id=" + emplId + ", room_id=" + request.getRoomId());
        }

        ChatRoomMessage message = new ChatRoomMessage();
        message.setChatRoom(chatRoom);
        message.setEmployee(employee);
        message.setMessageContent(request.getContent());
        chatRoomMessageRepository.save(message);

        return new ChatMessageResponse(message);
    }

    /**
     * 특정 방의 전체 대화 이력 (오래된 순)
     */
    public List<ChatMessageResponse> getHistory(Long roomId) {
        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다. room_id=" + roomId));

        return chatRoomMessageRepository.findByChatRoom_RoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }
}
