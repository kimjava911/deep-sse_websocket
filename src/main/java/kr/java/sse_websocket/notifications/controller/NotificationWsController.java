package kr.java.sse_websocket.notifications.controller;


import kr.java.sse_websocket.notifications.domain.NotificationMessage;
import kr.java.sse_websocket.notifications.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 관리자(Admin)가 WebSocket으로 알림을 발송하기 위한 컨트롤러.
 *
 * 흐름:
 * - Admin 콘솔 JS에서 /app/admin/notifications/broadcast 또는 /app/admin/notifications/target 로 send
 * - 서버가 수신하여 NotificationService로 SSE 발송 수행
 * - 처리 결과(발송된 NotificationMessage)를 /topic/admin/notifications/result로 응답 브로드캐스트
 *
 * 주의:
 * - @PreAuthorize 사용을 위해 SecurityConfig에 @EnableMethodSecurity 필요
 */
@Controller
@RequiredArgsConstructor
public class NotificationWsController {

    private final NotificationService notificationService;

    /**
     * 전체 발송.
     * - ADMIN만 호출 가능
     */
    @PreAuthorize("hasRole('ADMIN')")
    @MessageMapping("/admin/notifications/broadcast") // client send: /app/admin/notifications/broadcast
    @SendTo("/topic/admin/notifications/result")      // server publish: /topic/admin/notifications/result
    public NotificationMessage broadcast(BroadcastRequest req, Principal principal) {
        // sender는 로그인한 admin username 사용
        return notificationService.broadcast(principal.getName(), req.getTitle(), req.getBody());
    }

    /**
     * 유저 지정 발송.
     * - ADMIN만 호출 가능
     */
    @PreAuthorize("hasRole('ADMIN')")
    @MessageMapping("/admin/notifications/target")
    @SendTo("/topic/admin/notifications/result")
    public AdminSendResult target(TargetRequest req, Principal principal) {
        // 사용자 입력은 공백이 섞이는 경우가 매우 흔하므로 정규화
        String target = req.getTargetUsername() == null ? null : req.getTargetUsername().trim();

        NotificationMessage msg =
                notificationService.sendToUser(principal.getName(), target, req.getTitle(), req.getBody());

        // delivered는 "현재 SSE 연결이 있어서 즉시 푸시 성공했는지"를 의미
        return new AdminSendResult(msg, msg.isDelivered());
    }

    @Data
    public static class AdminSendResult {
        private final NotificationMessage message;
        private final boolean delivered;
    }

    /**
     * 전체 발송 요청 DTO.
     */
    @Data
    public static class BroadcastRequest {
        private String title;
        private String body;
    }

    /**
     * 타겟 발송 요청 DTO.
     */
    @Data
    public static class TargetRequest {
        private String targetUsername;
        private String title;
        private String body;
    }
}