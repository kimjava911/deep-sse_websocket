package kr.java.sse_websocket.chat.redis;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Redis Pub/Sub로 흘려보내는 이벤트 페이로드.
 * - 모든 서버 인스턴스가 이 payload를 받아 WebSocket 토픽으로 브로드캐스트한다.
 */
@Data
@Builder
public class ChatEventPayload {
    private Long roomId;
    private Long messageId;
    private String senderUsername;
    private String content;
    private Instant createdAt;
}