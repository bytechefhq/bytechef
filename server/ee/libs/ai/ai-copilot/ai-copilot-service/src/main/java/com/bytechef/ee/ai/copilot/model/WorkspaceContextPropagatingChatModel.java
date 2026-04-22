/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.model;

import com.bytechef.ee.automation.workspacefile.ai.tool.AgUiToolContextWorkspaceContextProvider;
import com.bytechef.ee.automation.workspacefile.ai.tool.WorkspaceInvocationContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

/**
 * {@link ChatModel} decorator that snapshots the thread-bound {@link WorkspaceInvocationContext} onto the
 * {@link Prompt}'s {@link ToolCallingChatOptions#setToolContext(Map)} immediately before delegating, so the workspace
 * identifiers survive the Reactor thread hops that happen between the copilot agent and Spring AI's tool-calling
 * manager. Tool callbacks receive the values through {@code ToolContext} regardless of which thread eventually executes
 * them.
 *
 * <p>
 * The decorator is a no-op when no workspace context is bound (e.g. non-files copilot agents such as the workflow
 * editor); the tool callbacks gracefully tool-error in that situation.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkspaceContextPropagatingChatModel implements ChatModel, StreamingChatModel {

    private final ChatModel delegate;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceContextPropagatingChatModel(ChatModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return delegate.call(propagateContext(prompt));
    }

    @Override
    public @NonNull Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(propagateContext(prompt));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return delegate.getDefaultOptions();
    }

    private Prompt propagateContext(Prompt prompt) {
        WorkspaceInvocationContext context = AgUiToolContextWorkspaceContextProvider.currentContext();

        if (context == null) {
            return prompt;
        }

        Map<String, Object> contextMap = context.toToolContext();

        if (contextMap.isEmpty()) {
            return prompt;
        }

        ChatOptions options = prompt.getOptions();

        if (options instanceof ToolCallingChatOptions toolCallingChatOptions) {
            toolCallingChatOptions.setToolContext(
                ToolCallingChatOptions.mergeToolContext(contextMap, toolCallingChatOptions.getToolContext()));

            return prompt;
        }

        ToolCallingChatOptions augmentedOptions;

        if (options == null) {
            augmentedOptions = new DefaultToolCallingChatOptions();
        } else {
            ChatOptions copied = options.copy();

            if (copied instanceof ToolCallingChatOptions already) {
                augmentedOptions = already;
            } else {
                augmentedOptions = new DefaultToolCallingChatOptions();
            }
        }

        augmentedOptions.setToolContext(
            ToolCallingChatOptions.mergeToolContext(contextMap, augmentedOptions.getToolContext()));

        return new Prompt(prompt.getInstructions(), augmentedOptions);
    }
}
