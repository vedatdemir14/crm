package com.sirket.platform.crm.activity.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.access.CrmAccessPolicy;
import com.sirket.platform.crm.activity.domain.Activity;
import com.sirket.platform.crm.activity.dto.ActivityDtos;
import com.sirket.platform.crm.activity.repository.ActivityRepository;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.service.ContactService;
import com.sirket.platform.crm.opportunity.domain.Opportunity;
import com.sirket.platform.crm.opportunity.service.OpportunityService;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final CrmAccessPolicy accessPolicy;
    private final Duration editWindow;

    public ActivityService(ActivityRepository activityRepository, ContactService contactService,
            OpportunityService opportunityService, CrmAccessPolicy accessPolicy,
            @Value("${crm.activity.edit-window:PT24H}") Duration editWindow) {
        this.activityRepository = activityRepository;
        this.contactService = contactService;
        this.opportunityService = opportunityService;
        this.accessPolicy = accessPolicy;
        this.editWindow = editWindow;
    }

    /**
     * Reading a timeline requires access to the record it belongs to, so visibility is decided by
     * the parent contact or opportunity rather than by the activity itself.
     */
    @Transactional(readOnly = true)
    public Page<ActivityDtos.ActivityResponse> timelineOfContact(UUID contactId, Pageable pageable) {
        contactService.requireVisible(contactId);
        return activityRepository.timelineOfContact(contactId, pageable)
                .map(ActivityDtos.ActivityResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDtos.ActivityResponse> timelineOfOpportunity(UUID opportunityId, Pageable pageable) {
        opportunityService.requireVisible(opportunityId);
        return activityRepository.timelineOfOpportunity(opportunityId, pageable)
                .map(ActivityDtos.ActivityResponse::from);
    }

    @Transactional
    public ActivityDtos.ActivityResponse create(ActivityDtos.ActivityRequest request) {
        Contact contact = request.contactId() == null ? null : contactService.requireVisible(request.contactId());
        Opportunity opportunity = request.opportunityId() == null
                ? null
                : opportunityService.requireVisible(request.opportunityId());

        Activity activity = new Activity(
                request.type(),
                request.subject(),
                request.description(),
                contact,
                opportunity,
                request.occurredAt(),
                accessPolicy.currentUserId());
        return ActivityDtos.ActivityResponse.from(activityRepository.save(activity));
    }

    @Transactional
    public ActivityDtos.ActivityResponse update(UUID id, ActivityDtos.UpdateActivityRequest request) {
        Activity activity = requireExisting(id);
        activity.requireEditableBy(accessPolicy.currentUserId(), editWindow);
        activity.update(request.type(), request.subject(), request.description(), request.occurredAt());
        return ActivityDtos.ActivityResponse.from(activityRepository.save(activity));
    }

    @Transactional
    public void delete(UUID id) {
        Activity activity = requireExisting(id);
        activity.requireEditableBy(accessPolicy.currentUserId(), editWindow);
        activityRepository.delete(activity);
    }

    private Activity requireExisting(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Aktivite bulunamadı: " + id));
    }
}
