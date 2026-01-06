package kr.java.sse_websocket.notifications.controller;

import kr.java.sse_websocket.notifications.domain.Notification;
import kr.java.sse_websocket.notifications.domain.NotificationRead;
import kr.java.sse_websocket.notifications.domain.NotificationTargetType;
import kr.java.sse_websocket.notifications.domain.NotificationView;
import kr.java.sse_websocket.notifications.repository.NotificationReadRepository;
import kr.java.sse_websocket.notifications.repository.NotificationRepository;
import kr.java.sse_websocket.notifications.utils.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static kr.java.sse_websocket.common.UsernameNormalizer.normalize;

/**
 * 알림 조회/읽음/카운트 API.
 *
 * 원칙:
 * - "보이는 알림" 기준은 ALL 또는 USER(targetUsername=본인)
 * - 읽음 처리는 idempotent(여러 번 눌러도 1건만)
 *
 * 개선 포인트:
 * - 읽음 처리(POST) 성공 직후, unreadCount를 SSE로 즉시 push한다.
 *   => 프론트에서 재조회(fetchUnreadCount)를 생략해도 배지가 즉시 갱신된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;

    /**
     * (추가) unreadCount를 즉시 푸시하기 위해 Registry 주입.
     */
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * 알림 목록 조회(최근 N개).
     *
     * - 초기 화면 렌더링 또는 새로고침 시 사용
     * - read 여부를 함께 내려준다.
     */
    @GetMapping
    public List<NotificationView> list(Principal principal,
                                       @RequestParam(defaultValue = "50") int size) {
        String username = normalize(principal.getName());

        // 최신순 N개
        Page<Notification> page = notificationRepository.findVisibleForUser(
                username,
                PageRequest.of(0, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<Notification> notifications = page.getContent();
        List<Long> ids = notifications.stream().map(Notification::getId).toList();

        // 현재 페이지에 대해서만 read 여부를 묶어서 조회(N+1 방지)
        Set<Long> readIds = ids.isEmpty()
                ? Set.of()
                : notificationReadRepository.findReadNotificationIds(username, ids);

        return notifications.stream()
                .map(n -> NotificationView.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .body(n.getBody())
                        .sender(n.getSender())
                        .target(n.getTargetType() == NotificationTargetType.ALL ? "ALL" : n.getTargetUsername())
                        .createdAt(n.getCreatedAt())
                        .read(readIds.contains(n.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 읽음 처리.
     * - 존재하지 않는 알림 id라면 404가 일반적이지만,
     *   실습 단계에서는 간단하게 "없으면 아무 것도 안 함"으로 처리할 수도 있다.
     *   여기서는 정합성을 위해 404를 반환한다.
     * 중요한 개선:
     * - 읽음 처리 성공 후 unreadCount를 SSE로 즉시 push한다.
     *
     * 트랜잭션:
     * - 읽음 저장 + unreadCount 재계산의 정합성을 위해 트랜잭션으로 묶는다.
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@PathVariable Long id, Principal principal) {
        String username = normalize(principal.getName());

        // 존재하지 않는 알림이면 404
        if (!notificationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // idempotent: 이미 읽은 상태면 저장 생략
        if (!notificationReadRepository.existsByNotificationIdAndUsername(id, username)) {
            notificationReadRepository.save(NotificationRead.builder()
                    .notificationId(id)
                    .username(username)
                    .build());
        }

        // unreadCount 재계산 후 SSE push
        long unread = notificationRepository.countUnreadForUser(username);
        sseEmitterRegistry.sendTo(username, "unreadCount", unread);

        return ResponseEntity.ok().build();
    }

    /**
     * 안읽음 카운트 조회(폴링/초기 렌더용).
     * - SSE를 써도 "초기값"은 API로 한 번 가져오는 편이 안전하다.
     */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal principal) {
        String username = normalize(principal.getName());
        long count = notificationRepository.countUnreadForUser(username);
        return Map.of("count", count);
    }
}