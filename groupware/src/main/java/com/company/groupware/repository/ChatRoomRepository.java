package com.company.groupware.repository;

import com.company.groupware.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 관리자용 - 활성 상태(닫히지 않은)인 전체 채팅방 목록
    List<ChatRoom> findByRoomStatTrue();
}
