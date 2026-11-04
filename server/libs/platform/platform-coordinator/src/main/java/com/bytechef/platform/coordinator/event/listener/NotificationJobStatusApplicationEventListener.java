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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.coordinator.metrics.JobExecutionCounter;
import com.bytechef.platform.coordinator.notification.NotificationHandlerContexts;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matija Petanjek
 */
public class NotificationJobStatusApplicationEventListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationJobStatusApplicationEventListener.class);

    private final @Nullable JobExecutionCounter jobExecutionCounter;
    private final JobService jobService;
    private final NotificationHandlerContexts notificationHandlerContexts;
    private final NotificationHandlerRegistry notificationHandlerRegistry;
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;

    @SuppressFBWarnings("EI")
    public NotificationJobStatusApplicationEventListener(
        @Nullable JobExecutionCounter jobExecutionCounter, JobService jobService,
        NotificationHandlerContexts notificationHandlerContexts,
        NotificationHandlerRegistry notificationHandlerRegistry,
        NotificationSenderRegistry notificationSenderRegistry, NotificationService notificationService) {

        this.jobExecutionCounter = jobExecutionCounter;
        this.jobService = jobService;
        this.notificationHandlerContexts = notificationHandlerContexts;
        this.notificationHandlerRegistry = notificationHandlerRegistry;
        this.notificationService = notificationService;
        this.notificationSenderRegistry = notificationSenderRegistry;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) {

            Job job = jobService.getJob(jobStatusApplicationEvent.getJobId());

            if (jobExecutionCounter != null) {
                jobExecutionCounter.process(jobStatusApplicationEvent, job);
            }

            Job.Status status = jobStatusApplicationEvent.getStatus();

            // Not every job status has a notification event; throwing here would abort the remaining coordinator
            // listeners.
            NotificationEvent.Type eventType =
                NotificationEvent.Type.of(NotificationEvent.Source.JOB, status.toString())
                    .orElse(null);

            if (eventType == null) {
                return;
            }

            List<Notification> notifications = notificationService.getNotifications(eventType);

            if (notifications.isEmpty()) {
                return;
            }

            NotificationHandlerContext notificationHandlerContext =
                notificationHandlerContexts.builder(job, status, eventType)
                    .build();

            for (Notification notification : notifications) {
                NotificationSender notificationSender = notificationSenderRegistry.getNotificationSender(
                    notification.getType());

                NotificationHandler notificationHandler = notificationHandlerRegistry.getNotificationHandler(
                    eventType, notification.getType());

                if (notificationSender == null || notificationHandler == null) {
                    log.warn(
                        "No {} found for notification {} and event type {}; skipping",
                        notificationSender == null ? "sender" : "handler", notification.getId(), eventType);

                    continue;
                }

                try {
                    notificationSender.send(notification, notificationHandler, notificationHandlerContext);
                } catch (RuntimeException runtimeException) {
                    log.error(
                        "Could not send notification {} for event type {} of job {}", notification.getId(), eventType,
                        job.getId(), runtimeException);
                }
            }
        }
    }
}
