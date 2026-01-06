package kr.java.sse_websocket.notifications;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결을 사용자 단위로 관리하는 레지스트리.
 *
 * 설계 의도:
 * - "현재 접속 중인 사용자"에게만 실시간 푸시
 * - 미접속 사용자는 DB 저장(다음 단계)로 보완
 *
 * 주의:
 * - 현재는 username(Principal.getName())을 키로 사용
 * - 추후 DB User 엔티티의 id 등을 키로 바꾸는 것이 일반적
 */

@Slf4j
@Component
public class SseEmitterRegistry {

    // username(normalized) -> SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // username을 저장/조회에 쓰기 전에 항상 같은 규칙으로 정규화한다.
    private String key(String username) {
        if (username == null) return null;
        return username.trim().toLowerCase();
    }

    /**
     * 특정 사용자 SSE 연결을 등록한다.
     */
    public SseEmitter add(String usernameRaw) {
        String username = key(usernameRaw);

        // timeout 없음(학습용). 운영에서는 적정 timeout 권장.
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(username, emitter);

        log.info("[SSE] add username={}, active={}", username, emitters.keySet());

        // 연결 종료/타임아웃/에러 발생 시 제거(메모리 누수 방지)
        emitter.onCompletion(() -> {
            emitters.remove(username);
            log.info("[SSE] completion username={}, active={}", username, emitters.keySet());
        });
        emitter.onTimeout(() -> {
            emitters.remove(username);
            log.info("[SSE] timeout username={}, active={}", username, emitters.keySet());
        });
        emitter.onError(e -> {
            emitters.remove(username);
            log.info("[SSE] error username={}, msg={}, active={}", username, e.getMessage(), emitters.keySet());
        });

        return emitter;
    }

    public void remove(String usernameRaw) {
        String username = key(usernameRaw);
        emitters.remove(username);
        log.info("[SSE] remove username={}, active={}", username, emitters.keySet());
    }

    /**
     * 특정 사용자에게 이벤트를 전송한다.
     * @return 전송 성공 여부(미접속/전송 실패면 false)
     */
    public boolean sendTo(String usernameRaw, String eventName, Object data) {
        String username = key(usernameRaw);

        SseEmitter emitter = emitters.get(username);
        if (emitter == null) {
            log.warn("[SSE] sendTo fail(no emitter) username={}, active={}", username, emitters.keySet());
            return false;
        }

        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            log.info("[SSE] sendTo ok username={}, event={}", username, eventName);
            return true;
        } catch (IOException e) {
            emitters.remove(username);
            log.warn("[SSE] sendTo fail(io) username={}, msg={}", username, e.getMessage());
            return false;
        }
    }

    public void sendToAll(String eventName, Object data) {
        emitters.forEach((username, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                log.info("[SSE] sendToAll ok username={}, event={}", username, eventName);
            } catch (IOException e) {
                emitters.remove(username);
                log.warn("[SSE] sendToAll fail(io) username={}, msg={}", username, e.getMessage());
            }
        });
    }

    // 디버그를 위해 현재 활성 username 목록을 노출(아래 DebugController에서 사용)
    public Map<String, SseEmitter> snapshot() {
        return Map.copyOf(emitters);
    }
}