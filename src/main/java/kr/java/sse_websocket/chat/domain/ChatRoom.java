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
@Table(name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_rooms_pair",
                columnNames = {"userA", "userB"}
        ),
        indexes = {
                @Index(name = "idx_chat_rooms_userA", columnList = "userA"),
                @Index(name = "idx_chat_rooms_userB", columnList = "userB")
        }
)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // username 정규화된 값만 저장(예: trim + lowercase)
    @Column(nullable = false, length = 100)
    private String userA;

    @Column(nullable = false, length = 100)
    private String userB;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
