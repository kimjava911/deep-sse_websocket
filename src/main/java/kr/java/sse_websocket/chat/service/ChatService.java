package kr.java.sse_websocket.chat.service;

import kr.java.sse_websocket.chat.redis.ChatEventPayload;
import kr.java.sse_websocket.chat.domain.ChatMessage;
import kr.java.sse_websocket.chat.domain.ChatRoom;
import kr.java.sse_websocket.chat.repository.ChatMessageRepository;
import kr.java.sse_websocket.chat.repository.ChatRoomRepository;
import kr.java.sse_websocket.chat.redis.ChatRedisKeys;
import kr.java.sse_websocket.chat.redis.RedisChatPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static kr.java.sse_websocket.common.UsernameNormalizer.normalize;

/**
 * 채팅 서비스.
 *
 * 책임:
 * 1) 메시지 DB 저장(정합성)
 * 2) Redis recent cache 갱신(초기 로딩 성능)
 * 3) Redis Pub/Sub publish(Scale-out 실시간 전파)
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisChatPublisher redisChatPublisher;

    @Value("${chat.redis.recent-cache-size:100}")
    private int recentCacheSize;

    @Transactional
    public ChatRoom getOrCreateRoom(String username1, String username2) {
        String u1 = normalize(username1);
        String u2 = normalize(username2);

        String userA = (u1.compareTo(u2) <= 0) ? u1 : u2;
        String userB = (u1.compareTo(u2) <= 0) ? u2 : u1;

        return chatRoomRepository.findByUserAAndUserB(userA, userB)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .userA(userA)
                        .userB(userB)
                        .build()));
    }

    /**
     * 메시지 전송(저장 + 캐시 + publish)
     */
    @Transactional
    public ChatMessage sendMessage(Long roomId, String senderUsername, String content) {
        String sender = normalize(senderUsername);

        // 1) DB 저장
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .roomId(roomId)
                .senderUsername(sender)
                .content(content)
                .build());

        // 2) Redis recent cache 저장(문자열로 간단히 캐싱)
        //    - MVP에서는 JSON 직렬화를 간단히 하기 위해 payload JSON을 list에 넣는 편이 낫다.
        ChatEventPayload payload = ChatEventPayload.builder()
                .roomId(roomId)
                .messageId(saved.getId())
                .senderUsername(saved.getSenderUsername())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .build();

        cacheRecent(roomId, payload);

        // 3) Redis publish(모든 서버에서 subscriber가 받아서 WebSocket 브로드캐스트)
        redisChatPublisher.publishRoomMessage(payload);

        return saved;
    }

    /**
     * 최근 메시지 DB 로딩(캐시 miss 시 fallback).
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> loadRecentFromDb(Long roomId, int size) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, PageRequest.of(0, Math.min(size, 200))
        );
    }

    private void cacheRecent(Long roomId, ChatEventPayload payload) {
        try {
            // payload를 Redis list에 넣기 위해 publish와 동일한 JSON 형태를 권장
            // (여기서는 publisher가 ObjectMapper를 갖고 있으니, 캐시도 publisher를 통해 json을 만드는 방식이 더 깔끔하지만
            //  MVP에서는 단순화를 위해 convertAndSend와 별개로 저장은 StringRedisTemplate로만 처리)
            String key = ChatRedisKeys.recentListKey(roomId);

            // StringRedisTemplate는 "값은 문자열" 전제이므로, publisher 쪽 JSON 생성과 동일하게 만들려면
            // ObjectMapper를 여기서도 쓰는 게 베스트지만, MVP에서는 publish에서 만든 JSON 재사용이 어렵다.
            // 따라서 cache는 "messageId|sender|content" 형태로도 가능하나, 아래는 payload.toString()을 피하기 위해 최소 JSON을 직접 구성한다.
            String json = "{\"roomId\":" + payload.getRoomId()
                    + ",\"messageId\":" + payload.getMessageId()
                    + ",\"senderUsername\":\"" + escapeJson(payload.getSenderUsername())
                    + "\",\"content\":\"" + escapeJson(payload.getContent())
                    + "\",\"createdAt\":\"" + payload.getCreatedAt() + "\"}";

            // 최신이 앞: LPUSH
            stringRedisTemplate.opsForList().leftPush(key, json);
            // 최근 N개 유지: LTRIM
            stringRedisTemplate.opsForList().trim(key, 0, recentCacheSize - 1);
        } catch (Exception e) {
            // 캐시는 실패해도 DB 정합성은 유지되므로, MVP에서는 로그만
            System.err.println("[ChatService] cacheRecent failed: " + e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
