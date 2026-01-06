package kr.java.sse_websocket.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * Thymeleaf 페이지 라우팅 전용 컨트롤러.
 *
 * - 실제 데이터 처리는 REST/SSE/WS 컨트롤러가 담당
 * - 여기서는 단순히 템플릿(view)만 반환
 */
@Controller
public class PageController {

    /**
     * ADMIN 전용 페이지(/admin) - 권한은 SecurityConfig에서 제어
     */
    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    /**
     * WS Echo 구동 체크 페이지
     */
//    @GetMapping("/chat")
//    public String chat() {
//        return "chat";
//    }

    /**
     * USER 알림 인박스(SSE 수신) 페이지
     */
    @GetMapping("/notifications/sse")
    public String notificationsSse() {
        return "notifications-sse";
    }

    /**
     * ADMIN 발송 콘솔(WS로 전체/타겟 발송) 페이지
     */
    @GetMapping("/admin/console")
    public String adminConsole() {
        return "admin-console";
    }
}