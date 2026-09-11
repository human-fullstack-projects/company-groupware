package com.company.groupware.repository;

import com.company.groupware.entity.ChatRoomFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomFileRepository extends JpaRepository<ChatRoomFile, Long> {
    // 방의 전체 대화 이력에 첨부파일 정보를 함께 채우기 위한 조회
    List<ChatRoomFile> findByChatRoomMessage_ChatRoom_RoomId(Long roomId);
}
