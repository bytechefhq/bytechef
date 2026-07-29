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
import java.math.BigDecimal;

/**
 * Thrown when an asynchronous job submission is refused because the tenant's execution spend has reached the plan's
 * included monthly cost. Unlike the rate and concurrency limits this one does not clear on its own within seconds — it
 * resets with the billing period or a plan upgrade — but it stays in the {@link RateLimitExceededException} family so
 * the global REST exception handler maps it to HTTP 429 with a Retry-After alongside its siblings.
 *
 * @author Ivica Cardic
 */
public class JobCostLimitExceededException extends RateLimitExceededException {

    public JobCostLimitExceededException(BigDecimal includedMonthlyCostUsd) {
        super(
            "Included monthly execution cost of %s USD reached. Runs resume next billing period or after a plan upgrade."
                .formatted(includedMonthlyCostUsd));
    }
}
