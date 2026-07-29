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

package com.bytechef.platform.workflow.execution.exception;

import com.bytechef.exception.RateLimitExceededException;

/**
 * Thrown when an asynchronous job submission would exceed the plan's sustained submissions-per-minute rate. Mirrors
 * {@link JobConcurrencyLimitExceededException} for the rate dimension; the token bucket refills continuously, so the
 * submission should be retried shortly. Mapped to HTTP 429 by the global REST exception handler via
 * {@link RateLimitExceededException}.
 *
 * @author Ivica Cardic
 */
public class JobRateLimitExceededException extends RateLimitExceededException {

    public JobRateLimitExceededException(int allowedPerMinute) {
        super("Async submission rate limit reached (allowed=%d/min). Retry shortly."
            .formatted(allowedPerMinute));
    }
}
