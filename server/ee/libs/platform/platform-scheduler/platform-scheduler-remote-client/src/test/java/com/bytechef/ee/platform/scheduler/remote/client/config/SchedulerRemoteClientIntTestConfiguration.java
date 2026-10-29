/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.client.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.QuartzConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.QuartzTriggerScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import org.quartz.Scheduler;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@SpringBootConfiguration
public class SchedulerRemoteClientIntTestConfiguration {

    public static final int POLLING_CHECK_PERIOD_MINUTES = 7;

    @Bean
    ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Coordinator coordinator = applicationProperties.getCoordinator();

        ApplicationProperties.Coordinator.Trigger trigger = coordinator.getTrigger();

        ApplicationProperties.Coordinator.Trigger.Polling polling = trigger.getPolling();

        polling.setCheckPeriod(POLLING_CHECK_PERIOD_MINUTES);

        return applicationProperties;
    }

    @Bean
    ConnectionRefreshScheduler connectionRefreshScheduler(Scheduler scheduler) {
        return new QuartzConnectionRefreshScheduler(scheduler);
    }

    @Bean
    TriggerScheduler triggerScheduler(ApplicationProperties applicationProperties, Scheduler scheduler) {
        ApplicationProperties.Coordinator coordinator = applicationProperties.getCoordinator();

        ApplicationProperties.Coordinator.Trigger trigger = coordinator.getTrigger();

        return new QuartzTriggerScheduler(trigger.getPolling(), scheduler);
    }
}
