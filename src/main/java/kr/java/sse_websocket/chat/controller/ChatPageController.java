package kr.java.sse_websocket.chat.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 채팅 화면 진입용 SSR 컨트롤러.
 * - UI는 Thymeleaf, 실시간/데이터는 REST + WebSocket으로 구성한다.
 */
@Controller
public class ChatPageController {

    @GetMapping("/chat")
    public String chat() {
        return "chat-room";
    }
}

