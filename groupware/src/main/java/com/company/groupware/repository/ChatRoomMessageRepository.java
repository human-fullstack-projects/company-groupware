package com.company.groupware.repository;

import com.company.groupware.entity.ChatRoomMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRoomMessageRepository extends JpaRepository<ChatRoomMessage, Long> {
    List<ChatRoomMessage> findByChatRoom_RoomIdOrderByCreatedAtAsc(Long roomId);

    List<ChatRoomMessage> findByChatRoom_RoomIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(Long roomId, LocalDateTime since);
}
