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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link CreateAiHubPersonalAgentToolCallback}. Pin three contracts:
 *
 * <ul>
 * <li>Workspace identity is read ONLY from {@link ToolContext} — never from input. An LLM that hallucinates a
 * {@code workspaceId} field cannot create agents in another workspace.</li>
 * <li>{@link ConflictException} from the service maps to a JSON {@code error} field — the LLM uses this signal to pick
 * a different name rather than crash the turn.</li>
 * <li>The output shape ({@code id, name, title, description}) is what {@link OpenAiHubPersonalAgentTabToolCallback}
 * consumes; a rename would silently break the create-then-open chain.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CreateAiHubPersonalAgentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testRejectsMissingName() throws Exception {
        CreateAiHubPersonalAgentToolCallback callback = new CreateAiHubPersonalAgentToolCallback(
            mock(AiHubPersonalAgentService.class));

        JsonNode node = jsonMapper.readTree(callback.call("{}", contextFor(7L, 3L)));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("name");
    }

    @Test
    void testRejectsMissingWorkspaceContext() throws Exception {
        CreateAiHubPersonalAgentToolCallback callback = new CreateAiHubPersonalAgentToolCallback(
            mock(AiHubPersonalAgentService.class));

        JsonNode node = jsonMapper.readTree(callback.call("{\"name\":\"research-bot\"}", null));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");
    }

    @Test
    void testReturnsAgentOnSuccess() throws Exception {
        AiHubPersonalAgentService service = mock(AiHubPersonalAgentService.class);

        AiHubPersonalAgent agent = new AiHubPersonalAgent(3L);

        agent.setId(42L);
        agent.setName("research-bot");
        agent.setTitle("Research Assistant");
        agent.setDescription("Lit reviews");

        when(service.create(
            anyLong(), anyLong(), anyInt(), anyString(), anyString(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(agent);

        CreateAiHubPersonalAgentToolCallback callback =
            new CreateAiHubPersonalAgentToolCallback(service);

        JsonNode node = jsonMapper.readTree(callback.call(
            "{\"name\":\"research-bot\",\"title\":\"Research Assistant\",\"description\":\"Lit reviews\","
                + "\"instructions\":\"Cite sources.\"}",
            contextFor(7L, 3L)));

        // Output contract pinned: openAiHubPersonalAgentTab reads `id`; sidebar refresh reads `name`. A rename
        // of
        // either field silently breaks the create-then-open flow.
        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("name")
            .asText()).isEqualTo("research-bot");
        assertThat(node.get("title")
            .asText()).isEqualTo("Research Assistant");
    }

    @Test
    void testConflictMapsToToolErrorRatherThanExceptionPropagation() throws Exception {
        AiHubPersonalAgentService service = mock(AiHubPersonalAgentService.class);

        when(service.create(
            anyLong(), anyLong(), anyInt(), eq("research-bot"), anyString(), anyString(), anyString(), isNull(),
            isNull()))
                .thenThrow(new ConflictException("Personal agent with name 'research-bot' already exists"));

        CreateAiHubPersonalAgentToolCallback callback =
            new CreateAiHubPersonalAgentToolCallback(service);

        JsonNode node = jsonMapper.readTree(callback.call(
            "{\"name\":\"research-bot\",\"title\":\"Research Assistant\",\"description\":\"Lit reviews\","
                + "\"instructions\":\"Cite sources.\"}",
            contextFor(7L, 3L)));

        // ConflictException → JSON error rather than runtime stack trace. The LLM uses this signal to retry
        // with a different name; if the exception propagated, the turn would crash and the user would see a
        // generic failure with no recovery path.
        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("already exists");
    }

    private static ToolContext contextFor(long workspaceId, long userId) {
        return new ToolContext(Map.of(
            AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId,
            AiHubToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, userId));
    }
}
