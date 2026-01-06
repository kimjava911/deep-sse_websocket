package kr.java.sse_websocket.notifications.repository;

import kr.java.sse_websocket.notifications.domain.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;


/**
 * 읽음 상태 Repository.
 */
public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    /**
     * 읽음 처리 idempotent를 위해 존재 여부 확인.
     */
    boolean existsByNotificationIdAndUsername(Long notificationId, String username);

    /**
     * 현재 페이지에 포함된 알림 중, 사용자가 읽은 알림 id 목록을 가져온다.
     * - 목록 화면에서 read 여부 표시를 위해 사용
     */
    @Query("""
        select r.notificationId
        from NotificationRead r
        where r.username = :username
          and r.notificationId in :notificationIds
    """)
    Set<Long> findReadNotificationIds(@Param("username") String username,
                                      @Param("notificationIds") Collection<Long> notificationIds);
}