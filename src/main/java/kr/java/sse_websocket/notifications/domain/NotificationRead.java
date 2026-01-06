package kr.java.sse_websocket.notifications.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 알림 읽음 상태 엔티티.
 *
 * - 한 사용자가 한 알림을 "읽음" 처리하면 1건 생성된다.
 * - (notificationId, username) 조합은 유일해야 한다(중복 읽음 처리 방지, idempotent).
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_reads",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_reads_notification_username",
                columnNames = {"notificationId", "username"}
        ),
        indexes = {
                @Index(name = "idx_notification_reads_username", columnList = "username"),
                @Index(name = "idx_notification_reads_notification_id", columnList = "notificationId")
        }
)
public class NotificationRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 단순화를 위해 FK 매핑 대신 notificationId(Long)만 저장한다.
     * - 실습 단계에서는 이 방식이 쿼리/구현이 단순하다.
     * - 필요 시 @ManyToOne으로 확장 가능.
     */
    @Column(nullable = false)
    private Long notificationId;

    /**
     * 읽은 사용자 username(Principal.getName()).
     */
    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private Instant readAt;

    @PrePersist
    void prePersist() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}