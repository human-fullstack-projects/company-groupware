package com.company.groupware.repository;

import com.company.groupware.entity.ChatRoomMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMessageRepository extends JpaRepository<ChatRoomMessage, Long> {
}
