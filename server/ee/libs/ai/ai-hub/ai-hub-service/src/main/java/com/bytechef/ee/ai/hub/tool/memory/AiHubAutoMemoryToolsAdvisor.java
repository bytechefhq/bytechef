/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool.memory;

import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.component.ai.agent.utils.cluster.AutoMemoryResourceResolver;
import com.bytechef.component.ai.agent.utils.cluster.ServiceBackedAutoMemoryDirectoryOps;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.ai.agent.memory.AutoMemoryTools;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubAutoMemoryToolsAdvisor implements BaseChatMemoryAdvisor {

    static final String MEMORY_VIEW_TOOL_NAME = "MemoryView";

    private final AiAutoMemoryService aiAutoMemoryService;
    private final int order;
    private final String memorySystemPrompt;
    private final boolean readOnly;
    private final UnaryOperator<ToolCallback> toolCallbackWrapper;

    private AiHubAutoMemoryToolsAdvisor(Builder builder, String memorySystemPrompt) {
        this.aiAutoMemoryService = builder.aiAutoMemoryService;
        this.order = builder.order;
        this.memorySystemPrompt = memorySystemPrompt;
        this.readOnly = builder.readOnly;
        this.toolCallbackWrapper = builder.toolCallbackWrapper;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Prompt prompt = chatClientRequest.prompt();

        if (!(prompt.getOptions() instanceof ToolCallingChatOptions toolOptions)) {
            return chatClientRequest;
        }

        AiAutoMemoryOwner owner = resolveOwner(toolOptions.getToolContext());

        if (owner == null) {
            return chatClientRequest;
        }

        UnaryOperator<ToolCallback> toolCallbackDecorator = toolCallbackDecorator(chatClientRequest);

        List<ToolCallback> memoryToolCallbacks = memoryToolCallbacks(owner)
            .stream()
            .map(toolCallback -> toolCallbackDecorator.apply(toolCallbackWrapper.apply(toolCallback)))
            .toList();

        Prompt augmentedPrompt = prompt.augmentSystemMessage(
            prompt.getSystemMessage()
                .getText() + System.lineSeparator() + System.lineSeparator() + memorySystemPrompt);

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

    private List<ToolCallback> memoryToolCallbacks(AiAutoMemoryOwner owner) {
        AutoMemoryTools autoMemoryTools = new AutoMemoryTools(
            new AutoMemoryResourceResolver(aiAutoMemoryService, owner),
            new ServiceBackedAutoMemoryDirectoryOps(aiAutoMemoryService, owner));

        MethodToolCallbackProvider methodToolCallbackProvider = MethodToolCallbackProvider.builder()
            .toolObjects(autoMemoryTools)
            .build();

        List<ToolCallback> toolCallbacks = Arrays.asList(methodToolCallbackProvider.getToolCallbacks());

        if (!readOnly) {
            return toolCallbacks;
        }

        return toolCallbacks.stream()
            .filter(toolCallback -> MEMORY_VIEW_TOOL_NAME.equals(toolCallback.getToolDefinition()
                .name()))
            .toList();
    }

    @SuppressWarnings("unchecked")
    private static UnaryOperator<ToolCallback> toolCallbackDecorator(ChatClientRequest chatClientRequest) {
        Object toolCallbackDecorator = chatClientRequest.context()
            .get(SpringAIAgent.TOOL_CALLBACK_DECORATOR_KEY);

        if (toolCallbackDecorator instanceof UnaryOperator<?> unaryOperator) {
            return (UnaryOperator<ToolCallback>) unaryOperator;
        }

        return UnaryOperator.identity();
    }

    private static @Nullable AiAutoMemoryOwner resolveOwner(@Nullable Map<String, Object> toolContextMap) {
        if (toolContextMap == null || toolContextMap.isEmpty()) {
            return null;
        }

        AiHubToolInvocationContext context = AiHubToolInvocationContext.fromToolContext(
            new ToolContext(toolContextMap));

        if (context == null || context.workspaceId() == null || context.userId() == null) {
            return null;
        }

        Environment environment = Environment.values()[AiHubToolInvocationContext.resolveEnvironmentOrDefault(context)];

        return new AiAutoMemoryOwner(
            context.workspaceId(), AiAutoMemoryPrincipalType.USER, context.userId(), environment);
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

        private AiAutoMemoryService aiAutoMemoryService;
        private int order = HIGHEST_PRECEDENCE + 200;
        private Resource memorySystemPrompt;
        private boolean readOnly;
        private UnaryOperator<ToolCallback> toolCallbackWrapper = UnaryOperator.identity();

        private Builder() {
        }

        public Builder order(int order) {
            this.order = order;

            return this;
        }

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder aiAutoMemoryService(AiAutoMemoryService aiAutoMemoryService) {
            this.aiAutoMemoryService = aiAutoMemoryService;

            return this;
        }

        public Builder memorySystemPrompt(Resource memorySystemPrompt) {
            Assert.notNull(memorySystemPrompt, "Memory system prompt must not be null");

            this.memorySystemPrompt = memorySystemPrompt;

            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;

            return this;
        }

        public Builder toolCallbackWrapper(UnaryOperator<ToolCallback> toolCallbackWrapper) {
            Assert.notNull(toolCallbackWrapper, "toolCallbackWrapper must not be null");

            this.toolCallbackWrapper = toolCallbackWrapper;

            return this;
        }

        public AiHubAutoMemoryToolsAdvisor build() {
            Assert.notNull(aiAutoMemoryService, "aiAutoMemoryService must not be null");
            Assert.notNull(memorySystemPrompt, "Memory system prompt must not be null");

            PromptTemplate promptTemplate = PromptTemplate.builder()
                .resource(memorySystemPrompt)
                .build();

            return new AiHubAutoMemoryToolsAdvisor(this, promptTemplate.render());
        }
    }
}
