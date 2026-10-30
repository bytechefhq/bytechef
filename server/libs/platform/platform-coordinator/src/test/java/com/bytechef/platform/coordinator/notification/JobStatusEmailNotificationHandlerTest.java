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

import com.bytechef.platform.notification.domain.NotificationEvent.Type;
import com.bytechef.platform.notification.handler.NotificationEventType;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * @author Ivica Cardic
 */
class JobStatusEmailNotificationHandlerTest {

    private final JobStatusEmailNotificationHandler jobStatusEmailNotificationHandler =
        new JobStatusEmailNotificationHandler(createMessageSource());

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

    private static ResourceBundleMessageSource createMessageSource() {
        ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();

        resourceBundleMessageSource.setBasename("messages");
        resourceBundleMessageSource.setDefaultEncoding("UTF-8");

        return resourceBundleMessageSource;
    }

    private static Type[] handledTypes() {
        NotificationEventType notificationEventType =
            JobStatusEmailNotificationHandler.class.getAnnotation(NotificationEventType.class);

        return Arrays.copyOf(notificationEventType.value(), notificationEventType.value().length);
    }
}
