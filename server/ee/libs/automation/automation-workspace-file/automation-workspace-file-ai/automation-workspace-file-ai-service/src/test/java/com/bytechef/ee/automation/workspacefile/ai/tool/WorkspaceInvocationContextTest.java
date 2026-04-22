/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceInvocationContextTest {

    @Test
    void testRoundTripViaToolContext() {
        WorkspaceInvocationContext original = new WorkspaceInvocationContext(42L, (short) 3, "hello");

        ToolContext toolContext = new ToolContext(original.toToolContext());

        WorkspaceInvocationContext restored = WorkspaceInvocationContext.fromToolContext(toolContext);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void testFromEmptyToolContextReturnsNull() {
        assertThat(WorkspaceInvocationContext.fromToolContext(null)).isNull();
        assertThat(WorkspaceInvocationContext.fromToolContext(new ToolContext(Map.of()))).isNull();
    }

    @Test
    void testFromToolContextCoercesNumbers() {
        Map<String, Object> map = Map.of(
            AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_WORKSPACE_ID_KEY, (Integer) 7,
            AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (Integer) 1);

        WorkspaceInvocationContext context = WorkspaceInvocationContext.fromToolContext(new ToolContext(map));

        assertThat(context).isNotNull();
        assertThat(context.workspaceId()).isEqualTo(7L);
        assertThat(context.sourceOrdinal()).isEqualTo((short) 1);
        assertThat(context.lastUserPrompt()).isNull();
    }
}
