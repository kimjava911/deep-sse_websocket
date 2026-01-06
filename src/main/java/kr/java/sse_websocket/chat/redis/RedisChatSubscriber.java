package kr.java.sse_websocket.chat.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 구독자.
 *
 * - 모든 서버 인스턴스가 Redis 채널(chat:room:*)을 구독한다.
 * - 메시지를 수신하면 STOMP 토픽(/topic/chat/rooms/{roomId})으로 브로드캐스트한다.
 *
 * 이 구조의 핵심:
 * - WebSocket 세션은 서버 인스턴스마다 분산되어 있어도,
 *   Redis를 통해 "모든 서버가 동일 이벤트를 받는" 구조가 된다.
 */
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            ChatEventPayload payload = objectMapper.readValue(json, ChatEventPayload.class);

            String topic = "/topic/chat/rooms/" + payload.getRoomId();
            simpMessagingTemplate.convertAndSend(topic, payload);
        } catch (Exception e) {
            // 실습 단계: subscriber 에러는 로그로라도 남기는 것을 권장
            // (여기서는 런타임 예외로 올리면 listener container가 영향을 받을 수 있어 주의)
            System.err.println("[RedisChatSubscriber] parse/broadcast failed: " + e.getMessage());
        }
    }
}