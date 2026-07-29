/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

/**
 * Resolves a tool callback by its pre-known name via a map lookup, without ever calling
 * {@link ToolCallback#getToolDefinition()}. Spring AI's {@code StaticToolCallbackResolver} builds its name index by
 * calling {@code getToolDefinition().name()} on every callback at construction — which, for a lazy
 * {@link ClusterElementToolCallback}, forces the input schema (and its component) to load. Because the tool-search
 * catalog already knows each tool's name cheaply when it builds the callback map, this resolver keys off that map so a
 * component loads only when the model actually invokes a surfaced tool.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class MapToolCallbackResolver implements ToolCallbackResolver {

    private final Map<String, ToolCallback> toolCallbacks;

    MapToolCallbackResolver(Map<String, ToolCallback> toolCallbacks) {
        this.toolCallbacks = Map.copyOf(toolCallbacks);
    }

    @Override
    public @Nullable ToolCallback resolve(String name) {
        return toolCallbacks.get(name);
    }
}
