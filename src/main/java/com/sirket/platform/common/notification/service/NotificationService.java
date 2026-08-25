package com.sirket.platform.common.notification.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.common.notification.domain.Notification;
import com.sirket.platform.common.notification.domain.NotificationType;
import com.sirket.platform.common.notification.dto.NotificationDtos;
import com.sirket.platform.common.notification.repository.NotificationRepository;
import com.sirket.platform.common.security.CurrentUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUser currentUser;

    public NotificationService(NotificationRepository notificationRepository, CurrentUser currentUser) {
        this.notificationRepository = notificationRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDtos.NotificationResponse> listForCurrentUser(boolean unreadOnly, Pageable pageable) {
        return notificationRepository.findForUser(currentUser.id(), unreadOnly, pageable)
                .map(NotificationDtos.NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public NotificationDtos.UnreadCountResponse unreadCount() {
        return new NotificationDtos.UnreadCountResponse(
                notificationRepository.countByUserIdAndReadAtIsNull(currentUser.id()));
    }

    @Transactional
    public NotificationDtos.NotificationResponse markRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Bildirim bulunamadı: " + id));
        if (!notification.getUserId().equals(currentUser.id())) {
            // Reported as "not found" so the API does not confirm that someone else's notification exists.
            throw new ApiExceptions.NotFound("Bildirim bulunamadı: " + id);
        }
        notification.markRead();
        return NotificationDtos.NotificationResponse.from(notificationRepository.save(notification));
    }

    /**
     * Creates a notification unless the same user already has one of this type for this entity.
     * The reminder job runs on a schedule, so without this check every pass would add another copy;
     * a unique index backs the same rule at the database level.
     *
     * @return the created notification, or empty when one already existed
     */
    @Transactional
    public Optional<Notification> createIfAbsent(UUID userId, NotificationType type, String title, String message,
            String relatedEntityType, UUID relatedEntityId) {
        if (notificationRepository.existsByUserIdAndTypeAndRelatedEntityId(userId, type, relatedEntityId)) {
            return Optional.empty();
        }
        return Optional.of(notificationRepository.save(
                new Notification(userId, type, title, message, relatedEntityType, relatedEntityId)));
    }
}
