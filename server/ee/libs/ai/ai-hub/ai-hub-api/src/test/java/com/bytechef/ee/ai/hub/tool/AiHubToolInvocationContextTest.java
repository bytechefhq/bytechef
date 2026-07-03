/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

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
        AiHubToolInvocationContext original =
            new AiHubToolInvocationContext(42L, null, (short) 3, "hello", null);

        ToolContext toolContext = new ToolContext(original.toToolContext());

        AiHubToolInvocationContext restored = AiHubToolInvocationContext.fromToolContext(toolContext);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void testFromEmptyToolContextReturnsNull() {
        assertThat(AiHubToolInvocationContext.fromToolContext(null)).isNull();
        assertThat(AiHubToolInvocationContext.fromToolContext(new ToolContext(Map.of()))).isNull();
    }

    @Test
    void testFromToolContextCoercesNumbers() {
        Map<String, Object> map = Map.of(
            AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, (Integer) 7,
            AiHubToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (Integer) 1);

        AiHubToolInvocationContext context =
            AiHubToolInvocationContext.fromToolContext(new ToolContext(map));

        assertThat(context).isNotNull();
        assertThat(context.workspaceId()).isEqualTo(7L);
        assertThat(context.sourceOrdinal()).isEqualTo((short) 1);
        assertThat(context.lastUserPrompt()).isNull();
    }
}
