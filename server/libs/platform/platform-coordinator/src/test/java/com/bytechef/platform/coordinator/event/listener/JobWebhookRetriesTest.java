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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.notification.delivery.WebhookRetry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JobWebhookRetriesTest {

    @Test
    void testUsesDefaultRetryWhenTheWebhookHasNone() {
        Job.Webhook webhook =
            new Job.Webhook("job.status", "https://example.com", new Job.Retry(null, null, null, null));

        assertThat(JobWebhookRetries.toWebhookRetry(webhook))
            .isEqualTo(new WebhookRetry(5, Duration.ofSeconds(2), 2.0));
    }

    @Test
    void testMapsMaxAttemptsToMaxRetries() {
        Job.Webhook webhook = new Job.Webhook("job.status", "https://example.com", new Job.Retry(3, 60, 7, 4));

        assertThat(JobWebhookRetries.toWebhookRetry(webhook))
            .isEqualTo(new WebhookRetry(7, Duration.ofSeconds(3), 4.0));
    }
}
