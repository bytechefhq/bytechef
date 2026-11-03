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

package com.bytechef.platform.notification.handler;

import org.jspecify.annotations.Nullable;

/**
 * The outcome of a test send of a notification.
 *
 * @param unsignedReason why a webhook test was sent without a signature, {@link #UNSIGNED_REASON_NO_SECRET} or
 *                       {@link #UNSIGNED_REASON_URL_CHANGED}; {@code null} when the test was signed or is not a webhook
 * @author Ivica Cardic
 */
public record NotificationTestResult(
    boolean success, @Nullable Integer statusCode, @Nullable String responseBody, @Nullable String error,
    boolean signed, @Nullable String unsignedReason) {

    public static final String UNSIGNED_REASON_NO_SECRET = "NO_SECRET";
    public static final String UNSIGNED_REASON_URL_CHANGED = "URL_CHANGED";

    public NotificationTestResult(
        boolean success, @Nullable Integer statusCode, @Nullable String responseBody, @Nullable String error,
        boolean signed) {

        this(success, statusCode, responseBody, error, signed, null);
    }

    public static NotificationTestResult sent(boolean signed) {
        return new NotificationTestResult(true, null, null, null, signed);
    }

    public static NotificationTestResult failed(String error) {
        return new NotificationTestResult(false, null, null, error, false);
    }

    public NotificationTestResult withUnsignedReason(@Nullable String unsignedReason) {
        return new NotificationTestResult(success, statusCode, responseBody, error, signed, unsignedReason);
    }
}
