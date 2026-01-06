package kr.java.sse_websocket.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.java.sse_websocket.chat.domain.ChatMessage;
import kr.java.sse_websocket.chat.domain.ChatRoom;
import kr.java.sse_websocket.chat.redis.ChatRedisKeys;
import kr.java.sse_websocket.chat.repository.ChatMessageRepository;
import kr.java.sse_websocket.chat.repository.ChatRoomRepository;
import kr.java.sse_websocket.chat.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

import static kr.java.sse_websocket.common.UsernameNormalizer.normalize;

/**
 * 채팅 REST API.
 *
 * - 방 생성/조회: POST /api/chat/room
 * - 최근 메시지: GET /api/chat/rooms/{roomId}/messages?size=50
 *
 * 최근 메시지는 Redis cache(List)를 우선 사용하고, 없으면 DB에서 가져온다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping("/room")
    public Map<String, Object> getOrCreateRoom(@RequestBody CreateRoomRequest req, Principal principal) {
        String me = normalize(principal.getName());
        String other = normalize(req.getOtherUsername());

        ChatRoom room = chatService.getOrCreateRoom(me, other);
        return Map.of("roomId", room.getId(), "me", me, "other", other);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatEventDto> recent(@PathVariable Long roomId,
                                     @RequestParam(defaultValue = "50") int size,
                                     Principal principal) {

        assertMember(roomId, principal);

        int limit = Math.min(size, 200);

        // 1) Redis cache 우선 (LPUSH 구조라 최신이 앞, 화면은 오래된→최신 순이 자연스러움)
        List<String> cached = stringRedisTemplate.opsForList()
                .range(ChatRedisKeys.recentListKey(roomId), 0, limit - 1);

        if (cached != null && !cached.isEmpty()) {
            List<ChatEventDto> list = new ArrayList<>();
            for (String json : cached) {
                try {
                    // cache에 저장한 JSON은 ChatEventPayload 구조와 동일
                    ChatEventDto dto = objectMapper.readValue(json, ChatEventDto.class);
                    list.add(dto);
                } catch (Exception ignored) {
                    // 캐시 파싱 실패는 캐시 품질 문제이므로 조용히 무시하고 fallback 할 수 있다.
                }
            }
            // Redis는 최신이 앞이므로 역순 정렬(오래된 -> 최신)
            Collections.reverse(list);
            return list;
        }

        // 2) miss면 DB fallback (DESC로 가져온 뒤 ASC로 정렬)
        List<ChatMessage> fromDb = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, PageRequest.of(0, limit)
        );

        List<ChatMessage> asc = new ArrayList<>(fromDb);
        asc.sort(Comparator.comparing(ChatMessage::getId));

        // DB 결과를 DTO로 변환(캐시 warming은 MVP에서는 생략 가능)
        List<ChatEventDto> result = new ArrayList<>();
        for (ChatMessage m : asc) {
            result.add(ChatEventDto.builder()
                    .roomId(m.getRoomId())
                    .messageId(m.getId())
                    .senderUsername(m.getSenderUsername())
                    .content(m.getContent())
                    .createdAt(m.getCreatedAt().toString())
                    .build());
        }
        return result;
    }

    private void assertMember(Long roomId, Principal principal) {
        String me = normalize(principal.getName());
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("room not found"));

        if (!me.equals(room.getUserA()) && !me.equals(room.getUserB())) {
            throw new IllegalArgumentException("not a member");
        }
    }

    @Data
    public static class CreateRoomRequest {
        private String otherUsername;
    }

    /**
     * Redis cache(JSON)와 바로 매핑하기 위한 DTO
     * - createdAt은 MVP에서 문자열로 받도록 둔다(파싱 이슈 단순화)
     */
    @Data
    @lombok.Builder
    public static class ChatEventDto {
        private Long roomId;
        private Long messageId;
        private String senderUsername;
        private String content;
        private String createdAt;
    }
}