package kr.java.sse_websocket.notifications.domain;


import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * STEP 2
 * 알림 전송 DTO.
 *
 * 목적:
 * - WS 발송/응답, SSE 수신 모두 동일 구조로 처리
 *
 * 주의:
 * - 지금 단계에서는 DB 저장을 하지 않으므로 id는 임시(UUID 문자열) 사용
 * - 다음 단계에서 JPA Entity로 확장 시 id는 PK(Long)로 바꾸는 것을 권장
 */

/**
 * STEP 3
 * 실시간 전송(SSE/WS)용 메시지 DTO.
 *
 * - DB 저장 후 생성된 notificationId를 전달한다.
 * - delivered: 현재 실시간으로 바로 전달되었는지(접속 중인 SSE emitter 존재 여부)
 */
@Data
@Builder
public class NotificationMessage {

    /** 알림 식별자(임시) */
//    private String id;
    private Long id;

    /** 알림 제목 */
    private String title;

    /** 알림 본문 */
    private String body;

    /** 발송자(현재는 admin username 등) */
    private String sender;

    /** 대상: "ALL" 또는 특정 username */
    private String target;

    /** 생성 시각 */
    private Instant createdAt;

    // 지금 단계 디버그/운영에 매우 유용: "실시간 전달 성공 여부"
    private boolean delivered;
}