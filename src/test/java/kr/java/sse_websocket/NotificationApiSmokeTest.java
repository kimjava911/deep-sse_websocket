package kr.java.sse_websocket;

import kr.java.sse_websocket.notifications.domain.Notification;
import kr.java.sse_websocket.notifications.domain.NotificationTargetType;
import kr.java.sse_websocket.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 알림 API 최소 스모크 테스트.
 *
 * - 목록 조회: 인증 사용자면 200
 * - 읽음 처리: 존재하는 알림이면 200
 * - 안읽음 카운트: 200
 *
 * 주의:
 * - DB를 실제로 쓰므로 테스트 DB 설정(H2 또는 테스트용 MySQL)이 필요하다.
 * - 지금 단계에서는 "구동 확인" 목적이므로 최소만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationApiSmokeTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    NotificationRepository notificationRepository;

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void notifications_list_and_unreadCount_ok() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").exists());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void notifications_read_ok_whenExists() throws Exception {
        Notification saved = notificationRepository.save(Notification.builder()
                .targetType(NotificationTargetType.ALL)
                .targetUsername(null)
                .title("t")
                .body("b")
                .sender("admin")
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(post("/api/notifications/" + saved.getId() + "/read"))
                .andExpect(status().isOk());

        // idempotent: 두 번 호출해도 200
        mockMvc.perform(post("/api/notifications/" + saved.getId() + "/read"))
                .andExpect(status().isOk());
    }
}