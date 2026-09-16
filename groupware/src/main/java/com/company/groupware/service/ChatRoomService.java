package com.company.groupware.service;

import com.company.groupware.Exception.InvalidChatRoomStateException;
import com.company.groupware.Exception.ResourceNotFoundException;
import com.company.groupware.dto.ChatRoomCreateRequest;
import com.company.groupware.dto.ChatRoomMemberResponse;
import com.company.groupware.dto.ChatRoomResponse;
import com.company.groupware.entity.ChatRoom;
import com.company.groupware.entity.ChatRoomAffiliation;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.ChatRoomAffiliationRepository;
import com.company.groupware.repository.ChatRoomRepository;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomAffiliationRepository affiliationRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest request, Long creatorEmplId) {
        Employee creator = employeeRepository.findById(creatorEmplId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "직원을 찾을 수 없습니다. empl_id=" + creatorEmplId));

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomName(request.getRoomName());
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // 개설자 자동 참여
        addMember(chatRoom, creator);

        // 초대 대상 참여 처리 (개설자 중복 방지)
        if (request.getMemberEmplIds() != null) {
            for (Long emplId : request.getMemberEmplIds()) {
                if (Objects.equals(emplId, creator.getEmplId())) {
                    continue;
                }
                Employee member = employeeRepository.findById(emplId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "직원을 찾을 수 없습니다. empl_id=" + emplId));
                addMember(chatRoom, member);
            }
        }

        return new ChatRoomResponse(chatRoom);
    }

    /**
     * 방 입장 처리
     * - 참여 이력이 없으면 신규 생성
     * - 과거에 퇴장했던 이력이 있으면(room_out_date 존재) 재입장으로 갱신
     *   (empl_id + room_id 유니크 제약 때문에 새 row 를 만들 수 없음)
     * - 이미 활성 참여자라면 예외 발생 (중복 입장 방지)
     */
    @Transactional
    public ChatRoomMemberResponse joinRoom(Long roomId, Long emplId) {
        ChatRoom chatRoom = getChatRoomOrThrow(roomId);
        Employee employee = getEmployeeOrThrow(emplId);

        ChatRoomAffiliation affiliation = affiliationRepository
                .findByEmployee_EmplIdAndChatRoom_RoomId(emplId, roomId)
                .orElse(null);

        if (affiliation == null) {
            affiliation = addMember(chatRoom, employee);
        } else if (affiliation.getRoomOutDate() == null) {
            throw new InvalidChatRoomStateException("이미 참여 중인 방입니다. empl_id=" + emplId + ", room_id=" + roomId);
        } else {
            // 재입장: 이전 퇴장 이력을 초기화하고 다시 입장 처리
            affiliation.setRoomInDate(LocalDateTime.now());
            affiliation.setRoomOutDate(null);
        }

        return new ChatRoomMemberResponse(affiliation);
    }

    /**
     * 방 퇴장 처리 (row 삭제가 아니라 room_out_date 를 채워서 이력을 남김)
     */
    @Transactional
    public void leaveRoom(Long roomId, Long emplId) {
        ChatRoomAffiliation affiliation = affiliationRepository
                .findByEmployee_EmplIdAndChatRoom_RoomId(emplId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "참여 이력이 없습니다. empl_id=" + emplId + ", room_id=" + roomId));

        if (affiliation.getRoomOutDate() != null) {
            throw new InvalidChatRoomStateException("이미 퇴장한 방입니다. empl_id=" + emplId + ", room_id=" + roomId);
        }

        affiliation.setRoomOutDate(LocalDateTime.now());
    }

    /**
     * 특정 방의 현재 활성 참여자 목록
     */
    public List<ChatRoomMemberResponse> getActiveMembers(Long roomId) {
        getChatRoomOrThrow(roomId); // 존재 여부만 확인
        return affiliationRepository.findByChatRoom_RoomIdAndRoomOutDateIsNull(roomId).stream()
                .map(ChatRoomMemberResponse::new)
                .collect(Collectors.toList());
    }


    /**
     * 특정 직원이 현재 참여 중인 방 목록
     */
    public List<ChatRoomResponse> getMyRooms(Long emplId) {
        getEmployeeOrThrow(emplId); // 존재 여부만 확인
        return affiliationRepository.findByEmployee_EmplIdAndRoomOutDateIsNull(emplId).stream()
                .map(ChatRoomAffiliation::getChatRoom)
                .map(ChatRoomResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * 입장/퇴장 시스템 메시지 문구에 쓸 직원 이름 조회
     */
    public String getEmployeeName(Long emplId) {
        return getEmployeeOrThrow(emplId).getEmplName();
    }

    /**
     * 방 목록 실시간 알림(개인 큐 전송)에 쓸 방 정보 조회
     */
    public ChatRoomResponse getRoomInfo(Long roomId) {
        return new ChatRoomResponse(getChatRoomOrThrow(roomId));
    }

    /**
     * 읽음 위치 갱신 - 역행 방지(이미 더 최신 값이 저장돼 있으면 무시)
     */
    @Transactional
    public void markAsRead(Long roomId, Long emplId, Long lastMessageId) {
        ChatRoomAffiliation affiliation = affiliationRepository
                .findByEmployee_EmplIdAndChatRoom_RoomId(emplId, roomId)
                .filter(a -> a.getRoomOutDate() == null)
                .orElseThrow(() -> new InvalidChatRoomStateException(
                        "해당 방에 참여 중이 아닙니다. empl_id=" + emplId + ", room_id=" + roomId));

        if (lastMessageId == null) {
            return;
        }
        Long current = affiliation.getLastReadMessageId();
        if (current == null || current < lastMessageId) {
            affiliation.setLastReadMessageId(lastMessageId);
        }
    }

    /**
     * 방의 활성 참여자 전원의 읽음 위치 (emplId -> lastReadMessageId, 아직 하나도 안 읽었으면 0)
     */
    public Map<Long, Long> getReadStatus(Long roomId) {
        getChatRoomOrThrow(roomId);
        return affiliationRepository.findByChatRoom_RoomIdAndRoomOutDateIsNull(roomId).stream()
                .collect(Collectors.toMap(
                        a -> a.getEmployee().getEmplId(),
                        a -> a.getLastReadMessageId() == null ? 0L : a.getLastReadMessageId()
                ));
    }

    private ChatRoomAffiliation addMember(ChatRoom chatRoom, Employee employee) {
        ChatRoomAffiliation affiliation = new ChatRoomAffiliation();
        affiliation.setChatRoom(chatRoom);
        affiliation.setEmployee(employee);
        affiliation.setRoomInDate(LocalDateTime.now());
        return affiliationRepository.save(affiliation);
    }

    private ChatRoom getChatRoomOrThrow(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다. room_id=" + roomId));
    }

    private Employee getEmployeeOrThrow(Long emplId) {
        return employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다. empl_id=" + emplId));
    }

    /**
     * 방을 닫음 처리 (모든 인원이 나갔을 때 호출) - row 삭제가 아니라 room_stat 을 false 로 변경
     */
    @Transactional
    public void closeRoom(Long roomId) {
        ChatRoom chatRoom = getChatRoomOrThrow(roomId);
        chatRoom.setRoomStat(false);
    }

    public int checkTotalMember(Long roomId) {
        return affiliationRepository.findByChatRoom_RoomIdAndRoomOutDateIsNull(roomId).size();
    }
}
