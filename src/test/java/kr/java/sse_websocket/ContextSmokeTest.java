package kr.java.sse_websocket;


import kr.java.sse_websocket.notifications.NotificationSseController;
import kr.java.sse_websocket.notifications.NotificationWsController;
import kr.java.sse_websocket.ws.EchoWsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "부팅이 깨지지 않는다"를 보장하는 최소 스모크 테스트.
 * - 설정/빈 충돌, 컴포넌트 스캔 누락을 빠르게 탐지한다.
 */
@SpringBootTest
class ContextSmokeTest {

    @Autowired
    EchoWsController echoWsController;

    @Autowired
    NotificationSseController notificationSseController;

    @Autowired
    NotificationWsController notificationWsController;

    @Test
    void beans_exist() {
        assertThat(echoWsController).isNotNull();
        assertThat(notificationSseController).isNotNull();
        assertThat(notificationWsController).isNotNull();
    }
}