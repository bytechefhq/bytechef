/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.coordinator.config;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.coordinator.event.listener.ExecutionDataJobDeletionListener;
import com.bytechef.platform.coordinator.event.listener.NotificationJobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.SseStreamApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.SuspendedTaskStateJobDeletionListener;
import com.bytechef.platform.coordinator.event.listener.WebhookJobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.WebhookTaskStartedApplicationEventListener;
import com.bytechef.platform.coordinator.metrics.JobExecutionCounter;
import com.bytechef.platform.coordinator.monitor.ApprovalEscalationMonitor;
import com.bytechef.platform.coordinator.monitor.ApprovalExpiryMonitor;
import com.bytechef.platform.coordinator.monitor.ApprovalReminderMonitor;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Matija Petanjek
 */
@Configuration
@ConditionalOnCoordinator
public class PlatformCoordinatorConfiguration {

    private final ApplicationProperties.Workflow.Execution execution;
    private final JobService jobService;
    private final NotificationHandlerRegistry notificationHandlerRegistry;
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    public PlatformCoordinatorConfiguration(
        ApplicationProperties applicationProperties, JobService jobService,
        NotificationHandlerRegistry notificationHandlerRegistry, NotificationSenderRegistry notificationSenderRegistry,
        NotificationService notificationService) {

        ApplicationProperties.Workflow workflow = applicationProperties.getWorkflow();

        this.execution = workflow.getExecution();
        this.jobService = jobService;
        this.notificationHandlerRegistry = notificationHandlerRegistry;
        this.notificationSenderRegistry = notificationSenderRegistry;
        this.notificationService = notificationService;
        this.publicUrl = applicationProperties.getPublicUrl();
    }

    @Bean
    @ConditionalOnProperty(name = "bytechef.observability.enabled", havingValue = "true")
    JobExecutionCounter jobExecutionCounter(MeterRegistry meterRegistry) {
        return new JobExecutionCounter(meterRegistry);
    }

    @Bean
    WebhookJobStatusApplicationEventListener webhookJobStatusApplicationEventListener(
        WebhookNotificationClient webhookNotificationClient) {

        return new WebhookJobStatusApplicationEventListener(jobService, webhookNotificationClient);
    }

    @Bean
    WebhookTaskStartedApplicationEventListener taskStartedWebhookEventListener() {
        return new WebhookTaskStartedApplicationEventListener(jobService);
    }

    @Bean
    NotificationJobStatusApplicationEventListener jobStatusApplicationEventListener(
        Optional<JobExecutionCounter> jobExecutionCounter) {

        return new NotificationJobStatusApplicationEventListener(
            jobExecutionCounter.orElse(null), jobService, notificationHandlerRegistry, notificationSenderRegistry,
            notificationService);
    }

    @Bean
    @ConditionalOnProperty(
        name = "bytechef.workflow.execution.approval-expiry.enabled", havingValue = "true", matchIfMissing = true)
    ApprovalExpiryMonitor approvalExpiryMonitor(
        ApplicationEventPublisher eventPublisher, ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
        TaskExecutionService taskExecutionService,
        ObjectProvider<TaskStateService> taskStateServiceObjectProvider, TenantService tenantService) {

        return new ApprovalExpiryMonitor(
            eventPublisher, jobService, meterRegistryObjectProvider, taskExecutionService,
            taskStateServiceObjectProvider.getIfAvailable(), tenantService);
    }

    @Bean
    ExecutionDataJobDeletionListener executionDataJobDeletionListener(
        ObjectProvider<DataStorage> dataStorageObjectProvider) {

        return new ExecutionDataJobDeletionListener(dataStorageObjectProvider.getIfAvailable());
    }

    @Bean
    SuspendedTaskStateJobDeletionListener suspendedTaskStateJobDeletionListener(
        ObjectProvider<TaskStateService> taskStateServiceObjectProvider) {

        return new SuspendedTaskStateJobDeletionListener(taskStateServiceObjectProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(
        name = "bytechef.workflow.execution.approval-reminder.enabled", havingValue = "true", matchIfMissing = true)
    ApprovalReminderMonitor approvalReminderMonitor(
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider, TaskExecutionService taskExecutionService,
        TenantService tenantService) {

        ApplicationProperties.Workflow.Execution.ApprovalReminder approvalReminder = execution.getApprovalReminder();

        return new ApprovalReminderMonitor(
            approvalTokensObjectProvider.getIfAvailable(), jobService, approvalReminder.getLeadTime(),
            notificationHandlerRegistry, notificationSenderRegistry, notificationService, publicUrl,
            taskExecutionService, tenantService);
    }

    @Bean
    @ConditionalOnProperty(
        name = "bytechef.workflow.execution.approval-escalation.enabled", havingValue = "true",
        matchIfMissing = true)
    ApprovalEscalationMonitor approvalEscalationMonitor(
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider, TaskExecutionService taskExecutionService,
        TenantService tenantService) {

        ApplicationProperties.Workflow.Execution.ApprovalEscalation approvalEscalation =
            execution.getApprovalEscalation();

        return new ApprovalEscalationMonitor(
            approvalEscalation.getAfter(), approvalTokensObjectProvider.getIfAvailable(), jobService,
            notificationHandlerRegistry, notificationSenderRegistry, notificationService, publicUrl,
            taskExecutionService, tenantService);
    }

    @Bean
    SseStreamApplicationEventListener sseStreamApplicationEventListener(
        JobService jobService, MessageBroker messageBroker, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        return new SseStreamApplicationEventListener(jobService, messageBroker, taskExecutionService, taskFileStorage);
    }
}
