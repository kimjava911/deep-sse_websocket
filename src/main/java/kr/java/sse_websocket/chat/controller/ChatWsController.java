package kr.java.sse_websocket.chat.controller;

import kr.java.sse_websocket.chat.domain.ChatRoom;
import kr.java.sse_websocket.chat.repository.ChatRoomRepository;
import kr.java.sse_websocket.chat.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;

import static kr.java.sse_websocket.common.UsernameNormalizer.normalize;

/**
 * 채팅 WS 수신 컨트롤러.
 *
 * - client send: /app/chat/rooms/{roomId}/send
 * - server는 DB 저장 + Redis publish만 수행
 * - 실제 브로드캐스트는 Redis subscriber가 /topic/chat/rooms/{roomId}로 수행
 *
 * 장점:
 * - 서버 인스턴스가 여러 대여도 일관된 실시간 전파 구조
 */
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;

    @PreAuthorize("isAuthenticated()")
    @MessageMapping("/chat/rooms/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatSendRequest req,
                     Principal principal) {

        String me = normalize(principal.getName());

        // 방 멤버 검증(타인 roomId로 스팸 전송 방지)
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("room not found"));

        if (!me.equals(room.getUserA()) && !me.equals(room.getUserB())) {
            throw new IllegalArgumentException("not a member");
        }

        // DB 저장 + Redis publish
        chatService.sendMessage(roomId, me, req.getContent());
    }

    @Data
    public static class ChatSendRequest {
        private String content;
    }
}