package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 채팅방 첨부파일
 */
@Entity
@Table(name = "chat_room_file", indexes = {
        @Index(name = "IX_chat_room_file_origin_name", columnList = "message_file_origin_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_file_id")
    private Long messageFileId; // 채팅방첨부파일번호

    @Lob
    @Column(name = "message_file_link")
    private String messageFileLink; // 파일저장경로

    @Column(name = "message_file_size")
    private Long messageFileSize; // 첨부파일크기 (CHECK: > -1)

    @Column(name = "message_file_origin_name", length = 200)
    private String messageFileOriginName; // 첨부파일원본이름

    @Column(name = "message_file_saved_name", length = 200)
    private String messageFileSavedName; // 첨부파일저장이름

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", foreignKey = @ForeignKey(name = "FK_chat_room_message_TO_chat_room_file"))
    private ChatRoomMessage chatRoomMessage; // 채팅방 메시지
}
