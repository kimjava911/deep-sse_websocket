package kr.java.sse_websocket;

import kr.java.sse_websocket.chat.domain.ChatRoom;
import kr.java.sse_websocket.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChatServiceRoomTest {

    @Autowired
    ChatService chatService;

    @Test
    void getOrCreateRoom_shouldBeIdempotent_forSamePair_evenIfOrderDiffers() {
        ChatRoom r1 = chatService.getOrCreateRoom("user1", "admin");
        ChatRoom r2 = chatService.getOrCreateRoom("admin", "user1");

        assertThat(r1.getId()).isNotNull();
        assertThat(r2.getId()).isNotNull();
        assertThat(r1.getId()).isEqualTo(r2.getId());
    }
}