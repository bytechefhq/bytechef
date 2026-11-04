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
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.notification.domain.NotificationEvent.Type;
import com.bytechef.platform.notification.handler.NotificationEventType;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
class JobStatusEmailNotificationHandlerIntTest {

    @Autowired
    private JobStatusEmailNotificationHandler jobStatusEmailNotificationHandler;

    @Test
    void testJobStoppedIsHandled() {
        assertThat(handledTypes()).contains(Type.JOB_STOPPED);
    }

    @Test
    void testJobStoppedEmail() {
        NotificationHandlerContext notificationHandlerContext = createContext(Type.JOB_STOPPED);

        assertThat(jobStatusEmailNotificationHandler.getSubject(notificationHandlerContext))
            .isEqualTo("Job Execution Status - Job Daily sync (ID: 42) stopped");
        assertThat(jobStatusEmailNotificationHandler.getContent(notificationHandlerContext))
            .isEqualTo("Job Daily sync (ID: 42) was stopped.");
    }

    @Test
    void testContentAppendsDetailsWhenPresent() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .environment("PRODUCTION")
            .error(new NotificationHandlerContext.ErrorSummary("Send email", "email/v1/send", "boom", null))
            .eventType(Type.JOB_FAILED)
            .executionUrl("https://x/automation/executions?id=42")
            .jobId(42L)
            .jobName("Daily sync")
            .project(new NotificationHandlerContext.ProjectSummary(3L, "CRM", 3))
            .workflow(new NotificationHandlerContext.WorkflowSummary("wf1", "Daily sync"))
            .build();

        assertThat(jobStatusEmailNotificationHandler.getContent(notificationHandlerContext))
            .isEqualTo(
                "Job Daily sync (ID: 42) execution failed.\n\nWorkflow: Daily sync\nProject: CRM (v3)\n" +
                    "Environment: PRODUCTION\nFailed task: Send email (email/v1/send)\nError: boom\n" +
                    "Execution: https://x/automation/executions?id=42");
    }

    @ParameterizedTest
    @MethodSource("handledTypes")
    void testEveryHandledTypeHasEmailText(Type type) {
        NotificationHandlerContext notificationHandlerContext = createContext(type);

        assertThatCode(() -> jobStatusEmailNotificationHandler.getSubject(notificationHandlerContext))
            .doesNotThrowAnyException();
        assertThatCode(() -> jobStatusEmailNotificationHandler.getContent(notificationHandlerContext))
            .doesNotThrowAnyException();
    }

    private static NotificationHandlerContext createContext(Type type) {
        return new NotificationHandlerContext.Builder()
            .eventType(type)
            .jobId(42L)
            .jobName("Daily sync")
            .build();
    }

    private static Type[] handledTypes() {
        NotificationEventType notificationEventType =
            JobStatusEmailNotificationHandler.class.getAnnotation(NotificationEventType.class);

        return Arrays.copyOf(notificationEventType.value(), notificationEventType.value().length);
    }
}
