package com.sirket.platform.crm.access;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.common.security.CurrentUser;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Record-level authorisation for CRM data (Mimari Tasarım Dokümanı §7): a sales rep only reaches
 * their own records, while a sales manager or admin sees the whole team's.
 */
@Component
public class CrmAccessPolicy {

    private static final String ROLE_SALES_MANAGER = "ROLE_SALES_MANAGER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final CurrentUser currentUser;

    public CrmAccessPolicy(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public UUID currentUserId() {
        return currentUser.id();
    }

    public boolean canSeeAllRecords() {
        return currentUser.hasAnyRole(ROLE_SALES_MANAGER, ROLE_ADMIN);
    }

    /**
     * The owner id a query must be restricted to, or null when the caller may see everything.
     */
    public UUID ownerRestriction() {
        return canSeeAllRecords() ? null : currentUser.id();
    }

    public void requireVisible(UUID ownerUserId) {
        if (!canSeeAllRecords() && !currentUser.id().equals(ownerUserId)) {
            // Reported as "not found" so the API does not confirm that someone else's record exists.
            throw new ApiExceptions.NotFound("Kayıt bulunamadı");
        }
    }

    public void requireModifiable(UUID ownerUserId) {
        requireVisible(ownerUserId);
    }
}
