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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.notification.domain.NotificationEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class NotificationThrottleTest {

    private static final Instant START = Instant.parse("2026-10-19T10:00:00Z");

    private final Clock clock = mock(Clock.class);
    private final NotificationThrottle notificationThrottle = new NotificationThrottle(Duration.ofHours(1), clock);

    @Test
    void testFirstNotificationIsSentWithNoSuppressedCount() {
        when(clock.instant()).thenReturn(START);

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
    }

    @Test
    void testRepeatedNotificationWithinTheWindowIsSuppressed() {
        when(clock.instant()).thenReturn(START);

        notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        when(clock.instant()).thenReturn(START.plus(Duration.ofMinutes(59)));

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .isEmpty();
    }

    @Test
    void testNotificationAfterTheWindowIsSentWithTheSuppressedCount() {
        when(clock.instant()).thenReturn(START);

        notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        for (int i = 1; i <= 3; i++) {
            when(clock.instant()).thenReturn(START.plus(Duration.ofMinutes(5L * i)));

            notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);
        }

        when(clock.instant()).thenReturn(START.plus(Duration.ofHours(1)));

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(3);

        when(clock.instant()).thenReturn(START.plus(Duration.ofHours(1))
            .plus(Duration.ofMinutes(5)));

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .isEmpty();

        when(clock.instant()).thenReturn(START.plus(Duration.ofHours(2)));

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(1);
    }

    @Test
    void testDifferentNotificationWorkflowOrEventTypeIsThrottledSeparately() {
        when(clock.instant()).thenReturn(START);

        notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        assertThat(notificationThrottle.tryAcquire(2L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
        assertThat(notificationThrottle.tryAcquire(1L, "workflow2", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_COMPLETED))
            .hasValue(0);
    }

    @Test
    void testZeroWindowDisablesThrottling() {
        NotificationThrottle disabledNotificationThrottle = new NotificationThrottle(Duration.ZERO, clock);

        when(clock.instant()).thenReturn(START);

        disabledNotificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        assertThat(disabledNotificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
    }
}
