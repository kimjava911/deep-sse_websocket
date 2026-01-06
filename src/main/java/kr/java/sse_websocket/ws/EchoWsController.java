package kr.java.sse_websocket.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 구동 체크용 Echo 컨트롤러.
 *
 * 흐름:
 * - 브라우저가 /app/echo 로 메시지 전송(send)
 * - 서버가 이를 받아 echo 가공 후 /topic/echo 로 브로드캐스트(sendTo)
 * - 브라우저는 /topic/echo 를 subscribe 하여 수신
 */
@Controller
public class EchoWsController {

    @MessageMapping("/echo")       // client sends to: /app/echo
    @SendTo("/topic/echo")         // server publishes to: /topic/echo
    public EchoResponse echo(EchoRequest request) {
        return new EchoResponse("echo: " + request.getMessage());
    }

    /**
     * 요청 DTO(JSON -> Object 매핑)
     */
    @Data
    public static class EchoRequest {
        private String message;
    }

    /**
     * 응답 DTO(Object -> JSON)
     */
    @Data
    @AllArgsConstructor
    public static class EchoResponse {
        private String message;
    }
}