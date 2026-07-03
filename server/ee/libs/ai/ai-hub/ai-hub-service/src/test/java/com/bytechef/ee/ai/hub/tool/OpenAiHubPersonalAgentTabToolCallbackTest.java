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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskKind;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link OpenAiHubPersonalAgentTabToolCallback}. The tool's job is "resolve the agent, create-or-restore
 * its task, return threadId+taskId for the client subscriber to navigate". Tests pin both legs: ownership refusal when
 * the agent doesn't belong to the requesting user, and the success-path shape that {@code AiHubRuntimeProvider}'s
 * tool-result handler reads.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenAiHubPersonalAgentTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testRejectsMissingAiHubPersonalAgentId() throws Exception {
        OpenAiHubPersonalAgentTabToolCallback callback = new OpenAiHubPersonalAgentTabToolCallback(
            mock(AiHubPersonalAgentService.class), mock(AiHubTaskService.class));

        JsonNode node = jsonMapper.readTree(callback.call("{}", contextFor(7L, 3L)));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("aiHubPersonalAgentId");
    }

    @Test
    void testRejectsAgentInDifferentWorkspace() throws Exception {
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);

        // Agent owned by someone else / different workspace — the service's findOwned returns empty. The tool
        // surfaces a clear "not found" error rather than silently creating a task against a foreign
        // agent. Defense-in-depth above the LLM hallucination layer.
        when(aiHubPersonalAgentService.findOwned(anyLong(), anyLong(), anyLong())).thenReturn(Optional.empty());

        OpenAiHubPersonalAgentTabToolCallback callback = new OpenAiHubPersonalAgentTabToolCallback(
            aiHubPersonalAgentService, mock(AiHubTaskService.class));

        JsonNode node = jsonMapper.readTree(callback.call(
            "{\"aiHubPersonalAgentId\":42}", contextFor(7L, 3L)));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("not found");
    }

    @Test
    void testReturnsThreadIdAndTaskIdOnSuccess() throws Exception {
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        AiHubPersonalAgent agent = new AiHubPersonalAgent(3L);

        agent.setId(42L);
        agent.setName("research-bot");
        agent.setTitle("Research Assistant");

        when(aiHubPersonalAgentService.findOwned(eq(42L), eq(7L), eq(3L))).thenReturn(Optional.of(agent));

        AiHubTask task = new AiHubTask(3L);

        task.setId(101L);
        task.setThreadId("00000000-0000-0000-0000-000000000042");
        task.setKind(AiHubTaskKind.PERSONAL_AGENT);
        task.setTitle("Research Assistant");

        when(taskService.createAiHubPersonalAgentChat(anyLong(), anyLong(), anyInt(), anyLong(),
            anyString()))
                .thenReturn(task);

        OpenAiHubPersonalAgentTabToolCallback callback = new OpenAiHubPersonalAgentTabToolCallback(
            aiHubPersonalAgentService, taskService);

        JsonNode node = jsonMapper.readTree(callback.call(
            "{\"aiHubPersonalAgentId\":42}", contextFor(7L, 3L)));

        // The output shape (opened, threadId, taskId, title) is the contract
        // AiHubRuntimeProvider's tool-result handler reads. Pin it explicitly.
        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("threadId")
            .asText()).isEqualTo("00000000-0000-0000-0000-000000000042");
        assertThat(node.get("taskId")
            .asLong()).isEqualTo(101L);
        assertThat(node.get("title")
            .asText()).isEqualTo("Research Assistant");

        // The tool falls back to agent.title when present, agent.name when not — this user has a title set.
        verify(taskService).createAiHubPersonalAgentChat(
            eq(7L), eq(3L), anyInt(), eq(42L), eq("Research Assistant"));
    }

    @Test
    void testFallsBackToAgentNameAsTitleWhenTitleNull() throws Exception {
        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        AiHubPersonalAgent agent = new AiHubPersonalAgent(3L);

        agent.setId(42L);
        agent.setName("research-bot");
        // Title intentionally not set — the tool should fall back to the slug-style name.

        when(aiHubPersonalAgentService.findOwned(eq(42L), eq(7L), eq(3L))).thenReturn(Optional.of(agent));

        AiHubTask task = new AiHubTask(3L);

        task.setId(101L);
        task.setThreadId("00000000-0000-0000-0000-000000000042");
        task.setTitle("research-bot");

        when(taskService.createAiHubPersonalAgentChat(anyLong(), anyLong(), anyInt(), anyLong(),
            anyString()))
                .thenReturn(task);

        OpenAiHubPersonalAgentTabToolCallback callback = new OpenAiHubPersonalAgentTabToolCallback(
            aiHubPersonalAgentService, taskService);

        callback.call("{\"aiHubPersonalAgentId\":42}", contextFor(7L, 3L));

        // Without a title, the slug fills in — the task row needs SOMETHING legible to render in the
        // sidebar, and "Untitled Agent" would be confusing when the user named their agent.
        verify(taskService).createAiHubPersonalAgentChat(
            eq(7L), eq(3L), anyInt(), eq(42L), eq("research-bot"));
    }

    private static ToolContext contextFor(long workspaceId, long userId) {
        return new ToolContext(Map.of(
            AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId,
            AiHubToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, userId));
    }
}
