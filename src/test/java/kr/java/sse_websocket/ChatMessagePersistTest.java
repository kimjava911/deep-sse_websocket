package kr.java.sse_websocket;

import kr.java.sse_websocket.chat.domain.ChatMessage;
import kr.java.sse_websocket.chat.domain.ChatRoom;
import kr.java.sse_websocket.chat.repository.ChatMessageRepository;
import kr.java.sse_websocket.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChatMessagePersistTest {

    @Autowired
    ChatService chatService;
    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Test
    void sendMessage_shouldPersistToDb() {
        ChatRoom room = chatService.getOrCreateRoom("user1", "admin");

        ChatMessage saved = chatService.sendMessage(room.getId(), "user1", "hello");
        assertThat(saved.getId()).isNotNull();

        ChatMessage found = chatMessageRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getContent()).isEqualTo("hello");
        assertThat(found.getSenderUsername()).isEqualTo("user1");
        assertThat(found.getRoomId()).isEqualTo(room.getId());
    }
}