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

package com.bytechef.ai.copilot.tool.ask;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * The whole "a specialist may ask the user a question" capability, as one optional seam.
 *
 * <p>
 * A seam rather than a direct dependency because the intelligent delegate callbacks and the catalog that builds them
 * are CE, while the thread-bound channel backing this is EE. An absent implementation is the pre-existing behaviour: no
 * ask tool is attached to any specialist, the delegation runs unwrapped, and every delegate always returns the
 * specialist's own summary.
 * </p>
 *
 * <p>
 * The tool and the channel binding live on ONE interface deliberately. They are useless apart and actively wrong when
 * only one is present: an attached tool with no channel bound makes every question a tool error, and a bound channel
 * with no tool attached is inert. One optional bean cannot be half-configured.
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
public interface SubAgentAskRelay {

    /**
     * The specialist-facing {@code askUserQuestion} tool to attach to a delegate's own {@code ChatClient}.
     *
     * <p>
     * Its contract differs from the main agent's tool of the same name in exactly one way: it writes the rendered
     * question envelope into the channel and returns a stop instruction, never the envelope itself. See the EE
     * implementation's javadoc for why returning the envelope there would be a real bug rather than a style choice.
     * </p>
     */
    ToolCallback askUserQuestionToolCallback();

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
