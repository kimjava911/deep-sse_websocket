package kr.java.sse_websocket;


import kr.java.sse_websocket.controller.EchoWsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WebSocketContextSmokeTest {

    @Autowired
    EchoWsController echoWsController;

    @Test
    void contextLoads_websocketControllerExists() {
        assertThat(echoWsController).isNotNull();
    }
}