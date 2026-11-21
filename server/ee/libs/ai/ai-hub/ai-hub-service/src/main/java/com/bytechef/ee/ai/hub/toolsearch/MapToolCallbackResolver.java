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
