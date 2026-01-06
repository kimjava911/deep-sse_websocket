package kr.java.sse_websocket;


import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SecurityConfig가 의도한 대로 동작하는지 확인하는 최소 테스트.
 *
 * 주의:
 * - @WithMockUser는 실제 InMemoryUserDetailsService를 타지 않고,
 *   "권한 규칙"만 빠르게 검증하는 목적이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecuritySmokeTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void admin_requiresLogin_redirectToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", Matchers.containsString("/login")));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void admin_forbidden_forUserRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_ok_forAdminRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"));
    }

    @Test
    void staticJs_isPublic() throws Exception {
        // SecurityConfig에서 /js/** permitAll 여부 확인(404면 파일 경로가 잘못된 것)
        mockMvc.perform(get("/js/chat-ws.js"))
                .andExpect(status().isOk());
    }
}