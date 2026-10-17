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

import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * @author Ivica Cardic
 */
class ApprovalReminderEmailNotificationHandlerTest {

    private static final String FORM_URL = "https://example.com/resume/v1.signed.token";

    @Test
    void testContentKeepsTheFormUrlOnALineOfItsOwn() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

        messageSource.setBasename("messages");

        ApprovalReminderEmailNotificationHandler approvalReminderEmailNotificationHandler =
            new ApprovalReminderEmailNotificationHandler(messageSource);

        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .approvalExpiresAt("2026-10-17T13:39:23Z")
            .approvalFormUrl(FORM_URL)
            .eventType(NotificationEvent.Type.JOB_APPROVAL_EXPIRING)
            .jobId(4L)
            .jobName("Order approval")
            .build();

        String content = approvalReminderEmailNotificationHandler.getContent(notificationHandlerContext);

        assertThat(content.lines())
            .contains(FORM_URL);
    }
}
