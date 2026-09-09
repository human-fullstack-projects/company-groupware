package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방
 */
@Entity
@Table(name = "chat_room", indexes = {
        @Index(name = "IX_chat_room_name", columnList = "room_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId; // 채팅방번호

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 생성일자 (DB DEFAULT NOW())

    @Column(name = "room_name", length = 200)
    private String roomName; // 채팅방이름

    @Column(name = "room_stat")
    @Builder.Default
    private Boolean roomStat = true; // 채팅방상태
}
