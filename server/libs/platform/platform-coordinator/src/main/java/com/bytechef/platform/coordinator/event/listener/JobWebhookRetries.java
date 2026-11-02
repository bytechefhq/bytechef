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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.notification.delivery.WebhookRetry;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author Ivica Cardic
 */
final class JobWebhookRetries {

    static WebhookRetry toWebhookRetry(Job.Webhook webhook) {
        Job.Retry retry = webhook.retry();

        return new WebhookRetry(getMaxAttempts(retry), getInitialInterval(retry), getMultiplier(retry));
    }

    private static int getMaxAttempts(Job.Retry retry) {
        return retry.maxAttempts() == null
            ? 5
            : retry.maxAttempts();
    }

    private static double getMultiplier(Job.Retry retry) {
        return retry.multiplier() == null
            ? 2.0
            : retry.multiplier();
    }

    private static Duration getInitialInterval(Job.Retry retry) {
        return retry.initialInterval() == null
            ? Duration.of(2, ChronoUnit.SECONDS)
            : Duration.of(retry.initialInterval(), ChronoUnit.SECONDS);
    }

    private JobWebhookRetries() {
    }
}
