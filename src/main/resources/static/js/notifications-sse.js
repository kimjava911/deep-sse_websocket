/**
 * notifications-sse.js
 *
 * 역할:
 * - 알림 인박스 화면의 모든 클라이언트 로직 담당
 *
 * 기능:
 * 1) 초기 로딩
 *    - GET /api/notifications          : 알림 목록
 *    - GET /api/notifications/unread-count : 안읽음 카운트
 *
 * 2) SSE 수신
 *    - event: connected     : 연결 확인
 *    - event: notification  : 신규 알림 실시간 수신
 *    - event: unreadCount   : 안읽음 배지 실시간 갱신
 *
 * 3) 읽음 처리
 *    - POST /api/notifications/{id}/read
 *    - 서버 성공(200) 시에만 UI 반영
 *
 * 전제:
 * - SecurityConfig에서 /api/** CSRF 제외됨
 * - username 정규화는 서버에서 처리
 */

(function () {
    const unreadEl = document.getElementById("unreadCount");
    const listEl = document.getElementById("list");
    const logEl = document.getElementById("log");

    /* =========================
     * 공통 유틸
     * ========================= */

    const log = (msg) => {
        logEl.textContent += msg + "\n";
    };

    const esc = (s) => {
        if (s === null || s === undefined) return "";
        return String(s)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    };

    const setUnreadCount = (count) => {
        unreadEl.textContent = String(count);
    };

    /* =========================
     * 서버 API 호출
     * ========================= */

    const fetchUnreadCount = async () => {
        const res = await fetch("/api/notifications/unread-count");
        if (!res.ok) {
            log("unread-count fetch failed: " + res.status);
            return;
        }
        const json = await res.json();
        setUnreadCount(json.count);
    };

    const fetchInitialNotifications = async () => {
        const res = await fetch("/api/notifications?size=50");
        if (!res.ok) {
            log("notifications fetch failed: " + res.status);
            return [];
        }
        return await res.json();
    };

    /* =========================
     * DOM 렌더링
     * ========================= */

    const renderNotificationItem = (n, { prepend = false } = {}) => {
        const li = document.createElement("li");
        li.dataset.id = n.id;
        li.dataset.read = n.read ? "true" : "false";

        const readMark = n.read ? "[READ]" : "[UNREAD]";
        const targetLabel = n.target === "ALL" ? "ALL" : n.target;

        li.innerHTML = `
      <div>
        <strong class="titleText">${esc(readMark)} ${esc(n.title)}</strong>
        <span> (from: ${esc(n.sender)} / target: ${esc(targetLabel)})</span>
      </div>
      <div>${esc(n.body)}</div>
      <div>
        <small>${esc(n.createdAt)}</small>
      </div>
      <div>
        <button class="readBtn" ${n.read ? "disabled" : ""}>읽음</button>
      </div>
    `;

        const readBtn = li.querySelector(".readBtn");
        const titleEl = li.querySelector(".titleText");

        readBtn.addEventListener("click", async () => {
            // 이미 읽음 처리된 경우 방어
            if (li.dataset.read === "true") {
                return;
            }

            const res = await fetch(`/api/notifications/${n.id}/read`, {
                method: "POST",
            });

            if (!res.ok) {
                log(`read failed: id=${n.id}, status=${res.status}`);
                return;
            }

            // 서버 반영 성공 후에만 UI 갱신
            li.dataset.read = "true";
            titleEl.textContent = `[READ] ${n.title}`;
            readBtn.disabled = true;

            // unreadCount는 서버가 SSE로 push하지만,
            // 혹시 누락되었을 경우를 대비해 안전하게 재조회 가능
            // await fetchUnreadCount();
        });

        if (prepend) {
            listEl.prepend(li);
        } else {
            listEl.appendChild(li);
        }
    };

    /* =========================
     * 초기 로딩
     * ========================= */

    const loadInitial = async () => {
        listEl.innerHTML = "";

        const notifications = await fetchInitialNotifications();
        notifications.forEach((n) => renderNotificationItem(n));

        await fetchUnreadCount();
    };

    /* =========================
     * SSE 연결
     * ========================= */

    const connectSse = () => {
        const es = new EventSource("/sse/notifications");

        es.addEventListener("connected", (e) => {
            log("SSE connected: " + e.data);
        });

        // 신규 알림 수신
        es.addEventListener("notification", (e) => {
            try {
                const msg = JSON.parse(e.data);

                renderNotificationItem(
                    {
                        id: msg.id,
                        title: msg.title,
                        body: msg.body,
                        sender: msg.sender,
                        target: msg.target,
                        createdAt: msg.createdAt,
                        read: false,
                    },
                    { prepend: true }
                );
            } catch (err) {
                log("notification parse error: " + err);
            }
        });

        // 안읽음 카운트 갱신
        es.addEventListener("unreadCount", (e) => {
            setUnreadCount(e.data);
        });

        es.onerror = () => {
            log("SSE error (will retry automatically)");
        };
    };

    /* =========================
     * 실행
     * ========================= */

    loadInitial().catch((e) => log("initial load error: " + e));
    connectSse();
})();
