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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.notification.domain.NotificationEvent.Type;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
class JobStatusWebhookNotificationHandlerIntTest {

    @Autowired
    private ApprovalReminderWebhookNotificationHandler approvalReminderWebhookNotificationHandler;

    @Autowired
    private JobStatusWebhookNotificationHandler jobStatusWebhookNotificationHandler;

    @Test
    void testPayloadIsContextMap() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .error(new NotificationHandlerContext.ErrorSummary("Send email", "email/v1/send", "boom", null))
            .eventType(Type.JOB_FAILED)
            .jobId(42L)
            .jobName("Daily sync")
            .build();

        Map<String, ?> payload = JsonUtils.readMap(
            jobStatusWebhookNotificationHandler.getPayload(notificationHandlerContext));

        assertThat(payload)
            .containsKeys("eventType", "jobId", "jobName", "timestamp");
        assertThat(((Map<?, ?>) payload.get("error")).get("message"))
            .isEqualTo("boom");
    }

    @Test
    void testPayloadForApprovalEventKeepsTopLevelApprovalKeys() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .approvalExpiresAt("2026-10-17T13:39:23Z")
            .approvalFormUrl("https://example.com/resume/v1.token")
            .eventType(Type.JOB_APPROVAL_EXPIRING)
            .jobId(4L)
            .jobName("Order approval")
            .build();

        Map<String, ?> payload = JsonUtils.readMap(
            approvalReminderWebhookNotificationHandler.getPayload(notificationHandlerContext));

        assertThat(payload.get("approvalExpiresAt"))
            .isEqualTo("2026-10-17T13:39:23Z");
        assertThat(payload.get("approvalFormUrl"))
            .isEqualTo("https://example.com/resume/v1.token");
        assertThat(((Map<?, ?>) payload.get("approval")).get("formUrl"))
            .isEqualTo("https://example.com/resume/v1.token");
    }

    @Test
    void testPayloadForApprovalEventKeepsNullApprovalKeysInJson() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .approvalExpiresAt("2026-10-17T13:39:23Z")
            .eventType(Type.JOB_APPROVAL_EXPIRING)
            .jobId(4L)
            .jobName("Order approval")
            .build();

        String payload = approvalReminderWebhookNotificationHandler.getPayload(notificationHandlerContext);

        assertThat(payload)
            .contains("\"approvalFormUrl\":null");
    }
}
