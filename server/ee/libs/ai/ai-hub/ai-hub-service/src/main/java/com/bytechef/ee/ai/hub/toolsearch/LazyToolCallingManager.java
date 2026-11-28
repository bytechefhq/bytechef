/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.commons.util.MemoizationUtils;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class LazyToolCallingManager implements ToolCallingManager {

    private final Supplier<ToolCallingManager> delegateSupplier;

    LazyToolCallingManager(Supplier<ToolCallingManager> delegateSupplier) {
        this.delegateSupplier = MemoizationUtils.memoize(delegateSupplier);
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegateSupplier.get()
            .resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        return delegateSupplier.get()
            .executeToolCalls(prompt, chatResponse);
    }
}
