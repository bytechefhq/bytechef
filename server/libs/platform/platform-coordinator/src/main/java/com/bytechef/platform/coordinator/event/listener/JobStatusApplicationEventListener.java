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
import com.bytechef.platform.configuration.domain.Notification;
import com.bytechef.platform.configuration.domain.NotificationEvent;
import com.bytechef.platform.configuration.notification.NotificationHandler;
import com.bytechef.platform.configuration.notification.NotificationHandlerContext;
import com.bytechef.platform.configuration.notification.NotificationHandlerRegistry;
import com.bytechef.platform.configuration.notification.NotificationSender;
import com.bytechef.platform.configuration.notification.NotificationSenderRegistry;
import com.bytechef.platform.configuration.service.NotificationService;
import com.bytechef.platform.coordinator.metrics.JobExecutionCounter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;

/**
 * @author Matija Petanjek
 */
public class JobStatusApplicationEventListener implements ApplicationEventListener {

    private final Optional<JobExecutionCounter> jobExecutionCounter;
    private final JobService jobService;
    private final NotificationHandlerRegistry notificationHandlerRegistry;
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;

    @SuppressFBWarnings("EI")
    public JobStatusApplicationEventListener(
        Optional<JobExecutionCounter> jobExecutionCounter, JobService jobService,
        NotificationHandlerRegistry notificationHandlerRegistry,
        NotificationSenderRegistry notificationSenderRegistry, NotificationService notificationService) {

        this.jobExecutionCounter = jobExecutionCounter;
        this.jobService = jobService;
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

            jobExecutionCounter.ifPresent(
                jobExecutionCounter -> jobExecutionCounter.process(jobStatusApplicationEvent, job));

            Job.Status status = jobStatusApplicationEvent.getStatus();

            NotificationEvent.Type eventType =
                NotificationEvent.Type.of(NotificationEvent.Source.JOB, status.toString());

            NotificationHandlerContext notificationHandlerContext = getNotificationHandlerContext(
                eventType, job);

            List<Notification> notifications = notificationService.getNotifications(eventType);

            for (Notification notification : notifications) {
                NotificationSender notificationSender = notificationSenderRegistry.getNotificationSender(
                    notification.getType());

                NotificationHandler notificationHandler = notificationHandlerRegistry.getNotificationHandler(
                    eventType, notification.getType());

                notificationSender.send(notification, notificationHandler, notificationHandlerContext);
            }
        }
    }

    private NotificationHandlerContext getNotificationHandlerContext(NotificationEvent.Type eventType, Job job) {
        return new NotificationHandlerContext.Builder()
            .eventType(eventType)
            .jobId(job.getId())
            .jobName(job.getLabel())
            .build();
    }

}
