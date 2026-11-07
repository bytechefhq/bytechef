/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import java.util.List;
import java.util.Set;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
record AiHubModeToolCallbacks(List<ToolCallback> toolCallbacks, Set<String> pinnedToolNames) {

    AiHubModeToolCallbacks {
        toolCallbacks = List.copyOf(toolCallbacks);
        pinnedToolNames = Set.copyOf(pinnedToolNames);
    }

    List<ToolCallback> pinned() {
        return toolCallbacks.stream()
            .filter(this::isPinned)
            .toList();
    }

    List<ToolCallback> unpinned() {
        return toolCallbacks.stream()
            .filter(toolCallback -> !isPinned(toolCallback))
            .toList();
    }

    private boolean isPinned(ToolCallback toolCallback) {
        return pinnedToolNames.contains(
            toolCallback.getToolDefinition()
                .name());
    }
}
