/**
 * SSE 알림 수신 스크립트.
 *
 * 사전 조건:
 * - 서버: GET /sse/notifications 제공(NotificationSseController)
 * - 서버: emitter 등록 및 connected 이벤트 송신
 * - 서버: notification 이벤트 송신(NotificationService -> SseEmitterRegistry)
 */

(function () {
    const logEl = document.getElementById("log");

    const log = (s) => {
        logEl.textContent += s + "\n";
    };

    // SSE 연결
    const es = new EventSource("/sse/notifications");

    // 연결 확인 이벤트(서버에서 1회 전송)
    es.addEventListener("connected", (e) => {
        log("connected: " + e.data);
    });

    // 알림 이벤트
    es.addEventListener("notification", (e) => {
        // 현재는 콘솔처럼 찍기만 한다. 이후 UI 리스트로 확장 가능
        log("notification: " + e.data);
    });

    // SSE는 기본적으로 자동 재연결이 동작한다.
    es.onerror = () => {
        log("sse error (auto-reconnect will try)");
    };
})();
