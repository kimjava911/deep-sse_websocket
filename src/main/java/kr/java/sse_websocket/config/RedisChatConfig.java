package kr.java.sse_websocket.config;

import kr.java.sse_websocket.chat.redis.ChatRedisKeys;
import kr.java.sse_websocket.chat.redis.RedisChatSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 구독 컨테이너 설정.
 *
 * 조건:
 * - chat.redis.enabled=true일 때만 활성화(테스트 환경에서 Redis 의존 제거)
 */
@Configuration
@RequiredArgsConstructor
public class RedisChatConfig {

    private final RedisConnectionFactory redisConnectionFactory;

    @Bean
    @ConditionalOnProperty(prefix = "chat.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisChatSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);

        // chat:room:* 패턴 구독
        container.addMessageListener(subscriber, new PatternTopic(ChatRedisKeys.channelPatternAllRooms()));
        return container;
    }
}
