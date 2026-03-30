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

package com.bytechef.platform.ai.util;

/**
 * Thread-local holder for accumulating LLM token usage from chat responses. Allows token metadata to be passed across
 * layers without changing method signatures.
 *
 * <p>
 * This holder uses an opt-in accumulation model: call {@link #start()} to enable tracking on the current thread, then
 * {@link #capture(int, int)} to accumulate counts, and {@link #getAndClear()} to read the totals and reset all state.
 * Calls to {@code capture()} are no-ops unless tracking has been started, preventing unintended accumulation across
 * unrelated requests on pooled threads.
 * </p>
 *
 * <p>
 * <strong>Thread safety:</strong> All methods must be called on the same thread. The data is not visible across thread
 * boundaries.
 * </p>
 *
 * @author Ivica Cardic
 */
public class TokenUsageHolder {

    private static final ThreadLocal<Boolean> TRACKING_ENABLED = new ThreadLocal<>();
    private static final ThreadLocal<TokenUsage> TOKEN_USAGE = new ThreadLocal<>();

    private TokenUsageHolder() {
    }

    /**
     * Starts token usage tracking on the current thread. Must be called before {@link #capture(int, int)} to enable
     * accumulation. Resets any previously accumulated counts.
     */
    public static void start() {
        TRACKING_ENABLED.set(Boolean.TRUE);

        TOKEN_USAGE.remove();
    }

    /**
     * Accumulates token usage for the current thread. This is a no-op unless {@link #start()} has been called on the
     * current thread, preventing unintended accumulation across unrelated requests on pooled threads.
     *
     * @param promptTokens     the number of tokens used in the prompt (input)
     * @param completionTokens the number of tokens used in the completion (output)
     */
    public static void capture(int promptTokens, int completionTokens) {
        if (!Boolean.TRUE.equals(TRACKING_ENABLED.get())) {
            return;
        }

        TokenUsage existing = TOKEN_USAGE.get();

        if (existing != null) {
            TOKEN_USAGE.set(
                new TokenUsage(existing.promptTokens() + promptTokens, existing.completionTokens() + completionTokens));
        } else {
            TOKEN_USAGE.set(new TokenUsage(promptTokens, completionTokens));
        }
    }

    /**
     * Returns the accumulated token usage and clears all tracking state including the enabled flag.
     *
     * @return the accumulated token usage, or {@link TokenUsage#EMPTY} if no usage was captured
     */
    public static TokenUsage getAndClear() {
        TokenUsage usage = TOKEN_USAGE.get();

        TOKEN_USAGE.remove();
        TRACKING_ENABLED.remove();

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
