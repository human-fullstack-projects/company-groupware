package com.company.groupware.repository;

import com.company.groupware.entity.ChatRoomFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomFileRepository extends JpaRepository<ChatRoomFile, Long> {
}
