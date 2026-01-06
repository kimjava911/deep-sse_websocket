package kr.java.sse_websocket.chat.repository;


import kr.java.sse_websocket.chat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 최근 N개 가져오기(내림차순)
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    // room에서 특정 id 이후 메시지(선택 확장용)
    List<ChatMessage> findByRoomIdAndIdGreaterThanOrderByIdAsc(Long roomId, Long afterId);
}
