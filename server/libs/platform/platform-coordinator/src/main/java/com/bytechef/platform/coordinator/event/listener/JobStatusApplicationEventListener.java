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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.configuration.domain.notification.Event;
import com.bytechef.platform.configuration.domain.notification.Notification;
import com.bytechef.platform.configuration.notification.NotificationHandler;
import com.bytechef.platform.configuration.notification.NotificationHandlerContext;
import com.bytechef.platform.configuration.notification.NotificationHandlerRegistry;
import com.bytechef.platform.configuration.notification.NotificationSender;
import com.bytechef.platform.configuration.notification.NotificationSenderRegistry;
import com.bytechef.platform.configuration.service.NotificationService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class JobStatusApplicationEventListener implements ApplicationEventListener {

    private JobService jobService;
    private NotificationHandlerRegistry notificationHandlerRegistry;
    private NotificationSenderRegistry notificationSenderRegistry;
    private NotificationService notificationService;

    public JobStatusApplicationEventListener(
        JobService jobService, NotificationHandlerRegistry notificationHandlerRegistry,
        NotificationSenderRegistry notificationSenderRegistry, NotificationService notificationService) {

        this.jobService = jobService;
        this.notificationHandlerRegistry = notificationHandlerRegistry;
        this.notificationService = notificationService;
        this.notificationSenderRegistry = notificationSenderRegistry;

    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) {

            Event.Type eventType = Event.Type.of(Event.Source.JOB, jobStatusApplicationEvent.getStatus()
                .toString());

            NotificationHandlerContext notificationHandlerContext = getNotificationHandlerContext(
                eventType, jobService.getJob(jobStatusApplicationEvent.getJobId()));

            List<Notification> notifications =
                notificationService.fetchNotifications(eventType);

            for (Notification notification : notifications) {
                NotificationSender notificationSender = notificationSenderRegistry.getNotificationSender(
                    notification.getType());

                NotificationHandler notificationHandler = notificationHandlerRegistry.getNotificationHandler(
                    eventType, notification.getType());

                notificationSender.send(notification, notificationHandler, notificationHandlerContext);
            }
        }
    }

    private NotificationHandlerContext getNotificationHandlerContext(Event.Type eventType, Job job) {
        return new NotificationHandlerContext.Builder()
            .eventType(eventType)
            .jobId(job.getId())
            .jobName(job.getLabel())
            .build();
    }

}
