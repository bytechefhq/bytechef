/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.platform.configuration.notification.NotificationHandlerRegistry;
import com.bytechef.platform.configuration.notification.NotificationSenderRegistry;
import com.bytechef.platform.configuration.service.NotificationService;
import com.bytechef.platform.coordinator.event.listener.JobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.WebhookJobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.WebhookTaskStartedApplicationEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Matija Petanjek
 */
@Configuration
@ConditionalOnCoordinator
public class PlatformCoordinatorConfiguration {

    @Autowired
    private JobService jobService;

    @Autowired
    private NotificationHandlerRegistry notificationHandlerRegistry;

    @Autowired
    private NotificationSenderRegistry notificationSenderRegistry;

    @Autowired
    private NotificationService notificationService;

    @Bean
    WebhookJobStatusApplicationEventListener webhookJobStatusApplicationEventListener() {
        return new WebhookJobStatusApplicationEventListener(jobService);
    }

    @Bean
    WebhookTaskStartedApplicationEventListener taskStartedWebhookEventListener() {
        return new WebhookTaskStartedApplicationEventListener(jobService);
    }

    @Bean
    JobStatusApplicationEventListener jobStatusApplicationEventListener() {
        return new JobStatusApplicationEventListener(
            jobService, notificationHandlerRegistry, notificationSenderRegistry, notificationService);
    }

}
