package kr.java.sse_websocket.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS는 선택이지만, 브라우저 호환/테스트 편의 때문에 켜두는 편이 좋습니다.
        registry.addEndpoint("/ws")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트 → 서버(컨트롤러)로 보낼 prefix
        registry.setApplicationDestinationPrefixes("/app");
        // 서버 → 클라이언트(구독)로 보낼 브로커 prefix
        registry.enableSimpleBroker("/topic");
    }
}