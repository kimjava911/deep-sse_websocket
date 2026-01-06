package kr.java.sse_websocket.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

// /api/admin/sse/active-users
@RestController
@RequiredArgsConstructor
public class SseDebugController {

    private final SseEmitterRegistry registry;

    /**
     * 현재 SSE 연결된 사용자 목록을 반환한다.
     * - ADMIN만 접근(실습용 디버그)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/sse/active-users")
    public Set<String> activeUsers() {
        return registry.snapshot().keySet();
    }
}