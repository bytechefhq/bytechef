/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.scheduler.aws.AwsTriggerScheduler;
import com.bytechef.ee.platform.scheduler.aws.listener.DynamicWebhookTriggerRefreshListener;
import com.bytechef.ee.platform.scheduler.aws.listener.PollingTriggerListener;
import com.bytechef.ee.platform.scheduler.aws.listener.ScheduleTriggerListener;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.trigger.scheduler.provider", havingValue = "aws")
@ConditionalOnEEVersion
public class AwsTriggerSchedulerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AwsTriggerSchedulerConfiguration.class);

    private final ApplicationProperties applicationProperties;

    AwsTriggerSchedulerConfiguration(ApplicationProperties applicationProperties) {
        if (log.isInfoEnabled()) {
            log.info("Trigger scheduler provider type enabled: aws");
        }

        this.applicationProperties = applicationProperties;
    }

    @Bean
    AwsTriggerScheduler awsTriggerScheduler(
        AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider) {

        ApplicationProperties.Cloud.Aws aws = applicationProperties.getCloud()
            .getAws();
        ApplicationProperties.Coordinator.Trigger.Polling polling = applicationProperties
            .getCoordinator()
            .getTrigger()
            .getPolling();

        SchedulerClient schedulerClient = SchedulerClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(awsRegionProvider.getRegion())
            .build();

        return new AwsTriggerScheduler(aws, polling, schedulerClient);
    }

    @Bean
    DynamicWebhookTriggerRefreshListener dynamicWebhookListener(
        AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerStateService triggerStateService, WorkflowService workflowService) {

        SchedulerClient schedulerClient = SchedulerClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(awsRegionProvider.getRegion())
            .build();

        return new DynamicWebhookTriggerRefreshListener(
            jobPrincipalAccessorRegistry, schedulerClient, triggerDefinitionFacade, triggerStateService,
            workflowService);
    }

    @Bean
    PollingTriggerListener pollingListener(ApplicationEventPublisher eventPublisher) {
        return new PollingTriggerListener(eventPublisher);
    }

    @Bean
    ScheduleTriggerListener scheduleListener(ApplicationEventPublisher eventPublisher) {
        return new ScheduleTriggerListener(eventPublisher);
    }
}
