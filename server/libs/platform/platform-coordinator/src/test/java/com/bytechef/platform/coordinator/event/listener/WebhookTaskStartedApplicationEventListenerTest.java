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

package com.bytechef.platform.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.service.JobService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class WebhookTaskStartedApplicationEventListenerTest {

    private final JobService jobService = Mockito.mock(JobService.class);

    @Test
    void testPrivateUrlNotAllowedWhenEnabled() {
        WebhookTaskStartedApplicationEventListener listener =
            new WebhookTaskStartedApplicationEventListener(jobService, true, Set.of());

        assertThat(listener.isAllowedWebhookUrl("http://169.254.169.254/")).isFalse();
    }

    @Test
    void testPublicUrlAllowedWhenEnabled() {
        WebhookTaskStartedApplicationEventListener listener =
            new WebhookTaskStartedApplicationEventListener(jobService, true, Set.of());

        assertThat(listener.isAllowedWebhookUrl("https://1.1.1.1/hook")).isTrue();
    }

    @Test
    void testAllUrlsAllowedWhenDisabled() {
        WebhookTaskStartedApplicationEventListener listener =
            new WebhookTaskStartedApplicationEventListener(jobService, false, Set.of());

        assertThat(listener.isAllowedWebhookUrl("http://169.254.169.254/")).isTrue();
    }
}
