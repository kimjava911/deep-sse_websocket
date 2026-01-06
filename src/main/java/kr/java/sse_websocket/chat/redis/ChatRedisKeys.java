package kr.java.sse_websocket.chat.redis;


/**
 * Redis 키/채널 규칙.
 * - channel: chat:room:{roomId} (Pub/Sub)
 * - recent cache(list): chat:recent:{roomId}
 */
public final class ChatRedisKeys {

    private ChatRedisKeys() {}

    public static String channelRoom(Long roomId) {
        return "chat:room:" + roomId;
    }

    public static String recentListKey(Long roomId) {
        return "chat:recent:" + roomId;
    }

    public static String channelPatternAllRooms() {
        return "chat:room:*";
    }
}