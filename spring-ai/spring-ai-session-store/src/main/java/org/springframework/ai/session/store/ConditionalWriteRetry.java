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

package org.springframework.ai.session.store;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

/**
 * @author Ivica Cardic
 */
public final class ConditionalWriteRetry {

    public static final int DEFAULT_MAX_ATTEMPTS = 8;
    public static final long DEFAULT_BASE_DELAY_MILLIS = 15L;
    public static final long DEFAULT_MAX_DELAY_MILLIS = 500L;

    private final int maxAttempts;
    private final long baseDelayMillis;
    private final long maxDelayMillis;
    private final LongConsumer sleeper;

    public ConditionalWriteRetry(int maxAttempts, long baseDelayMillis, long maxDelayMillis, LongConsumer sleeper) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }

        if (baseDelayMillis < 1 || maxDelayMillis < baseDelayMillis) {
            throw new IllegalArgumentException("delays must satisfy 1 <= baseDelayMillis <= maxDelayMillis");
        }

        this.maxAttempts = maxAttempts;
        this.baseDelayMillis = baseDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper must not be null");
    }

    public static ConditionalWriteRetry defaults() {
        return new ConditionalWriteRetry(
            DEFAULT_MAX_ATTEMPTS, DEFAULT_BASE_DELAY_MILLIS, DEFAULT_MAX_DELAY_MILLIS,
            ConditionalWriteRetry::sleepMillis);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public void run(String operation, String sessionId, BooleanSupplier attempt) {
        call(operation, sessionId, () -> attempt.getAsBoolean() ? Optional.of(Boolean.TRUE) : Optional.empty());
    }

    public <T> T call(String operation, String sessionId, Supplier<Optional<T>> attempt) {
        for (int attemptNumber = 1; attemptNumber <= maxAttempts; attemptNumber++) {
            Optional<T> result = attempt.get();

            if (result.isPresent()) {
                return result.get();
            }

            if (attemptNumber < maxAttempts) {
                sleeper.accept(delayMillis(attemptNumber));
            }
        }

        throw new IllegalStateException(
            "Failed to " + operation + " for session " + sessionId + " after " + maxAttempts +
                " attempts because concurrent writers kept modifying the session");
    }

    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    long delayMillis(int attemptNumber) {
        int shift = Math.min(attemptNumber - 1, 20);

        long exponentialDelayMillis = Math.min(maxDelayMillis, baseDelayMillis << shift);

        long halfDelayMillis = exponentialDelayMillis / 2;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        return halfDelayMillis + random.nextLong(exponentialDelayMillis - halfDelayMillis + 1);
    }

    private static void sleepMillis(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(
                "Interrupted while waiting to retry a conditional session write", interruptedException);
        }
    }
}
