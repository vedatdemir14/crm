package com.sirket.platform.common.notification.dto;

import com.sirket.platform.common.notification.domain.Notification;
import com.sirket.platform.common.notification.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            UUID id,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            UUID relatedEntityId,
            boolean read,
            Instant readAt,
            Instant createdAt) {

        public static NotificationResponse from(Notification notification) {
            return new NotificationResponse(
                    notification.getId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getRelatedEntityType(),
                    notification.getRelatedEntityId(),
                    notification.isRead(),
                    notification.getReadAt(),
                    notification.getCreatedAt());
        }
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
