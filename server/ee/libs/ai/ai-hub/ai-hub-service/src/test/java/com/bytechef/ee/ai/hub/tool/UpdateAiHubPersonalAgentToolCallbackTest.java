/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link UpdateAiHubPersonalAgentToolCallback}. Pin two contracts:
 * <ul>
 * <li>The "no-op patch" rejection — the LLM must supply at least one of (title, description, instructions). A patch
 * with all-null fields would otherwise silently succeed and confuse the user.</li>
 * <li>Null-passthrough in the service call — null fields stay null all the way to the service so its "leave existing
 * value" semantics fire correctly.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class UpdateAiHubPersonalAgentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testRejectsAllNullPatch() throws Exception {
        UpdateAiHubPersonalAgentToolCallback callback = new UpdateAiHubPersonalAgentToolCallback(
            mock(AiHubPersonalAgentService.class));

        JsonNode node = jsonMapper.readTree(callback.call(
            "{\"aiHubPersonalAgentId\":42}", contextFor(7L, 3L)));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("At least one");
    }

    @Test
    void testPassesNullsThroughForUntouchedFields() throws Exception {
        AiHubPersonalAgentService service = mock(AiHubPersonalAgentService.class);

        AiHubPersonalAgent agent = new AiHubPersonalAgent(3L);

        agent.setId(42L);
        agent.setName("research-bot");
        agent.setTitle("New Title");

        when(service.update(
            anyLong(), anyLong(), anyLong(), eq("New Title"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(agent);

        UpdateAiHubPersonalAgentToolCallback callback =
            new UpdateAiHubPersonalAgentToolCallback(service);

        JsonNode node = jsonMapper.readTree(callback.call(
            "{\"aiHubPersonalAgentId\":42,\"title\":\"New Title\"}", contextFor(7L, 3L)));

        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("title")
            .asText()).isEqualTo("New Title");
        // The service receives null for description + instructions, NOT empty string. The "leave value untouched"
        // semantic depends on null-passthrough; if the tool defaulted nulls to empty strings, every patch would
        // overwrite description and instructions with "" instead of preserving them.
        verify(service).update(eq(42L), eq(7L), eq(3L), eq("New Title"), isNull(), isNull(), isNull(), isNull());
    }

    private static ToolContext contextFor(long workspaceId, long userId) {
        return new ToolContext(Map.of(
            AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId,
            AiHubToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, userId));
    }
}
