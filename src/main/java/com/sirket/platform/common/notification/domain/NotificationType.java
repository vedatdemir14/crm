package com.sirket.platform.common.notification.domain;

/**
 * Kinds of notification the platform produces. A task yields at most one of each kind, which is
 * what the unique index on (user_id, type, related_entity_id) enforces.
 */
public enum NotificationType {
    /** An open task whose due date is approaching (FR-CRM-07). */
    TASK_DUE_SOON,
    /** An open task whose due date has passed (FR-CRM-07). */
    TASK_OVERDUE
}
