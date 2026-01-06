package kr.java.sse_websocket.notifications.repository;

import kr.java.sse_websocket.notifications.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 알림 조회/카운트용 Repository.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 로그인 사용자 관점에서 "보이는 알림" 페이징 조회.
     * - ALL 또는 USER(targetUsername=username)
     */
    @Query("""
        select n
        from Notification n
        where (
              n.targetType = kr.java.sse_websocket.notifications.domain.NotificationTargetType.ALL
           or (n.targetType = kr.java.sse_websocket.notifications.domain.NotificationTargetType.USER and n.targetUsername = :username)
        )
        order by n.createdAt desc
    """)
    Page<Notification> findVisibleForUser(@Param("username") String username, Pageable pageable);

    /**
     * 안읽음 카운트:
     * - 보이는 알림 중에서 notification_reads에 (notificationId, username)가 없는 것만 count
     *
     * 주의:
     * - 실습 단계에서는 서브쿼리 방식이 가장 단순/명료하다.
     */
    @org.springframework.data.jpa.repository.Query("""
        select count(n)
        from Notification n
        where (
              n.targetType = kr.java.sse_websocket.notifications.domain.NotificationTargetType.ALL
           or (n.targetType = kr.java.sse_websocket.notifications.domain.NotificationTargetType.USER and n.targetUsername = :username)
        )
        and not exists (
            select 1
            from NotificationRead r
            where r.notificationId = n.id
              and r.username = :username
        )
    """)
    long countUnreadForUser(@Param("username") String username);
}