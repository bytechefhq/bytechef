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
import com.bytechef.platform.notification.handler.EmailNotificationHandler;
import com.bytechef.platform.notification.handler.NotificationEventType;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Shapes the EMAIL-channel escalation sent when a run has been awaiting approval past the escalation window and is
 * still unresolved. Delivery targets are the {@code JOB_APPROVAL_ESCALATED}-subscribed {@code Notification} rows, which
 * an operator points at a different recipient (a manager, an on-call channel) than the reminder.
 *
 * @author Ivica Cardic
 */
@Component
@NotificationEventType({
    Type.JOB_APPROVAL_ESCALATED
})
public class ApprovalEscalationEmailNotificationHandler implements EmailNotificationHandler {

    private final MessageSource messageSource;

    public ApprovalEscalationEmailNotificationHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getContent(NotificationHandlerContext notificationHandlerContext) {
        return messageSource.getMessage(
            "email." + notificationHandlerContext.getEventType() + ".content",
            getMessageArguments(notificationHandlerContext), Locale.getDefault());
    }

    @Override
    public String getSubject(NotificationHandlerContext notificationHandlerContext) {
        return messageSource.getMessage(
            "email." + notificationHandlerContext.getEventType() + ".subject",
            getMessageArguments(notificationHandlerContext), Locale.getDefault());
    }

    @Override
    public boolean isHtml() {
        return false;
    }

    static Object[] getMessageArguments(NotificationHandlerContext notificationHandlerContext) {
        String approvalFormUrl = notificationHandlerContext.getApprovalFormUrl();

        return new Object[] {
            notificationHandlerContext.getJobName(), notificationHandlerContext.getJobId(),
            notificationHandlerContext.getApprovalExpiresAt(),
            approvalFormUrl == null ? "the Approval Tasks page" : approvalFormUrl
        };
    }
}
