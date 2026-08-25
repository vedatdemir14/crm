package com.sirket.platform.common.notification.controller;

import com.sirket.platform.common.notification.dto.NotificationDtos;
import com.sirket.platform.common.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notifications always belong to the caller; there is no endpoint for reading another user's.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Bildirimler")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Giriş yapmış kullanıcının bildirimleri (en yeni önce)")
    public Page<NotificationDtos.NotificationResponse> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.listForCurrentUser(unreadOnly, pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Okunmamış bildirim sayısı")
    public NotificationDtos.UnreadCountResponse unreadCount() {
        return notificationService.unreadCount();
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Bildirimi okundu olarak işaretler")
    public NotificationDtos.NotificationResponse markRead(@PathVariable UUID id) {
        return notificationService.markRead(id);
    }
}
