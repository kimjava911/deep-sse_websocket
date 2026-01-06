package kr.java.sse_websocket.notifications.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 목록 화면에 필요한 형태로 내려주는 View DTO.
 * - read(boolean) 포함
 */
@Data
@Builder
public class NotificationView {
    private Long id;
    private String title;
    private String body;
    private String sender;
    private String target;      // "ALL" or username
    private Instant createdAt;
    private boolean read;
}