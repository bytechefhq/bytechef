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
 * Thrown when a job submission would exceed the plan's concurrent-execution limit. Mirrors the licence job-count limit
 * exception for the concurrency dimension; the submission should be retried once running executions drain. Mapped to
 * HTTP 429 by the global REST exception handler via {@link RateLimitExceededException}.
 *
 * @author Ivica Cardic
 */
public class JobConcurrencyLimitExceededException extends RateLimitExceededException {

    public JobConcurrencyLimitExceededException(int allowed) {
        super("Concurrent execution limit reached (allowed=%d). Retry when running executions finish."
            .formatted(allowed));
    }
}
