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

package com.bytechef.platform.coordinator.notification;

import com.bytechef.platform.notification.domain.NotificationEvent.Type;
import com.bytechef.platform.notification.handler.NotificationEventType;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.SlackNotificationHandler;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Composes the Slack message text for job-status notifications, reusing the same i18n content keys as the email
 * handler. Payload shape and transport are owned by the shared Slack delivery client behind
 * {@code SlackNotificationSender}.
 *
 * @author Ivica Cardic
 */
@Component
@NotificationEventType({
    Type.JOB_CANCELLED, Type.JOB_CREATED, Type.JOB_COMPLETED, Type.JOB_FAILED, Type.JOB_STARTED, Type.JOB_STOPPED
})
public class JobStatusSlackNotificationHandler implements SlackNotificationHandler {

    private final MessageSource messageSource;

    public JobStatusSlackNotificationHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getText(NotificationHandlerContext notificationHandlerContext) {
        String icon = notificationHandlerContext.getEventType() == Type.JOB_FAILED
            ? ":rotating_light:" : ":information_source:";

        return icon + " " + messageSource.getMessage(
            "email." + notificationHandlerContext.getEventType() + ".content",
            new Object[] {
                notificationHandlerContext.getJobName(), notificationHandlerContext.getJobId()
            },
            Locale.getDefault());
    }
}
