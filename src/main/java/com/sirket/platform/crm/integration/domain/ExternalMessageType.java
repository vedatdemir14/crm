package com.sirket.platform.crm.integration.domain;

import com.sirket.platform.crm.activity.domain.ActivityType;

public enum ExternalMessageType {

    EMAIL(ActivityType.EMAIL),
    MEETING(ActivityType.MEETING);

    private final ActivityType activityType;

    ExternalMessageType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public ActivityType toActivityType() {
        return activityType;
    }
}
