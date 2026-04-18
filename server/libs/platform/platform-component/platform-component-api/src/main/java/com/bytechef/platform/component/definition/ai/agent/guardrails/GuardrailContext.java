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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Call-site context passed to every guardrail function. All {@link Parameters} fields are guaranteed non-null — the
 * compact constructor substitutes an empty {@link Parameters} for any {@code null} argument so guardrail functions can
 * read parameters without scattering null-guards. Access the shared LLM client via {@link #chatClient()} which returns
 * an {@link Optional}; rule-based guardrails do not read it, LLM-based guardrails must {@code orElseThrow} a
 * {@code MissingModelChildException}.
 *
 * <p>
 * The record component stores a nullable raw {@link ChatClient}; the {@link #chatClient()} accessor wraps it in an
 * {@link Optional} at the boundary. This follows Effective Java Item 55 (Optional is for return values, not fields) —
 * storing an Optional as a record component would make the serialized shape of the record surprising under JSON
 * round-trips and is discouraged in modern Java style.
 *
 * @param inputParameters      this guardrail's own parameters; never {@code null}
 * @param connectionParameters the connection parameters; never {@code null}
 * @param parentParameters     parameters of the enclosing cluster root (e.g. shared system message); never {@code null}
 *                             — an empty {@link Parameters} is substituted when no parent context applies (e.g.
 *                             sanitizers or direct test construction)
 * @param extensions           nested cluster elements keyed by type; never {@code null}
 * @param componentConnections workflow connections keyed by node name; never {@code null}
 * @param rawChatClient        shared LLM client resolved once by the parent from its MODEL child; {@code null} when no
 *                             MODEL is wired. Callers should use {@link #chatClient()} rather than this raw accessor.
 * @author Ivica Cardic
 */
public record GuardrailContext(
    Parameters inputParameters,
    Parameters connectionParameters,
    Parameters parentParameters,
    Parameters extensions,
    Map<String, ComponentConnection> componentConnections,
    ChatClient rawChatClient) {

    public GuardrailContext {
        // Normalize nullable Parameters fields so guardrail functions never have to null-guard before reading. Empty
        // Parameters behave like "no value set" for every lookup, so callers get the same effective semantics without
        // scattered `context.xParameters() == null` checks.
        inputParameters = inputParameters == null ? ParametersFactory.create(Map.of()) : inputParameters;
        connectionParameters = connectionParameters == null ? ParametersFactory.create(Map.of()) : connectionParameters;
        parentParameters = parentParameters == null ? ParametersFactory.create(Map.of()) : parentParameters;
        extensions = extensions == null ? ParametersFactory.create(Map.of()) : extensions;

        // Defensive copy of componentConnections so a caller that retained a mutable HashMap reference cannot mutate
        // the map seen by the guardrail function after construction. This is the public SPI boundary; callers may
        // live in a different module.
        componentConnections = componentConnections == null ? Map.of() : Map.copyOf(componentConnections);
    }

    /**
     * Convenience constructor for rule-based guardrails and tests that do not need an LLM client. Equivalent to passing
     * {@code null} for {@code rawChatClient}. LLM-based guardrail functions that need a client should use the full
     * constructor and pass one in — calling {@link #chatClient()} on a context built via this overload returns
     * {@link Optional#empty()} and any LLM guardrail downstream must treat that as a
     * {@code MissingModelChildException}.
     */
    public GuardrailContext(
        Parameters inputParameters, Parameters connectionParameters, Parameters parentParameters,
        Parameters extensions, Map<String, ComponentConnection> componentConnections) {

        this(
            inputParameters, connectionParameters, parentParameters, extensions, componentConnections,
            (ChatClient) null);
    }

    /**
     * Optional-returning accessor so guardrail code can use {@link Optional#orElseThrow(java.util.function.Supplier)}
     * directly and the "no MODEL child wired" case stays explicit at the call site. Wraps the nullable record
     * component; any call to {@code chatClient()} materialises a fresh {@link Optional} so no state is shared.
     */
    public Optional<ChatClient> chatClient() {
        return Optional.ofNullable(rawChatClient);
    }

    /**
     * Explicit override of the record-auto-generated {@code rawChatClient()} accessor purely so {@link JsonIgnore} can
     * be attached — serialization frameworks then emit only {@link #chatClient()} (if they support Optional) or neither
     * (if they don't); never the raw field. Callers should prefer {@link #chatClient()}.
     */
    @JsonIgnore
    public ChatClient rawChatClient() {
        return rawChatClient;
    }
}
