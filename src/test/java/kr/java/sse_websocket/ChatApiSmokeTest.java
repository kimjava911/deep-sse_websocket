package kr.java.sse_websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatApiSmokeTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void createRoom_and_fetchRecentMessages_ok() throws Exception {
        // 1) room 생성
        String body = objectMapper.writeValueAsString(new Req("admin"));

        String json = mockMvc.perform(post("/api/chat/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // roomId 추출(간단 파싱)
        long roomId = objectMapper.readTree(json).get("roomId").asLong();

        // 2) 최근 메시지 조회(초기에는 빈 배열이 정상)
        mockMvc.perform(get("/api/chat/rooms/" + roomId + "/messages?size=50"))
                .andExpect(status().isOk());
    }

    static class Req {
        public String otherUsername;
        Req(String otherUsername) { this.otherUsername = otherUsername; }
    }
}
