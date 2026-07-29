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
 * A webhook delivery that failed — transport error, interruption, or a non-2xx response. {@code statusCode} is null for
 * transport-level failures.
 *
 * @author Ivica Cardic
 */
public class WebhookDeliveryException extends RuntimeException {

    private final @Nullable Integer statusCode;

    public WebhookDeliveryException(String message, @Nullable Integer statusCode) {
        super(message);

        this.statusCode = statusCode;
    }

    public WebhookDeliveryException(String message, Throwable cause) {
        super(message, cause);

        this.statusCode = null;
    }

    public @Nullable Integer getStatusCode() {
        return statusCode;
    }
}
