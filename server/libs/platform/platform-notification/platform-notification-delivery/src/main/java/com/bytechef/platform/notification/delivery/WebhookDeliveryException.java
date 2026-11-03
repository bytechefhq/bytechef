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

import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class WebhookDeliveryException extends RuntimeException {

    private final @Nullable Integer statusCode;
    private final @Nullable String responseBody;

    public WebhookDeliveryException(String message, @Nullable Integer statusCode) {
        super(message);

        this.statusCode = statusCode;
        this.responseBody = null;
    }

    public WebhookDeliveryException(String message, @Nullable Integer statusCode, @Nullable Throwable cause) {
        this(message, statusCode, cause, null);
    }

    public WebhookDeliveryException(
        String message, @Nullable Integer statusCode, @Nullable Throwable cause, @Nullable String responseBody) {

        super(message, cause);

        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public WebhookDeliveryException(String message, Throwable cause) {
        super(message, cause);

        this.statusCode = null;
        this.responseBody = null;
    }

    public @Nullable Integer getStatusCode() {
        return statusCode;
    }

    public @Nullable String getResponseBody() {
        return responseBody;
    }

    public boolean isRetryable() {
        return statusCode == null || statusCode >= 500 || statusCode == 429;
    }

    public boolean isEndpointUnusable() {
        if (statusCode == null) {
            return false;
        }

        return (statusCode >= 300 && statusCode < 400) || statusCode == 404 || statusCode == 405
            || statusCode == 410 || statusCode == 415;
    }

    public boolean isPayloadRejected() {
        return statusCode != null && (statusCode == 400 || statusCode == 409 || statusCode == 422);
    }
}
