package com.company.groupware.repository;

import com.company.groupware.entity.ChatRoomAffiliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomAffiliationRepository extends JpaRepository<ChatRoomAffiliation, Long> {
    // 특정 직원 + 특정 방에 대한 소속 이력 (탈퇴 이력 포함) 조회 - 유니크 제약(empl_id, room_id) 기준
    Optional<ChatRoomAffiliation> findByEmployee_EmplIdAndChatRoom_RoomId(Long emplId, Long roomId);

    // 특정 방의 현재 활성 참여자 목록 (퇴장하지 않은 사람만)
    List<ChatRoomAffiliation> findByChatRoom_RoomIdAndRoomOutDateIsNull(Long roomId);

    // 특정 직원이 현재 참여 중인 방 목록 (퇴장하지 않은 방만)
    List<ChatRoomAffiliation> findByEmployee_EmplIdAndRoomOutDateIsNull(Long emplId);


}
