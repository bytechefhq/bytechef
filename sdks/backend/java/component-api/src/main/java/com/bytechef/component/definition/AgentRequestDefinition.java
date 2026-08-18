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

package com.bytechef.component.definition;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Optional;

/**
 * Marks a trigger as an agent request and states where the agent channel contract fields live within that trigger's own
 * output. Each path defaults to the contract name it binds, so a trigger whose output already is the contract declares
 * the descriptor and nothing else. A path may be dotted, addressing a nested field of the trigger's output (Telegram's
 * {@code "message.chat.id"}).
 *
 * @author Ivica Cardic
 */
public interface AgentRequestDefinition {

    /**
     * Returns the path, within the trigger's output, at which the conversation identifier is found.
     *
     * @return the conversation identifier path, defaulting to {@link AgentChannelDefinition#CONVERSATION_ID}
     */
    String getConversationIdPath();

    /**
     * Returns the path, within the trigger's output, at which the incoming message text is found.
     *
     * @return the message path, defaulting to {@link AgentChannelDefinition#MESSAGE}
     */
    String getMessagePath();

    /**
     * Returns the path, within the trigger's output, at which the incoming attachments are found.
     *
     * @return an {@link Optional} containing the attachments path, or an empty {@link Optional} when the channel
     *         carries no attachments
     */
    Optional<String> getAttachmentsPath();

    /**
     * Returns why this descriptor's paths must not be checked in full against the trigger's declared output schema.
     * <p>
     * When a trigger declares a static object output schema, every segment of every path is checked against it, so a
     * mistyped nested field fails the component's own test instead of surfacing at run time as an expression that
     * resolves to nothing. That check assumes the declared schema describes the real payload. Where it demonstrably
     * does NOT — a schema written from documentation that the service's actual webhook body contradicts — the
     * declaration says so here, at the site where the disagreement lives, and validation falls back to checking each
     * path's first segment only.
     * <p>
     * Deliberately a stated reason rather than a boolean: an unexplained opt-out is indistinguishable from a typo
     * somebody silenced.
     *
     * @return an {@link Optional} containing the reason, or an empty {@link Optional} when the paths are checked in
     *         full
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<String> getUnverifiedPathsReason();
}
