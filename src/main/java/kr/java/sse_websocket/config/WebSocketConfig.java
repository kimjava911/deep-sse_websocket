package kr.java.sse_websocket.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 기반 WebSocket 설정.
 *
 * 핵심 개념:
 * - /ws : WebSocket(또는 SockJS) 엔드포인트(브라우저가 연결하는 주소)
 * - /app/** : 클라이언트 -> 서버(@MessageMapping)로 보내는 목적지 prefix
 * - /topic/** : 서버 -> 클라이언트(구독)로 보내는 브로커 prefix
 *
 * 본 단계에서는 단순 브로커(enableSimpleBroker)를 사용한다.
 * (추후 Redis relay 등으로 확장 가능)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * STOMP 엔드포인트 등록.
     * - withSockJS(): SockJS fallback 사용(테스트/호환성 향상)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .withSockJS();
    }

    /**
     * 메시지 브로커 설정.
     * - setApplicationDestinationPrefixes("/app"): 서버로 보내는 prefix
     * - enableSimpleBroker("/topic"): 구독 prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }


    /**
     * (핵심) STOMP 인바운드 채널에 SecurityContextChannelInterceptor 추가.
     * - STOMP 메시지 처리 쓰레드에서 SecurityContextHolder를 채움
     * - @MessageMapping 메서드의 @PreAuthorize가 Authentication을 찾을 수 있게 됨
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new SecurityContextChannelInterceptor());
    }
}