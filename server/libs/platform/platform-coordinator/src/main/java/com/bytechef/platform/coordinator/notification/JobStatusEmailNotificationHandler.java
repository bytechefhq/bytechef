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

import com.bytechef.platform.configuration.domain.NotificationEvent.Type;
import com.bytechef.platform.configuration.notification.EmailNotificationHandler;
import com.bytechef.platform.configuration.notification.NotificationEventType;
import com.bytechef.platform.configuration.notification.NotificationHandlerContext;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
@NotificationEventType({
    Type.JOB_CANCELLED, Type.JOB_CREATED, Type.JOB_COMPLETED, Type.JOB_FAILED, Type.JOB_STARTED
})
public class JobStatusEmailNotificationHandler implements EmailNotificationHandler {

    private final MessageSource messageSource;

    public JobStatusEmailNotificationHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getContent(NotificationHandlerContext notificationHandlerContext) {
        return messageSource.getMessage(
            "email." + notificationHandlerContext.getEventType() + ".content",
            new Object[] {
                notificationHandlerContext.getJobName(), notificationHandlerContext.getJobId()
            },
            Locale.getDefault());
    }

    @Override
    public String getSubject(NotificationHandlerContext notificationHandlerContext) {
        return messageSource.getMessage(
            "email." + notificationHandlerContext.getEventType() + ".subject",
            new Object[] {
                notificationHandlerContext.getJobName(), notificationHandlerContext.getJobId()
            },
            Locale.getDefault());
    }

    @Override
    public boolean isHtml() {
        return false;
    }
}
