package kr.java.sse_websocket.notifications.controller;


import kr.java.sse_websocket.notifications.utils.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

/**
 * SSE 구독(연결) 엔드포인트.
 *
 * - 브라우저에서 EventSource("/sse/notifications")로 연결
 * - 로그인 세션 기반으로 Principal이 존재해야 하므로 Security 설정에 따라 인증 필요
 */
@RestController
@RequiredArgsConstructor
public class NotificationSseController {

    private final SseEmitterRegistry registry;

    /**
     * 로그인 사용자 기준으로 SSE 연결을 생성/등록한다.
     *
     * 이벤트:
     * - connected: 연결 직후 확인용 1회 이벤트
     */
    @GetMapping("/sse/notifications")
    public SseEmitter subscribe(Principal principal) {
        // Principal은 Spring Security가 세션에서 복원해 제공
        String username = principal.getName();

        SseEmitter emitter = registry.add(username);

        // 연결 확인 이벤트(클라이언트가 정상 연결됐는지 눈으로 확인 가능)
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("ok"));
        } catch (Exception e) {
            // 초기 전송 실패면 연결이 이미 끊긴 경우 -> 제거
            registry.remove(username);
        }

        return emitter;
    }
}