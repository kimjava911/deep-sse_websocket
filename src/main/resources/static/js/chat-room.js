/**
 * chat-room.js
 *
 * 흐름:
 * 1) POST /api/chat/room 으로 roomId 확보
 * 2) GET /api/chat/rooms/{roomId}/messages 로 초기 메시지 로딩
 * 3) WebSocket(STOMP) 연결 후 /topic/chat/rooms/{roomId} 구독
 * 4) 전송은 /app/chat/rooms/{roomId}/send 로 SEND
 */

(function () {
    const otherEl = document.getElementById("otherUsername");
    const btnCreate = document.getElementById("btnCreateRoom");
    const roomIdEl = document.getElementById("roomId");

    const messagesEl = document.getElementById("messages");
    const inputEl = document.getElementById("messageInput");
    const btnSend = document.getElementById("btnSend");

    const logEl = document.getElementById("log");
    const log = (s) => (logEl.textContent += s + "\n");

    let roomId = null;
    let stompClient = null;

    const esc = (s) => {
        if (s === null || s === undefined) return "";
        return String(s)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    };

    const appendMessage = (m) => {
        const li = document.createElement("li");
        li.innerHTML = `<strong>${esc(m.senderUsername)}</strong>: ${esc(m.content)} <small>${esc(m.createdAt)}</small>`;
        messagesEl.appendChild(li);
    };

    const clearMessages = () => {
        messagesEl.innerHTML = "";
    };

    const createOrGetRoom = async (otherUsername) => {
        const res = await fetch("/api/chat/room", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ otherUsername }),
        });

        if (!res.ok) {
            log("room create failed: " + res.status);
            return null;
        }
        return await res.json();
    };

    const loadRecentMessages = async (roomId) => {
        const res = await fetch(`/api/chat/rooms/${roomId}/messages?size=50`);
        if (!res.ok) {
            log("recent messages fetch failed: " + res.status);
            return [];
        }
        return await res.json();
    };

    const connectStomp = (roomId) => {
        if (stompClient && stompClient.connected) {
            stompClient.disconnect(() => {});
        }

        const sock = new SockJS("/ws");
        stompClient = Stomp.over(sock);

        // 콘솔 노이즈 제거
        stompClient.debug = null;

        stompClient.connect({}, () => {
            log("stomp connected");

            stompClient.subscribe(`/topic/chat/rooms/${roomId}`, (frame) => {
                try {
                    const payload = JSON.parse(frame.body);
                    appendMessage({
                        senderUsername: payload.senderUsername,
                        content: payload.content,
                        createdAt: payload.createdAt,
                    });
                } catch (e) {
                    log("topic message parse error: " + e);
                }
            });

            log(`subscribed: /topic/chat/rooms/${roomId}`);
        }, (err) => {
            log("stomp connect error: " + err);
        });
    };

    const sendMessage = () => {
        if (!roomId) {
            log("roomId is null. create room first.");
            return;
        }
        if (!stompClient || !stompClient.connected) {
            log("stomp is not connected.");
            return;
        }

        const content = inputEl.value.trim();
        if (!content) return;

        stompClient.send(
            `/app/chat/rooms/${roomId}/send`,
            {},
            JSON.stringify({ content })
        );

        inputEl.value = "";
    };

    btnCreate.addEventListener("click", async () => {
        const other = otherEl.value.trim();
        if (!other) {
            log("otherUsername is empty");
            return;
        }

        const room = await createOrGetRoom(other);
        if (!room) return;

        roomId = room.roomId;
        roomIdEl.textContent = String(roomId);
        log("roomId = " + roomId);

        clearMessages();
        const recent = await loadRecentMessages(roomId);
        recent.forEach((m) => appendMessage(m));

        connectStomp(roomId);
    });

    btnSend.addEventListener("click", sendMessage);
    inputEl.addEventListener("keydown", (e) => {
        if (e.key === "Enter") sendMessage();
    });
})();
