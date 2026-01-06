package kr.java.sse_websocket.notifications.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 알림 엔티티.
 *
 * - targetType=ALL: 전체 공지
 * - targetType=USER: 특정 사용자(targetUsername)에게만 노출
 *
 * username 기반(A) 구현이므로 targetUsername은 username 문자열을 저장한다.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_created_at", columnList = "createdAt"),
        @Index(name = "idx_notifications_target", columnList = "targetType,targetUsername")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ALL / USER
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationTargetType targetType;

    /**
     * targetType=USER일 때만 값이 존재.
     * targetType=ALL이면 null.
     */
    @Column(length = 100)
    private String targetUsername;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    /**
     * 발송자 username (admin 등)
     */
    @Column(nullable = false, length = 100)
    private String sender;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}