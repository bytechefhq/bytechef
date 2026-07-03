/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link ListAiHubPersonalAgentsToolCallback}. The output shape is the cross-tool contract — the LLM
 * uses the {@code id} field as input to {@code openAiHubPersonalAgentTab} / {@code updateAiHubPersonalAgent} / {@code
 * deleteAiHubPersonalAgent}, so a rename or type change here would silently break those tools too.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ListAiHubPersonalAgentsToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testRejectsMissingWorkspaceContext() throws Exception {
        ListAiHubPersonalAgentsToolCallback callback =
            new ListAiHubPersonalAgentsToolCallback(mock(AiHubPersonalAgentService.class));

        JsonNode node = jsonMapper.readTree(callback.call("{}", null));

        // No tool context — the LLM must NOT be able to forge workspaceId via input arguments. Mirrors the
        // CreateAiHubPersonalAgent and CreateWorkflowChat security postures.
        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");
    }

    @Test
    void testReturnsAgentSummaries() throws Exception {
        AiHubPersonalAgentService service = mock(AiHubPersonalAgentService.class);

        AiHubPersonalAgent agent = new AiHubPersonalAgent(3L);

        agent.setId(42L);
        agent.setName("research-bot");
        agent.setTitle("Research Assistant");
        agent.setDescription("Helps with literature reviews");

        when(service.list(anyLong(), anyLong(), anyInt())).thenReturn(List.of(agent));

        ListAiHubPersonalAgentsToolCallback callback = new ListAiHubPersonalAgentsToolCallback(service);

        JsonNode node = jsonMapper.readTree(callback.call("{}", contextFor(7L, 3L)));

        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(1);
        // Cross-tool contract pinned: openAiHubPersonalAgentTab / updateAiHubPersonalAgent /
        // deleteAiHubPersonalAgent all read
        // these field names. A rename here would break all three.
        assertThat(node.get(0)
            .get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get(0)
            .get("name")
            .asText()).isEqualTo("research-bot");
        assertThat(node.get(0)
            .get("title")
            .asText()).isEqualTo("Research Assistant");
    }

    @Test
    void testReturnsEmptyArrayWhenNoAgents() throws Exception {
        AiHubPersonalAgentService service = mock(AiHubPersonalAgentService.class);

        when(service.list(anyLong(), anyLong(), anyInt())).thenReturn(List.of());

        ListAiHubPersonalAgentsToolCallback callback = new ListAiHubPersonalAgentsToolCallback(service);

        JsonNode node = jsonMapper.readTree(callback.call("{}", contextFor(7L, 3L)));

        // Empty array (not null, not an error) — the sidebar / LLM both handle "no agents" the same way as "lots
        // of agents", so the tool's response shape stays uniform.
        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(0);
    }

    private static ToolContext contextFor(long workspaceId, long userId) {
        return new ToolContext(Map.of(
            AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId,
            AiHubToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, userId));
    }
}
