package com.sirket.platform.hr.onboarding.domain;

/**
 * FR-HR-06 and FR-HR-07. Joining and leaving are the same checklist mechanism pointed in opposite
 * directions, so they share one table and differ by this type.
 */
public enum OnboardingTaskType {
    ONBOARDING,
    OFFBOARDING
}
