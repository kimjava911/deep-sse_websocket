package kr.java.sse_websocket.chat.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 채팅 이벤트를 Redis Pub/Sub로 발행하는 Publisher.
 */
@Component
@RequiredArgsConstructor
public class RedisChatPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publishRoomMessage(ChatEventPayload payload) {
        try {
            String channel = ChatRedisKeys.channelRoom(payload.getRoomId());
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.convertAndSend(channel, json);
        } catch (Exception e) {
            // 실습 단계: 예외를 삼키지 말고 그대로 올리는 편이 장애 인지가 빠름
            throw new IllegalStateException("Redis publish failed", e);
        }
    }
}
