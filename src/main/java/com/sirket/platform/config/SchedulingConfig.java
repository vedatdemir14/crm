package com.sirket.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduling is on by default and switched off under the test profile, so integration tests drive
 * the reminder job explicitly instead of racing a background trigger.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "crm.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
