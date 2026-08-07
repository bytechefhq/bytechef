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

package com.bytechef.component.ai.llm.tool;

import org.springframework.ai.tool.ToolCallback;

/**
 * A {@link ToolCallback} that wraps another and reports the wrapped callback's tool definition as its own. Such a
 * wrapper is indistinguishable from the tool it wraps when callbacks are looked up by name, so a caller that needs the
 * underlying tool — rather than the behaviour layered over it — must be able to unwrap.
 *
 * <p>
 * The approval gate is the motivating case: on resume, the human has already approved this exact invocation, so the
 * agent must execute the raw tool instead of raising a second approval request. The agent action lives in a different
 * module than the gate and must not import it, so it unwraps through this interface instead.
 *
 * @author Ivica Cardic
 */
public interface DelegatingToolCallback extends ToolCallback {

    /**
     * Returns the wrapped callback. Implementations wrapping another delegating callback are unwrapped repeatedly by
     * {@link #unwrap(ToolCallback)}.
     */
    ToolCallback getDelegate();

    /**
     * Unwraps to the innermost non-delegating callback, returning the argument unchanged when it does not delegate.
     */
    static ToolCallback unwrap(ToolCallback toolCallback) {
        ToolCallback unwrapped = toolCallback;

        while (unwrapped instanceof DelegatingToolCallback delegatingToolCallback) {
            unwrapped = delegatingToolCallback.getDelegate();
        }

        return unwrapped;
    }
}
