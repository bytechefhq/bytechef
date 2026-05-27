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

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

/**
 * Call-site context passed to every guardrail function. All {@link Parameters} accessors are non-null — null
 * constructor arguments are substituted with empty {@link Parameters}.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public final class GuardrailContext {

    private final Parameters inputParameters;
    private final Parameters connectionParameters;
    private final Parameters parentParameters;
    private final Parameters extensions;
    private final Map<String, ComponentConnection> componentConnections;
    private final @Nullable ChatClient chatClient;
    private final Context context;
    private final List<Message> conversationHistory;

    public GuardrailContext(
        Parameters inputParameters, Parameters connectionParameters, Parameters parentParameters,
        Parameters extensions, Map<String, ComponentConnection> componentConnections,
        @Nullable ChatClient chatClient, Context context) {

        this(inputParameters, connectionParameters, parentParameters, extensions, componentConnections, chatClient,
            context, List.of());
    }

    private GuardrailContext(
        Parameters inputParameters, Parameters connectionParameters, Parameters parentParameters,
        Parameters extensions, Map<String, ComponentConnection> componentConnections,
        @Nullable ChatClient chatClient, Context context, List<Message> conversationHistory) {

        this.inputParameters = inputParameters == null ? ParametersFactory.create(Map.of()) : inputParameters;
        this.connectionParameters = connectionParameters == null
            ? ParametersFactory.create(Map.of())
            : connectionParameters;
        this.parentParameters = parentParameters == null ? ParametersFactory.create(Map.of()) : parentParameters;
        this.extensions = extensions == null ? ParametersFactory.create(Map.of()) : extensions;
        this.componentConnections = componentConnections == null ? Map.of() : Map.copyOf(componentConnections);
        this.chatClient = chatClient;
        this.context = Objects.requireNonNull(context, "context");
        this.conversationHistory = conversationHistory == null ? List.of() : List.copyOf(conversationHistory);
    }

    public Parameters inputParameters() {
        return inputParameters;
    }

    public Parameters connectionParameters() {
        return connectionParameters;
    }

    public Parameters parentParameters() {
        return parentParameters;
    }

    public Parameters extensions() {
        return extensions;
    }

    public Map<String, ComponentConnection> componentConnections() {
        return componentConnections;
    }

    public Optional<ChatClient> chatClient() {
        return Optional.ofNullable(chatClient);
    }

    /**
     * Returns the configured {@link ChatClient} or throws {@link IllegalStateException} if the parent did not attach a
     * MODEL child.
     */
    public ChatClient requireChatClient(String guardrailName) {
        String safeName = guardrailName == null ? "unknown" : guardrailName;

        if (chatClient == null) {
            throw new IllegalStateException("Guardrail '" + safeName + "' requires a MODEL child");
        }

        return chatClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Variant of {@link #requireChatClient(String)} that throws a caller-supplied exception when the chat client is
     * missing.
     */
    public <X extends RuntimeException> ChatClient requireChatClient(Supplier<X> exceptionSupplier) {
        if (chatClient == null) {
            throw exceptionSupplier.get();
        }

        return chatClient;
    }

    public List<Message> conversationHistory() {
        return conversationHistory;
    }

    public GuardrailContext withConversationHistory(List<Message> history) {
        return new GuardrailContext(inputParameters, connectionParameters, parentParameters, extensions,
            componentConnections, chatClient, context, history);
    }

    public Context context() {
        return context;
    }

    public static final class Builder {

        private @Nullable Parameters inputParameters;
        private @Nullable Parameters connectionParameters;
        private @Nullable Parameters parentParameters;
        private @Nullable Parameters extensions;
        private @Nullable Map<String, ComponentConnection> componentConnections;
        private @Nullable ChatClient chatClient;
        private @Nullable Context context;

        private Builder() {
        }

        public Builder inputParameters(@Nullable Parameters value) {
            this.inputParameters = value;

            return this;
        }

        public Builder connectionParameters(@Nullable Parameters value) {
            this.connectionParameters = value;

            return this;
        }

        public Builder parentParameters(@Nullable Parameters value) {
            this.parentParameters = value;

            return this;
        }

        public Builder extensions(@Nullable Parameters value) {
            this.extensions = value;

            return this;
        }

        public Builder componentConnections(@Nullable Map<String, ComponentConnection> value) {
            this.componentConnections = value;

            return this;
        }

        public Builder chatClient(@Nullable ChatClient value) {
            this.chatClient = value;

            return this;
        }

        public Builder context(Context value) {
            this.context = value;

            return this;
        }

        public GuardrailContext build() {
            Objects.requireNonNull(context, "context");

            return new GuardrailContext(
                inputParameters, connectionParameters, parentParameters, extensions, componentConnections,
                chatClient, context);
        }
    }
}
