/**
 * WebSocket Echo 구동 체크 스크립트.
 *
 * 사전 조건:
 * - 서버: /ws STOMP endpoint 존재(WebSocketConfig)
 * - 서버: /app/echo @MessageMapping 존재(EchoWsController)
 * - 서버: /topic/echo 로 응답 브로드캐스트
 */

(function () {
    const logEl = document.getElementById("log");
    const msgEl = document.getElementById("msg");
    const sendBtn = document.getElementById("sendBtn");

    // 화면 로그 출력 유틸
    const log = (s) => {
        logEl.textContent += s + "\n";
    };

    // SockJS로 /ws 엔드포인트 연결
    const socket = new SockJS("/ws");
    const stomp = Stomp.over(socket);

    // 디버그 로그는 노이즈가 많아 비활성화
    stomp.debug = null;

    stomp.connect(
        {},
        () => {
            log("connected");

            // 서버 응답 토픽 구독
            stomp.subscribe("/topic/echo", (frame) => {
                log("recv: " + frame.body);
            });
        },
        (err) => {
            log("connect error: " + err);
        }
    );

    // 입력 메시지를 서버로 전송(/app/echo)
    sendBtn.addEventListener("click", () => {
        const msg = msgEl.value;
        stomp.send("/app/echo", {}, JSON.stringify({ message: msg }));
        log("sent: " + msg);
    });
})();
