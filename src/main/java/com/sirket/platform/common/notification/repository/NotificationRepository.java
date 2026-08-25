package com.sirket.platform.common.notification.repository;

import com.sirket.platform.common.notification.domain.Notification;
import com.sirket.platform.common.notification.domain.NotificationType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query(value = """
            SELECT n FROM Notification n
            WHERE n.userId = :userId
              AND (:unreadOnly = FALSE OR n.readAt IS NULL)
            ORDER BY n.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(n) FROM Notification n
            WHERE n.userId = :userId
              AND (:unreadOnly = FALSE OR n.readAt IS NULL)
            """)
    Page<Notification> findForUser(@Param("userId") UUID userId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    boolean existsByUserIdAndTypeAndRelatedEntityId(UUID userId, NotificationType type, UUID relatedEntityId);
}
