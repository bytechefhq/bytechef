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
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.coordinator.event.listener.ConcurrencySlotReleaseApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.NotificationJobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.SseStreamApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.WebhookJobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.WebhookTaskStartedApplicationEventListener;
import com.bytechef.platform.coordinator.metrics.JobExecutionCounter;
import com.bytechef.platform.coordinator.monitor.JobRetentionMonitor;
import com.bytechef.platform.coordinator.monitor.JobTimeoutMonitor;
import com.bytechef.platform.coordinator.monitor.OrphanedJobRecoveryMonitor;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.ratelimit.ConcurrentExecutionGate;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

    private final JobService jobService;
    private final NotificationHandlerRegistry notificationHandlerRegistry;
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;

    @SuppressFBWarnings("EI")
    public PlatformCoordinatorConfiguration(
        JobService jobService, NotificationHandlerRegistry notificationHandlerRegistry,
        NotificationSenderRegistry notificationSenderRegistry, NotificationService notificationService) {

        this.jobService = jobService;
        this.notificationHandlerRegistry = notificationHandlerRegistry;
        this.notificationSenderRegistry = notificationSenderRegistry;
        this.notificationService = notificationService;
    }

    @Bean
    @ConditionalOnProperty(name = "bytechef.observability.enabled", havingValue = "true")
    JobExecutionCounter jobExecutionCounter(MeterRegistry meterRegistry) {
        return new JobExecutionCounter(meterRegistry);
    }

    @Bean
    @ConditionalOnBean(ConcurrentExecutionGate.class)
    ConcurrencySlotReleaseApplicationEventListener concurrencySlotReleaseApplicationEventListener(
        ConcurrentExecutionGate concurrentExecutionGate) {

        return new ConcurrencySlotReleaseApplicationEventListener(concurrentExecutionGate);
    }

    @Bean
    @ConditionalOnProperty(
        name = "bytechef.workflow.execution.recovery.enabled", havingValue = "true", matchIfMissing = true)
    OrphanedJobRecoveryMonitor orphanedJobRecoveryMonitor(
        @Value("${bytechef.workflow.execution.recovery.auto-resume:false}") boolean autoResume,
        ApplicationEventPublisher eventPublisher,
        @Value("${bytechef.workflow.execution.recovery.max-auto-resume-attempts:3}") int maxAutoResumeAttempts,
        @Value("${bytechef.workflow.execution.recovery.staleness-threshold:PT5M}") Duration stalenessThreshold,
        TaskExecutionService taskExecutionService, TenantService tenantService) {

        return new OrphanedJobRecoveryMonitor(
            autoResume, eventPublisher, jobService, maxAutoResumeAttempts, stalenessThreshold, taskExecutionService,
            tenantService);
    }

    @Bean
    @ConditionalOnProperty(
        name = "bytechef.workflow.execution.timeout.enabled", havingValue = "true", matchIfMissing = true)
    JobTimeoutMonitor jobTimeoutMonitor(
        @Value("${bytechef.workflow.execution.timeout.default-timeout:#{null}}") Duration defaultTimeout,
        ApplicationEventPublisher eventPublisher,
        ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider,
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        TaskExecutionService taskExecutionService, TenantService tenantService) {

        return new JobTimeoutMonitor(
            defaultTimeout, eventPublisher, jobService, planLimitRejectionCounterObjectProvider,
            planLimitsProviderObjectProvider, taskExecutionService, tenantService);
    }

    @Bean
    @ConditionalOnProperty(
        name = "bytechef.workflow.execution.retention.enabled", havingValue = "true", matchIfMissing = true)
    JobRetentionMonitor jobRetentionMonitor(
        ObjectProvider<DataStorage> dataStorageObjectProvider,
        @Value("${bytechef.workflow.execution.retention.default-retention-days:#{null}}") Integer defaultRetentionDays,
        JobFacade jobFacade, ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        TenantService tenantService) {

        return new JobRetentionMonitor(
            dataStorageObjectProvider, defaultRetentionDays, jobFacade, jobService, planLimitsProviderObjectProvider,
            tenantService);
    }

    @Bean
    WebhookJobStatusApplicationEventListener webhookJobStatusApplicationEventListener(
        WebhookNotificationClient webhookNotificationClient) {

        return new WebhookJobStatusApplicationEventListener(jobService, webhookNotificationClient);
    }

    @Bean
    WebhookTaskStartedApplicationEventListener taskStartedWebhookEventListener(
        @Value("${bytechef.security.ssrf.enabled:true}") boolean ssrfEnabled,
        @Value("${bytechef.security.ssrf.allowed-hosts:}") Set<String> ssrfAllowedHosts) {

        return new WebhookTaskStartedApplicationEventListener(jobService, ssrfEnabled, ssrfAllowedHosts);
    }

    @Bean
    NotificationJobStatusApplicationEventListener jobStatusApplicationEventListener(
        Optional<JobExecutionCounter> jobExecutionCounter) {

        return new NotificationJobStatusApplicationEventListener(
            jobExecutionCounter.orElse(null), jobService, notificationHandlerRegistry, notificationSenderRegistry,
            notificationService);
    }

    @Bean
    SseStreamApplicationEventListener sseStreamApplicationEventListener(MessageBroker messageBroker) {
        return new SseStreamApplicationEventListener(messageBroker);
    }
}
