/**
 * 관리자 발송 콘솔(WebSocket) 스크립트.
 *
 * 사전 조건:
 * - 서버: /ws STOMP endpoint 존재(WebSocketConfig)
 * - 서버: /app/admin/notifications/broadcast, /app/admin/notifications/target 처리(NotificationWsController)
 * - 서버: /topic/admin/notifications/result 응답 송신
 *
 * 주의:
 * - 서버 측에서 @PreAuthorize("hasRole('ADMIN')")로 ADMIN 권한을 요구
 * - 따라서 user1로 접속하면 send는 가능해 보일 수 있어도, 서버에서 거부될 수 있음
 */

(function () {
    const logEl = document.getElementById("log");

    const bTitle = document.getElementById("bTitle");
    const bBody = document.getElementById("bBody");
    const broadcastBtn = document.getElementById("broadcastBtn");

    const tUser = document.getElementById("tUser");
    const tTitle = document.getElementById("tTitle");
    const tBody = document.getElementById("tBody");
    const targetBtn = document.getElementById("targetBtn");

    const log = (s) => {
        logEl.textContent += s + "\n";
    };

    // SockJS로 /ws 연결
    const socket = new SockJS("/ws");
    const stomp = Stomp.over(socket);
    stomp.debug = null;

    stomp.connect(
        {},
        () => {
            log("connected");

            // 서버 처리 결과(브로드캐스트/타겟 모두 같은 결과 토픽으로 받음)
            stomp.subscribe("/topic/admin/notifications/result", (frame) => {
                log("result: " + frame.body);
            });
        },
        (err) => {
            log("connect error: " + err);
        }
    );

    // 전체 발송
    broadcastBtn.addEventListener("click", () => {
        stomp.send(
            "/app/admin/notifications/broadcast",
            {},
            JSON.stringify({
                title: bTitle.value,
                body: bBody.value,
            })
        );
        log("sent broadcast");
    });

    // 유저 지정 발송
    targetBtn.addEventListener("click", () => {
        stomp.send(
            "/app/admin/notifications/target",
            {},
            JSON.stringify({
                targetUsername: tUser.value,
                title: tTitle.value,
                body: tBody.value,
            })
        );
        log("sent target");
    });
})();