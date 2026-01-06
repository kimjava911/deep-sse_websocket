package kr.java.sse_websocket.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class EchoWsController {

    @MessageMapping("/echo")          // client send: /app/echo
    @SendTo("/topic/echo")            // client subscribe: /topic/echo
    public EchoResponse echo(EchoRequest request) {
        return new EchoResponse("echo: " + request.getMessage());
    }

    @Data
    public static class EchoRequest {
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class EchoResponse {
        private String message;
    }
}