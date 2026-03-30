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
 * Thread-local holder for capturing LLM token usage from chat responses. Allows token metadata to be passed across
 * layers without changing method signatures.
 *
 * @author Ivica Cardic
 */
public class TokenUsageHolder {

    private static final ThreadLocal<int[]> TOKEN_USAGE = new ThreadLocal<>();

    private TokenUsageHolder() {
    }

    /**
     * Stores token usage for the current thread.
     *
     * @param promptTokens     the number of tokens used in the prompt (input)
     * @param completionTokens the number of tokens used in the completion (output)
     */
    public static void capture(int promptTokens, int completionTokens) {
        TOKEN_USAGE.set(new int[] {
            promptTokens, completionTokens
        });
    }

    /**
     * Returns the captured token usage and clears the stored value. The returned array contains
     * {@code [promptTokens, completionTokens]}.
     *
     * @return a two-element array with prompt and completion token counts, or {@code [0, 0]} if no usage was captured
     */
    public static int[] getAndClear() {
        int[] usage = TOKEN_USAGE.get();

        TOKEN_USAGE.remove();

        return usage != null ? usage : new int[] {
            0, 0
        };
    }

}
