package kr.java.sse_websocket.notifications.service;

import kr.java.sse_websocket.notifications.domain.Notification;
import kr.java.sse_websocket.notifications.domain.NotificationMessage;
import kr.java.sse_websocket.notifications.domain.NotificationTargetType;
import kr.java.sse_websocket.notifications.repository.NotificationReadRepository;
import kr.java.sse_websocket.notifications.repository.NotificationRepository;
import kr.java.sse_websocket.notifications.utils.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * STEP 02
 * 알림 발송 비즈니스 로직.
 *
 * 책임:
 * - "메시지 객체 생성"과 "SSE로 푸시"를 캡슐화
 *
 * 확장 포인트(다음 단계):
 * - MySQL 저장(JPA) + 읽음 처리 + 안읽음 카운트
 * - 미접속 사용자에 대해 DB 조회로 복구
 */


/**
 * STEP 03
 * 알림 발송 서비스.
 *
 * 이번 단계에서 하는 일:
 * - Notification을 DB에 저장한다(정합성 확보)
 * - 접속 중인 사용자에게 SSE로 notification 이벤트를 푸시한다
 * - unreadCount 이벤트도 함께 푸시한다(배지 갱신)
 *
 * 주의:
 * - 미접속 사용자는 SSE로 못 받지만, DB에 저장되어 있으므로 목록 조회로 복구 가능
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository; // 현재는 unreadCount 쿼리에서 간접 사용(확장 대비)
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * 전체 발송.
     */
    @Transactional
    public NotificationMessage broadcast(String sender, String title, String body) {
        // 1) DB 저장(전체 알림)
        Notification saved = notificationRepository.save(Notification.builder()
                .targetType(NotificationTargetType.ALL)
                .targetUsername(null)
                .title(title)
                .body(body)
                .sender(sender)
                .createdAt(Instant.now())
                .build());

        // 2) SSE 실시간 전송(접속자에게만 delivered=true 의미가 있음)
        // 전체 발송은 "개별 delivered" 개념이 애매하므로, 메시지 자체에는 delivered=false로 두고,
        // 실제 연결 사용자에게는 모두 전송 시도한다.
        NotificationMessage msg = NotificationMessage.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .body(saved.getBody())
                .sender(saved.getSender())
                .target("ALL")
                .createdAt(saved.getCreatedAt())
                .delivered(false)
                .build();

        sseEmitterRegistry.sendToAll("notification", msg);

        // 3) unreadCount는 사용자별로 다르므로, 현재 SSE 연결 중인 사용자에게 각각 계산해서 push
        pushUnreadCountToActiveUsers();

        return msg;
    }

    /**
     * 특정 사용자에게 발송.
     */
    @Transactional
    public NotificationMessage sendToUser(String sender, String targetUsernameRaw, String title, String body) {
        // targetUsername은 정규화된 키 규칙을 맞추는 편이 안정적이다.
        // (Registry도 trim+lowercase를 쓰므로, DB에도 동일하게 저장하면 운영이 편하다)
        String targetUsername = targetUsernameRaw == null ? null : targetUsernameRaw.trim().toLowerCase();

        // 1) DB 저장(타겟 알림)
        Notification saved = notificationRepository.save(Notification.builder()
                .targetType(NotificationTargetType.USER)
                .targetUsername(targetUsername)
                .title(title)
                .body(body)
                .sender(sender)
                .createdAt(Instant.now())
                .build());

        // 2) SSE 전송(타겟에게만)
        NotificationMessage msg = NotificationMessage.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .body(saved.getBody())
                .sender(saved.getSender())
                .target(targetUsername)
                .createdAt(saved.getCreatedAt())
                .delivered(false)
                .build();

        boolean delivered = sseEmitterRegistry.sendTo(targetUsername, "notification", msg);
        msg.setDelivered(delivered);

        // 3) unreadCount push(타겟만)
        long unreadCount = notificationRepository.countUnreadForUser(targetUsername);
        sseEmitterRegistry.sendTo(targetUsername, "unreadCount", unreadCount);

        return msg;
    }

    /**
     * 현재 SSE 연결 중인 사용자에게 unreadCount를 계산해 push한다.
     * - 전체 발송 이후 배지 갱신용
     */
    private void pushUnreadCountToActiveUsers() {
        for (String username : sseEmitterRegistry.activeUsernames()) {
            long unreadCount = notificationRepository.countUnreadForUser(username);
            sseEmitterRegistry.sendTo(username, "unreadCount", unreadCount);
        }
    }
}
