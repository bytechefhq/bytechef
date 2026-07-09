/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.config;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsBillingSchedulingConstants.STRIPE_USAGE_REPORTING_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.CONNECTION_REFRESH_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.POLLING_TRIGGER_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULE_TRIGGER_LISTENER_ID;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.scheduler.aws.AwsConnectionRefreshScheduler;
import com.bytechef.ee.platform.scheduler.aws.AwsStripeUsageReportScheduler;
import com.bytechef.ee.platform.scheduler.aws.AwsTriggerScheduler;
import com.bytechef.ee.platform.scheduler.aws.listener.ConnectionRefreshListener;
import com.bytechef.ee.platform.scheduler.aws.listener.DynamicWebhookTriggerRefreshListener;
import com.bytechef.ee.platform.scheduler.aws.listener.PollingTriggerListener;
import com.bytechef.ee.platform.scheduler.aws.listener.ScheduleTriggerListener;
import com.bytechef.ee.platform.scheduler.aws.listener.StripeUsageReportListener;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 * @author Nikolina Spehar
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "scheduler.provider", havingValue = "aws")
@ConditionalOnEEVersion
public class AwsSchedulerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AwsSchedulerConfiguration.class);

    private static final List<String> SCHEDULER_SQS_LISTENER_IDS = List.of(
        POLLING_TRIGGER_LISTENER_ID, SCHEDULE_TRIGGER_LISTENER_ID, DYNAMIC_WEBHOOK_TRIGGER_REFRESH_LISTENER_ID,
        CONNECTION_REFRESH_LISTENER_ID, STRIPE_USAGE_REPORTING_LISTENER_ID);

    private final ApplicationProperties applicationProperties;
    private final MessageListenerContainerRegistry messageListenerContainerRegistry;

    @SuppressFBWarnings("EI")
    AwsSchedulerConfiguration(
        ApplicationProperties applicationProperties,
        MessageListenerContainerRegistry messageListenerContainerRegistry) {

        log.info("Scheduler provider type enabled: aws");

        this.applicationProperties = applicationProperties;
        this.messageListenerContainerRegistry = messageListenerContainerRegistry;
    }

    @Bean
    AwsConnectionRefreshScheduler awsConnectionRefreshScheduler(SchedulerClient schedulerClient) {

        ApplicationProperties.Cloud.Aws aws = applicationProperties.getCloud()
            .getAws();

        return new AwsConnectionRefreshScheduler(aws, schedulerClient);
    }

    @Bean
    AwsStripeUsageReportScheduler awsStripeUsageReportScheduler(SchedulerClient schedulerClient) {
        return new AwsStripeUsageReportScheduler(applicationProperties, schedulerClient);
    }

    @Bean
    AwsTriggerScheduler awsTriggerScheduler(SchedulerClient schedulerClient) {

        ApplicationProperties.Cloud.Aws aws = applicationProperties.getCloud()
            .getAws();
        ApplicationProperties.Coordinator.Trigger.Polling polling = applicationProperties
            .getCoordinator()
            .getTrigger()
            .getPolling();

        return new AwsTriggerScheduler(aws, polling, schedulerClient);
    }

    @Bean
    ConnectionRefreshListener connectionRefreshListener(ConnectionFacade connectionFacade) {
        return new ConnectionRefreshListener(connectionFacade);
    }

    @Bean
    SchedulerClient schedulerClient(
        AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider) {

        return SchedulerClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(awsRegionProvider.getRegion())
            .build();
    }

    @Bean
    DynamicWebhookTriggerRefreshListener dynamicWebhookListener(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, SchedulerClient schedulerClient,
        TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerStateService triggerStateService, WorkflowService workflowService) {

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

    @Bean
    StripeUsageReportListener stripeUsageReportListener(BillingUsageService billingUsageService) {
        return new StripeUsageReportListener(billingUsageService);
    }

    @Bean(name = SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY, autowireCandidate = false)
    SqsMessageListenerContainerFactory<Object> schedulerSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
        SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();

        factory.setSqsAsyncClient(sqsAsyncClient);
        factory.configure(options -> options.autoStartup(false));

        return factory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startSchedulerListeners() {
        for (String listenerId : SCHEDULER_SQS_LISTENER_IDS) {
            MessageListenerContainer<?> container = messageListenerContainerRegistry.getContainerById(listenerId);

            if (container == null) {
                log.warn("Scheduler SQS listener container not found: {}", listenerId);

                continue;
            }

            if (!container.isRunning()) {
                try {
                    container.start();

                    log.info("Started scheduler SQS listener container: {}", listenerId);
                } catch (Exception e) {
                    log.warn("Failed to start scheduler SQS listener container: {}", listenerId, e);
                }
            }
        }
    }
}
