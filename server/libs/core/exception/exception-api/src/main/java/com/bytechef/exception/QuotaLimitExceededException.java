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

package com.bytechef.exception;

/**
 * Marker supertype for rejections caused by a plan quota on stored resources (workspaces, members, asset storage).
 * Unlike {@link RateLimitExceededException} the operation is NOT retryable after a delay — the tenant is at a hard
 * capacity ceiling and must delete resources or upgrade the plan first — so the REST layer maps this family to HTTP 403
 * without a {@code Retry-After} hint. Subclasses (or plain instances) should carry a message naming the quota that was
 * hit.
 *
 * @author Ivica Cardic
 */
public class QuotaLimitExceededException extends RuntimeException {

    public QuotaLimitExceededException(String message) {
        super(message);
    }
}
