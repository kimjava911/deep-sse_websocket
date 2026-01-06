package kr.java.sse_websocket.chat.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_room_created", columnList = "roomId,createdAt"),
                @Index(name = "idx_chat_messages_room_id", columnList = "roomId")
        }
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 단순화를 위해 FK 매핑 대신 roomId만 저장
    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false, length = 100)
    private String senderUsername;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}