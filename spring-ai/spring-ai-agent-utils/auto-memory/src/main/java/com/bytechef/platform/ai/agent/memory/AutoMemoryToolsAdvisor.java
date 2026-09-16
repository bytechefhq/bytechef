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

package com.bytechef.platform.ai.agent.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Forked from {@code org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor} (commit 5548e80). Augments the system
 * message with a memory system prompt (and an optional consolidation reminder) and registers the
 * {@link AutoMemoryTools} tool callbacks. Unlike upstream, the builder takes a pre-constructed {@link AutoMemoryTools}
 * instance (whose storage backend is supplied by the caller) instead of a filesystem {@code memoriesRootDirectory}.
 */
public class AutoMemoryToolsAdvisor implements BaseChatMemoryAdvisor {

    private final int order;
    private final String memorySystemPrompt;
    private final List<ToolCallback> memoryToolCallbacks;
    private final BiPredicate<ChatClientRequest, Instant> memoryConsolidationTrigger;

    private AutoMemoryToolsAdvisor(
        int order, String memorySystemPrompt, List<ToolCallback> memoryToolCallbacks,
        BiPredicate<ChatClientRequest, Instant> memoryConsolidationTrigger) {

        this.order = order;
        this.memorySystemPrompt = memorySystemPrompt;
        this.memoryToolCallbacks = memoryToolCallbacks;
        this.memoryConsolidationTrigger = memoryConsolidationTrigger;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        if (!(chatClientRequest.prompt()
            .getOptions() instanceof ToolCallingChatOptions toolOptions)) {

            return chatClientRequest;
        }

        String consolidationReminder = memoryConsolidationTrigger.test(chatClientRequest, Instant.now())
            ? "<system-reminder>Consolidate the long-term memory by summarizing and removing redundant "
                + "information.</system-reminder>"
            : "";

        Prompt augmentedPrompt = chatClientRequest.prompt()
            .augmentSystemMessage(chatClientRequest.prompt()
                .getSystemMessage()
                .getText() + System.lineSeparator() + System.lineSeparator() + memorySystemPrompt
                + System.lineSeparator() + System.lineSeparator() + consolidationReminder);

        ToolCallingChatOptions toolOptionsCopy = toolOptions.mutate()
            .build();

        List<ToolCallback> existingToolCallbacks = toolOptionsCopy.getToolCallbacks();

        List<ToolCallback> toolCallbacks = existingToolCallbacks == null
            ? new ArrayList<>()
            : new ArrayList<>(existingToolCallbacks);

        Set<String> existingNames = toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toSet());

        memoryToolCallbacks.stream()
            .filter(toolCallback -> !existingNames.contains(toolCallback.getToolDefinition()
                .name()))
            .forEach(toolCallbacks::add);

        ToolCallingChatOptions mergedOptions = ((ToolCallingChatOptions.Builder<?>) toolOptionsCopy.mutate())
            .toolCallbacks(new ArrayList<>(toolCallbacks))
            .build();

        return chatClientRequest.mutate()
            .prompt(augmentedPrompt.mutate()
                .chatOptions(mergedOptions)
                .build())
            .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        // Before the default ToolCallingAdvisor which is at HIGHEST_PRECEDENCE + 300.
        // HIGHEST_PRECEDENCE is inherited from Ordered via the BaseChatMemoryAdvisor → Advisor → Ordered hierarchy.
        private int order = HIGHEST_PRECEDENCE + 200;
        private AutoMemoryTools autoMemoryTools;
        private Resource memorySystemPrompt;
        private BiPredicate<ChatClientRequest, Instant> memoryConsolidationTrigger = (request, instant) -> false;

        private Builder() {
        }

        public Builder order(int order) {
            this.order = order;

            return this;
        }

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder autoMemoryTools(AutoMemoryTools autoMemoryTools) {
            this.autoMemoryTools = autoMemoryTools;

            return this;
        }

        public Builder memorySystemPrompt(Resource memorySystemPrompt) {
            Assert.notNull(memorySystemPrompt, "Memory system prompt must not be null");

            this.memorySystemPrompt = memorySystemPrompt;

            return this;
        }

        public Builder memoryConsolidationTrigger(
            BiPredicate<ChatClientRequest, Instant> memoryConsolidationTrigger) {

            Assert.notNull(memoryConsolidationTrigger, "Memory consolidation trigger must not be null");

            this.memoryConsolidationTrigger = memoryConsolidationTrigger;

            return this;
        }

        public AutoMemoryToolsAdvisor build() {
            Assert.notNull(this.autoMemoryTools, "autoMemoryTools must not be null");
            Assert.notNull(this.memorySystemPrompt, "Memory system prompt must not be null");

            List<ToolCallback> memoryToolCallbacks = Arrays.asList(
                MethodToolCallbackProvider.builder()
                    .toolObjects(this.autoMemoryTools)
                    .build()
                    .getToolCallbacks());

            String memorySystemPromptText = PromptTemplate.builder()
                .resource(this.memorySystemPrompt)
                .build()
                .render();

            return new AutoMemoryToolsAdvisor(
                this.order, memorySystemPromptText, memoryToolCallbacks, this.memoryConsolidationTrigger);
        }
    }
}
