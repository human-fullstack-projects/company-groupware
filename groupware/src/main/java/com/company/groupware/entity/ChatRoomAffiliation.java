package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방소속 (직원 - 채팅방 참여 이력)
 */
@Entity
@Table(name = "Chat_room_affliation", uniqueConstraints = {
        @UniqueConstraint(name = "UIX_Chat_room_affliation", columnNames = {"empl_id", "room_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomAffiliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId; // 채팅방소속번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empl_id", foreignKey = @ForeignKey(name = "FK_employee_TO_Chat_room_affliation"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", foreignKey = @ForeignKey(name = "FK_chat_room_TO_Chat_room_affliation"))
    private ChatRoom chatRoom; // 채팅방번호

    @Column(name = "room_in_date")
    private LocalDateTime roomInDate; // 참여 일시

    @Column(name = "room_out_date")
    private LocalDateTime roomOutDate; // 퇴장 일시

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId; // 이 방에서 마지막 읽은 메시지 ID (안 읽은 수 표시에 사용)
}
