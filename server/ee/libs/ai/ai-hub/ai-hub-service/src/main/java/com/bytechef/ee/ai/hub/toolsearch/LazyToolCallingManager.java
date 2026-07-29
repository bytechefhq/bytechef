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
 * A {@link ToolCallingManager} that defers building its real delegate until the first tool-related call.
 *
 * <p>
 * The delegate ({@code DefaultToolCallingManager} + {@code StaticToolCallbackResolver}) is expensive to build because
 * the resolver materialises the full tool-search catalog (every tool-typed cluster element across every component),
 * which forces the whole component definition catalog to load. Constructing that eagerly at Spring startup — as a side
 * effect of wiring the tool-search advisor beans — defeats the build-time component index that keeps boot lazy.
 * Wrapping the delegate in this lazy manager lets the advisor be constructed at startup without touching the catalog;
 * the first AI Hub chat turn (the first {@code resolveToolDefinitions}/{@code executeToolCalls}) pays the one-time
 * catalog load, and every call afterwards hits the memoised delegate.
 * </p>
 *
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
