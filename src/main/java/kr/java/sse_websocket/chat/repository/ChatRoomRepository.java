package kr.java.sse_websocket.chat.repository;

import kr.java.sse_websocket.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByUserAAndUserB(String userA, String userB);
}
