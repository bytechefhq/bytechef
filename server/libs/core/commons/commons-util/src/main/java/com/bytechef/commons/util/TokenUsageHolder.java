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

package com.bytechef.commons.util;

/**
 * Thread-local holder for accumulating LLM token usage from chat responses. Allows token metadata to be passed across
 * layers without changing method signatures.
 *
 * <p>
 * This holder uses an accumulation model: multiple calls to {@link #capture(int, int)} on the same thread add to the
 * running totals rather than overwriting previous values. Call {@link #getAndClear()} before the operation of interest
 * to reset the counters, and again afterward to read the accumulated totals.
 * </p>
 *
 * <p>
 * <strong>Thread safety:</strong> Both {@code capture()} and {@code getAndClear()} must be called on the same thread.
 * The data is not visible across thread boundaries.
 * </p>
 *
 * @author Ivica Cardic
 */
public class TokenUsageHolder {

    private static final ThreadLocal<TokenUsage> TOKEN_USAGE = new ThreadLocal<>();

    private TokenUsageHolder() {
    }

    /**
     * Accumulates token usage for the current thread. If token usage was already captured on this thread, the counts
     * are added to the existing totals.
     *
     * @param promptTokens     the number of tokens used in the prompt (input)
     * @param completionTokens the number of tokens used in the completion (output)
     */
    public static void capture(int promptTokens, int completionTokens) {
        TokenUsage existing = TOKEN_USAGE.get();

        if (existing != null) {
            TOKEN_USAGE.set(
                new TokenUsage(existing.promptTokens() + promptTokens, existing.completionTokens() + completionTokens));
        } else {
            TOKEN_USAGE.set(new TokenUsage(promptTokens, completionTokens));
        }
    }

    /**
     * Returns the accumulated token usage and clears the stored value.
     *
     * @return the accumulated token usage, or {@link TokenUsage#EMPTY} if no usage was captured
     */
    public static TokenUsage getAndClear() {
        TokenUsage usage = TOKEN_USAGE.get();

        TOKEN_USAGE.remove();

        return usage != null ? usage : TokenUsage.EMPTY;
    }

    /**
     * Immutable value object representing LLM token usage counts.
     *
     * @param promptTokens     the number of tokens used in the prompt (input)
     * @param completionTokens the number of tokens used in the completion (output)
     */
    public record TokenUsage(int promptTokens, int completionTokens) {

        public static final TokenUsage EMPTY = new TokenUsage(0, 0);
    }
}
