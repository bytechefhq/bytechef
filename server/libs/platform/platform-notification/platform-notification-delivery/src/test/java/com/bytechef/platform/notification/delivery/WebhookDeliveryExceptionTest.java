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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class WebhookDeliveryExceptionTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {
        500, 503, 429
    })
    void testRetryableStatuses(Integer statusCode) {
        assertThat(new WebhookDeliveryException("m", statusCode, null).isRetryable()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {
        400, 404, 302
    })
    void testNonRetryableStatuses(int statusCode) {
        assertThat(new WebhookDeliveryException("m", statusCode, null).isRetryable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {
        301, 302, 404, 405, 410, 415
    })
    void testEndpointUnusableStatuses(int statusCode) {
        assertThat(new WebhookDeliveryException("m", statusCode, null).isEndpointUnusable()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {
        400, 500
    })
    void testEndpointUsableStatuses(Integer statusCode) {
        assertThat(new WebhookDeliveryException("m", statusCode, null).isEndpointUnusable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {
        400, 409, 422
    })
    void testPayloadRejectedStatuses(int statusCode) {
        assertThat(new WebhookDeliveryException("m", statusCode, null).isPayloadRejected()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {
        404, 500
    })
    void testPayloadNotRejectedStatuses(int statusCode) {
        assertThat(new WebhookDeliveryException("m", statusCode, null).isPayloadRejected()).isFalse();
    }

    @Test
    void testInvalidUrlIsUnusableAndNotRetryable() {
        InvalidWebhookUrlException invalidWebhookUrlException = new InvalidWebhookUrlException("m", null);

        assertThat(invalidWebhookUrlException.isEndpointUnusable()).isTrue();
        assertThat(invalidWebhookUrlException.isRetryable()).isFalse();
    }
}
