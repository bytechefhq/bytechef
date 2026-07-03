/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link DeleteAiHubPersonalAgentToolCallback}. The LLM-side concerns are simple — pass the id, see the
 * delete fire — but the safety net is the ownership check inside the service: a forged id pointing at someone else's
 * agent surfaces as {@code IllegalArgumentException} from the service, which the tool maps to a JSON error rather than
 * letting the exception escape.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class DeleteAiHubPersonalAgentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testReturnsDeletedTrueOnSuccess() throws Exception {
        AiHubPersonalAgentService service = mock(AiHubPersonalAgentService.class);

        DeleteAiHubPersonalAgentToolCallback callback =
            new DeleteAiHubPersonalAgentToolCallback(service);

        JsonNode node = jsonMapper.readTree(callback.call(
            "{\"aiHubPersonalAgentId\":42}", contextFor(7L, 3L)));

        assertThat(node.get("deleted")
            .asBoolean()).isTrue();
        assertThat(node.get("aiHubPersonalAgentId")
            .asLong()).isEqualTo(42L);

        verify(service).delete(eq(42L), eq(7L), eq(3L));
    }

    @Test
    void testForeignAgentIdMapsServiceErrorToToolError() throws Exception {
        AiHubPersonalAgentService service = mock(AiHubPersonalAgentService.class);

        // findOwned returns empty for a foreign agent → service throws IllegalArgumentException ("not found").
        // The tool MUST surface this as a JSON error rather than letting the exception escape — the LLM's
        // tool-calling loop relies on getting a structured response back.
        doThrow(new IllegalArgumentException("Personal agent 42 not found for workspace 7 user 3"))
            .when(service)
            .delete(eq(42L), eq(7L), eq(3L));

        DeleteAiHubPersonalAgentToolCallback callback =
            new DeleteAiHubPersonalAgentToolCallback(service);

        JsonNode node = jsonMapper.readTree(callback.call(
            "{\"aiHubPersonalAgentId\":42}", contextFor(7L, 3L)));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("not found");
    }

    private static ToolContext contextFor(long workspaceId, long userId) {
        return new ToolContext(Map.of(
            AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId,
            AiHubToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, userId));
    }
}
