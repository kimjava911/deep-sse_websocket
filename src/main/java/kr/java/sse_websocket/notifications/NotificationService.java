package kr.java.sse_websocket.notifications;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 알림 발송 비즈니스 로직.
 *
 * 책임:
 * - "메시지 객체 생성"과 "SSE로 푸시"를 캡슐화
 *
 * 확장 포인트(다음 단계):
 * - MySQL 저장(JPA) + 읽음 처리 + 안읽음 카운트
 * - 미접속 사용자에 대해 DB 조회로 복구
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * 전체 발송.
     */
    public NotificationMessage broadcast(String sender, String title, String body) {
        NotificationMessage msg = NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .body(body)
                .sender(sender)
                .target("ALL")
                .createdAt(Instant.now())
                .build();

        // SSE로 "notification" 이벤트 전송
        sseEmitterRegistry.sendToAll("notification", msg);
        return msg;
    }

    /**
     * 특정 사용자에게 발송.
     */
    public NotificationMessage sendToUser(String sender, String targetUsername, String title, String body) {
        NotificationMessage msg = NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .body(body)
                .sender(sender)
                .target(targetUsername)
                .createdAt(Instant.now())
                .build();

        boolean delivered = sseEmitterRegistry.sendTo(targetUsername, "notification", msg);
        msg.setDelivered(delivered);

        return msg;
    }
}