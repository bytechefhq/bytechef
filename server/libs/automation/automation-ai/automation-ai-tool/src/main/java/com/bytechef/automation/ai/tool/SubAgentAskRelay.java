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

package com.bytechef.automation.ai.tool;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Carries a question raised inside a subagent delegation back out to the delegate {@code ToolCallback}, which returns
 * it as its own tool result so the parent agent's stream carries it to the client.
 *
 * <p>
 * A seam rather than a direct dependency because the delegate callbacks are CE while the thread-bound channel backing
 * this is EE. A {@code null} relay is the pre-existing behaviour: the delegation runs unwrapped and always returns the
 * specialist's own summary.
 * </p>
 *
 * <p>
 * {@link #runWithChannel(Supplier)} returns the pending question <b>alongside</b> the delegation's result rather than
 * exposing a separate {@code pending()} read afterwards. That is deliberate: a delegate {@code ToolCallback} is a
 * singleton serving every concurrent delegation, so stashing the question anywhere that outlives the call would let two
 * conversations race and surface one user's question in another's chat.
 * </p>
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface SubAgentAskRelay {

    /**
     * Runs {@code supplier} with a question channel bound, returning its result together with whatever question the
     * specialist raised while it ran.
     */
    <T> AskOutcome<T> runWithChannel(Supplier<T> supplier);

    /**
     * One delegation's outcome.
     *
     * @param result          whatever the delegated call returned
     * @param pendingQuestion the rendered question payload the specialist raised, or {@code null} if it raised none
     */
    record AskOutcome<T>(@Nullable T result, @Nullable String pendingQuestion) {
    }
}
