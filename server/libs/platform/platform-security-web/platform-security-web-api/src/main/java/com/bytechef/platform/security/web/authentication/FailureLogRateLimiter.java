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

package com.bytechef.platform.security.web.authentication;

import java.time.Clock;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Ivica Cardic
 */
public final class FailureLogRateLimiter {

    private final Clock clock;
    private final long intervalMillis;
    private final AtomicLong nextLogMillis = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong suppressedFailures = new AtomicLong();

    public FailureLogRateLimiter(Clock clock, Duration interval) {
        this.clock = clock;
        this.intervalMillis = interval.toMillis();
    }

    public OptionalLong tryAcquire() {
        long nowMillis = clock.millis();
        long currentNextLogMillis = nextLogMillis.get();

        if (nowMillis < currentNextLogMillis ||
            !nextLogMillis.compareAndSet(currentNextLogMillis, nowMillis + intervalMillis)) {

            suppressedFailures.incrementAndGet();

            return OptionalLong.empty();
        }

        return OptionalLong.of(suppressedFailures.getAndSet(0));
    }
}
