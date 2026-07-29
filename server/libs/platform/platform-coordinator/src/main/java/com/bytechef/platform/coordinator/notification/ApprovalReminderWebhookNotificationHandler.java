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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.notification.domain.NotificationEvent.Type;
import com.bytechef.platform.notification.handler.NotificationEventType;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.WebhookNotificationHandler;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Shapes the JSON payload for WEBHOOK-channel reminders sent while a run is paused on an approval that is about to
 * expire. Transport, headers, and HMAC signing are owned by the shared delivery client behind
 * {@code WebhookNotificationSender}.
 *
 * @author Ivica Cardic
 */
@Component
@NotificationEventType({
    Type.JOB_APPROVAL_EXPIRING
})
public class ApprovalReminderWebhookNotificationHandler implements WebhookNotificationHandler {

    @Override
    public String getPayload(NotificationHandlerContext notificationHandlerContext) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("eventType", String.valueOf(notificationHandlerContext.getEventType()));
        payload.put("jobId", notificationHandlerContext.getJobId());
        payload.put("jobName", notificationHandlerContext.getJobName());
        payload.put("approvalExpiresAt", notificationHandlerContext.getApprovalExpiresAt());
        payload.put("approvalFormUrl", notificationHandlerContext.getApprovalFormUrl());

        Instant now = Instant.now();

        payload.put("timestamp", now.toString());

        return JsonUtils.write(payload);
    }
}
