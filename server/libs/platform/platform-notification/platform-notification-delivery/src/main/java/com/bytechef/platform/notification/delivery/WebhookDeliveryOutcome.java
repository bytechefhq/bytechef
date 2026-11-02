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

package com.bytechef.platform.notification.delivery;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record WebhookDeliveryOutcome(
    List<PayloadResult> payloadResults, @Nullable Instant suspendedUntil, @Nullable String firstFailure,
    @Nullable String lastFailure) {

    public enum Status {
        SENT, FAILED, SKIPPED, DROPPED
    }

    public record PayloadResult(String label, Status status) {
    }

    public WebhookDeliveryOutcome {
        payloadResults = List.copyOf(payloadResults);
    }

    public int count(Status status) {
        int count = 0;

        for (PayloadResult payloadResult : payloadResults) {
            if (payloadResult.status() == status) {
                count++;
            }
        }

        return count;
    }

    public boolean anySent() {
        return count(Status.SENT) > 0;
    }

    public static WebhookDeliveryOutcome dropped(List<WebhookPayload> payloads) {
        List<PayloadResult> payloadResults = payloads.stream()
            .map(payload -> new PayloadResult(payload.label(), Status.DROPPED))
            .toList();

        return new WebhookDeliveryOutcome(payloadResults, null, null, null);
    }
}
