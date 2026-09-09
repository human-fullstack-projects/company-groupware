package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방 메시지
 */
@Entity
@Table(name = "chat_room_message", indexes = {
        @Index(name = "IX_chat_room_message", columnList = "message_content")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId; // 채팅방메시지번호

    @Lob
    @Column(name = "message_content")
    private String messageContent; // 채팅방메시지내용

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 메시지전송시간 (DB DEFAULT NOW())

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", foreignKey = @ForeignKey(name = "FK_chat_room_TO_chat_room_message"))
    private ChatRoom chatRoom; // 채팅방

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empl_id", foreignKey = @ForeignKey(name = "FK_employee_TO_chat_room_message"))
    private Employee employee; // 작성자(사번)
}
