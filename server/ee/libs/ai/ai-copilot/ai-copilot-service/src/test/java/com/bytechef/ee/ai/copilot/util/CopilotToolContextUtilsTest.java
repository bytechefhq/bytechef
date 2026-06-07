/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.state.State;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class CopilotToolContextUtilsTest {

    @Test
    void testToToolContextCopiesAllowedComponentNames() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", "wf-1");
        stateMap.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack", "logger"));

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext)
            .containsEntry(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack", "logger"));
    }

    @Test
    void testToToolContextWithoutAllowedComponentNamesIsEmpty() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", "wf-1");

        Map<String, Object> toolContext = CopilotToolContextUtils.toToolContext(new State(stateMap));

        assertThat(toolContext).isEmpty();
    }

    @Test
    void testToToolContextWithNullStateIsEmpty() {
        assertThat(CopilotToolContextUtils.toToolContext(null)).isEmpty();
    }
}
