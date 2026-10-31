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

package com.bytechef.platform.component.runner.remote;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class Deadline {

    private final long deadlineNanos;
    private final Duration budget;

    private Deadline(long deadlineNanos, Duration budget) {
        this.deadlineNanos = deadlineNanos;
        this.budget = budget;
    }

    public static Deadline after(Duration budget) {
        return new Deadline(System.nanoTime() + budget.toNanos(), budget);
    }

    public void check(String runnerType, String stage) {
        if (isExpired()) {
            throw new IllegalStateException(
                "The %s task runner timed out after %s while %s".formatted(runnerType, budget, stage));
        }
    }

    public Deadline earliest(Deadline other) {
        return other.deadlineNanos - deadlineNanos < 0 ? other : this;
    }

    public Duration getBudget() {
        return budget;
    }

    public boolean isExpired() {
        return System.nanoTime() - deadlineNanos >= 0;
    }

    public Duration remaining() {
        return Duration.ofMillis(remainingMillis());
    }

    public long remainingMillis() {
        long remainingNanos = deadlineNanos - System.nanoTime();

        return remainingNanos > 0 ? TimeUnit.NANOSECONDS.toMillis(remainingNanos) : 0;
    }
}
