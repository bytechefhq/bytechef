/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.util;

import com.agui.core.state.State;
import com.bytechef.platform.ai.tool.TaskTools;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Maps per-run agent {@link State} entries into the Spring AI {@code ToolContext} map handed to copilot tools.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class CopilotToolContextUtils {

    private CopilotToolContextUtils() {
    }

    public static Map<String, Object> toToolContext(@Nullable State state) {
        if (state == null) {
            return Map.of();
        }

        Object allowedComponentNames = state.get(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY);

        if (allowedComponentNames == null) {
            return Map.of();
        }

        return Map.of(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
    }
}
