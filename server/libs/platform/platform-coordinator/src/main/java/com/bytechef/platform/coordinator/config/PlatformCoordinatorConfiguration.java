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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.coordinator.event.listener.NotificationJobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.SseStreamApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.WebhookJobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.WebhookTaskStartedApplicationEventListener;
import com.bytechef.platform.coordinator.metrics.JobExecutionCounter;
import com.bytechef.platform.coordinator.notification.NotificationThrottle;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Matija Petanjek
 */
@Configuration
@ConditionalOnCoordinator
public class PlatformCoordinatorConfiguration {

    private static final Duration MINIMUM_NOTIFICATION_THROTTLE_TTL = Duration.ofMinutes(10);

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
    WebhookJobStatusApplicationEventListener webhookJobStatusApplicationEventListener() {
        return new WebhookJobStatusApplicationEventListener(jobService);
    }

    @Bean
    WebhookTaskStartedApplicationEventListener taskStartedWebhookEventListener() {
        return new WebhookTaskStartedApplicationEventListener(jobService);
    }

    @Bean
    NotificationJobStatusApplicationEventListener jobStatusApplicationEventListener(
        ApplicationProperties applicationProperties, ObjectProvider<CacheManager> cacheManagerProvider,
        Optional<JobExecutionCounter> jobExecutionCounter) {

        ApplicationProperties.Mail mail = applicationProperties.getMail();

        return new NotificationJobStatusApplicationEventListener(
            jobExecutionCounter.orElse(null), jobService, notificationHandlerRegistry, notificationSenderRegistry,
            notificationService,
            getNotificationThrottle(cacheManagerProvider, mail.getFailureNotificationThrottleWindow()));
    }

    @Bean
    SseStreamApplicationEventListener sseStreamApplicationEventListener(
        MessageBroker messageBroker, TaskExecutionService taskExecutionService) {

        return new SseStreamApplicationEventListener(messageBroker, taskExecutionService);
    }

    private static NotificationThrottle getNotificationThrottle(
        ObjectProvider<CacheManager> cacheManagerProvider, Duration window) {

        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();

        Duration claimTtl = window.isZero() || window.isNegative() ? MINIMUM_NOTIFICATION_THROTTLE_TTL : window;
        Duration suppressedCountTtl = window.multipliedBy(2);

        if (suppressedCountTtl.compareTo(MINIMUM_NOTIFICATION_THROTTLE_TTL) < 0) {
            suppressedCountTtl = MINIMUM_NOTIFICATION_THROTTLE_TTL;
        }

        return new NotificationThrottle(
            getCache(cacheManager, NotificationThrottle.CLAIM_CACHE_NAME, claimTtl),
            getCache(cacheManager, NotificationThrottle.SUPPRESSED_COUNT_CACHE_NAME, suppressedCountTtl), window);
    }

    private static Cache getCache(@Nullable CacheManager cacheManager, String name, Duration ttl) {
        Cache cache = cacheManager == null ? null : cacheManager.getCache(name);

        if (cache != null) {
            return cache;
        }

        return new CaffeineCache(
            name,
            Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(ttl)
                .build());
    }
}
