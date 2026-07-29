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
 * Shapes the SLACK-channel reminder sent while a run is paused on an approval that is about to expire.
 *
 * @author Ivica Cardic
 */
@Component
@NotificationEventType({
    Type.JOB_APPROVAL_EXPIRING
})
public class ApprovalReminderSlackNotificationHandler implements SlackNotificationHandler {

    private final MessageSource messageSource;

    public ApprovalReminderSlackNotificationHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getText(NotificationHandlerContext notificationHandlerContext) {
        return ":hourglass_flowing_sand: " + messageSource.getMessage(
            "email." + notificationHandlerContext.getEventType() + ".content",
            ApprovalReminderEmailNotificationHandler.getMessageArguments(notificationHandlerContext),
            Locale.getDefault());
    }
}
