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

import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.notification.domain.NotificationEvent.Type;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
class JobStatusSlackNotificationHandlerIntTest {

    @Autowired
    private JobStatusSlackNotificationHandler jobStatusSlackNotificationHandler;

    @Test
    void testTextStartsWithIconAndAppendsDetails() {
        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .eventType(Type.JOB_FAILED)
            .executionUrl("https://x/automation/workflow-executions?id=42")
            .jobId(42L)
            .jobName("Daily sync")
            .build();

        String text = jobStatusSlackNotificationHandler.getText(notificationHandlerContext);

        assertThat(text)
            .startsWith(":rotating_light: ")
            .endsWith("\nExecution: https://x/automation/workflow-executions?id=42");
    }
}
