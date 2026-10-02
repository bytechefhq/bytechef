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

package com.bytechef.platform.ai.sensitivedata;

import java.time.Duration;

/**
 * @author Ivica Cardic
 */
public final class MatchDeadline {

    private static final int CHECK_INTERVAL = 4096;

    private final long deadlineNanos;

    private MatchDeadline(long deadlineNanos) {
        this.deadlineNanos = deadlineNanos;
    }

    public static MatchDeadline in(Duration budget) {
        return new MatchDeadline(System.nanoTime() + budget.toNanos());
    }

    public static MatchDeadline at(long deadlineNanos) {
        return new MatchDeadline(deadlineNanos);
    }

    public static MatchDeadline unbounded() {
        return new MatchDeadline(Long.MAX_VALUE);
    }

    public CharSequence bound(String text) {
        if (deadlineNanos == Long.MAX_VALUE) {
            return text;
        }

        return new BoundedCharSequence(text, deadlineNanos);
    }

    public boolean expired() {
        return System.nanoTime() >= deadlineNanos;
    }

    private static final class BoundedCharSequence implements CharSequence {

        private final String delegate;
        private final long deadlineNanos;

        private long calls;

        private BoundedCharSequence(String delegate, long deadlineNanos) {
            this.delegate = delegate;
            this.deadlineNanos = deadlineNanos;
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public char charAt(int index) {
            if ((++calls & (CHECK_INTERVAL - 1)) == 0 && System.nanoTime() >= deadlineNanos) {
                throw new DetectionTimeoutException(
                    "match exceeded its deadline after " + calls + " character reads over a " + delegate.length() +
                        "-character input");
            }

            return delegate.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return delegate.subSequence(start, end);
        }

        @Override
        public String toString() {
            return delegate;
        }
    }
}
