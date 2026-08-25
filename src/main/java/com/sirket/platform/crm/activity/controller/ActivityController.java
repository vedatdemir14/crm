package com.sirket.platform.crm.activity.controller;

import com.sirket.platform.crm.activity.dto.ActivityDtos;
import com.sirket.platform.crm.activity.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The timeline endpoints live here rather than on the contact and opportunity controllers so those
 * packages keep no dependency on the activity package; the paths still follow the API design
 * document.
 */
@RestController
@RequestMapping("/api/crm")
@Tag(name = "CRM — Aktiviteler")
@PreAuthorize("hasAnyRole('SALES_REP', 'SALES_MANAGER', 'ADMIN')")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/activities")
    @Operation(summary = "Kişi/fırsata bağlı iletişim kaydı ekler (arama, e-posta, toplantı, not)")
    public ResponseEntity<ActivityDtos.ActivityResponse> create(
            @Valid @RequestBody ActivityDtos.ActivityRequest request) {
        ActivityDtos.ActivityResponse created = activityService.create(request);
        return ResponseEntity.created(URI.create("/api/crm/activities/" + created.id())).body(created);
    }

    @GetMapping("/contacts/{contactId}/activities")
    @Operation(summary = "Kişinin iletişim geçmişi (kronolojik, en yeni önce)")
    public Page<ActivityDtos.ActivityResponse> contactTimeline(
            @PathVariable UUID contactId, @PageableDefault(size = 20) Pageable pageable) {
        return activityService.timelineOfContact(contactId, pageable);
    }

    @GetMapping("/opportunities/{opportunityId}/activities")
    @Operation(summary = "Fırsatın iletişim geçmişi (kronolojik, en yeni önce)")
    public Page<ActivityDtos.ActivityResponse> opportunityTimeline(
            @PathVariable UUID opportunityId, @PageableDefault(size = 20) Pageable pageable) {
        return activityService.timelineOfOpportunity(opportunityId, pageable);
    }

    @PutMapping("/activities/{id}")
    @Operation(summary = "Aktiviteyi düzeltir (yalnızca oluşturan kullanıcı, düzenleme süresi içinde)")
    public ActivityDtos.ActivityResponse update(
            @PathVariable UUID id, @Valid @RequestBody ActivityDtos.UpdateActivityRequest request) {
        return activityService.update(id, request);
    }

    @DeleteMapping("/activities/{id}")
    @Operation(summary = "Aktiviteyi siler (yalnızca oluşturan kullanıcı, düzenleme süresi içinde)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
