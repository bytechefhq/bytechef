/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpToolCallbackProvider;
import com.bytechef.ee.ai.hub.skill.AiHubSkillsToolProvider;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolBinding;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubTaskBindingToolCallbackResolverTest {

    @Test
    void testResolveReturnsEmptyWhenNoThreadId() {
        AiHubTaskBindingToolCallbackResolver resolver = newResolver(
            mock(AiHubTaskService.class), mock(AiHubTaskToolFacade.class),
            mock(ClusterElementDefinitionService.class), mock(ConnectionService.class));

        // Missing thread id (anonymous request, broken context propagation) — must NOT throw and must NOT
        // try to look up a task. Defensive: this is the most common "context-less" path during
        // initial agent boot or test wiring.
        AiHubToolInvocationContext bareContext = new AiHubToolInvocationContext(
            1L, 10L, (short) 0, "x", 0L, null);

        assertThat(resolver.resolve(bareContext)).isEmpty();
    }

    @Test
    void testResolveReturnsEmptyWhenTaskNotFound() {
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        when(taskService.findByThreadId("missing-thread")).thenReturn(Optional.empty());

        AiHubTaskBindingToolCallbackResolver resolver = newResolver(
            taskService, mock(AiHubTaskToolFacade.class),
            mock(ClusterElementDefinitionService.class), mock(ConnectionService.class));

        AiHubToolInvocationContext context = new AiHubToolInvocationContext(
            1L, 10L, (short) 0, "x", 0L, "missing-thread");

        // Race condition: thread id is in the agent context but the task row was deleted in between.
        // Pre-v1 of this class would NPE on task.get().getId(); regression-pin the empty-list return.
        assertThat(resolver.resolve(context)).isEmpty();
    }

    @Test
    void testResolveBuildsCallbackPerBindingWithPinnedConnectionAndParameters() {
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        AiHubTaskToolFacade taskToolFacade = mock(AiHubTaskToolFacade.class);
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ConnectionService connectionService = mock(ConnectionService.class);

        AiHubTask task = mock(AiHubTask.class);
        when(task.getId()).thenReturn(7L);
        when(taskService.findByThreadId("thread-1")).thenReturn(Optional.of(task));

        AiHubTaskToolBinding sendBinding = new AiHubTaskToolBinding(
            11L, 99L, 7L, "slack", 1, "sendMessage", 42L, 0,
            Map.of("channel", "#engineering"));
        AiHubTaskToolBinding postBinding = new AiHubTaskToolBinding(
            12L, 99L, 7L, "github", 1, "createIssue", 50L, 0,
            Map.of("repo", "acme/site"));

        when(taskToolFacade.listTaskTools(7L)).thenReturn(List.of(sendBinding, postBinding));

        ClusterElementDefinition slackDef = mock(ClusterElementDefinition.class);
        when(slackDef.getDescription()).thenReturn("Send a message to a Slack channel");
        when(slackDef.getTitle()).thenReturn("Slack: Send Message");
        when(slackDef.getProperties()).thenReturn(List.of());

        ClusterElementDefinition githubDef = mock(ClusterElementDefinition.class);
        when(githubDef.getDescription()).thenReturn("Open a new GitHub issue");
        when(githubDef.getTitle()).thenReturn("GitHub: Create Issue");
        when(githubDef.getProperties()).thenReturn(List.of());

        when(clusterElementDefinitionService.getClusterElementDefinition(eq("slack"), eq(1), eq("sendMessage")))
            .thenReturn(slackDef);
        when(clusterElementDefinitionService.getClusterElementDefinition(eq("github"), eq(1), eq("createIssue")))
            .thenReturn(githubDef);

        AiHubTaskBindingToolCallbackResolver resolver = newResolver(
            taskService, taskToolFacade, clusterElementDefinitionService, connectionService);

        AiHubToolInvocationContext context = new AiHubToolInvocationContext(
            1L, 10L, (short) 0, "x", 0L, "thread-1");

        List<ToolCallback> callbacks = resolver.resolve(context);

        // Both bindings must produce a callback with the LLM-visible name shape that the search-discovery
        // path also uses — keeping the chat agent's tool-name model consistent across discovery vs attach.
        assertThat(callbacks).hasSize(2);
        assertThat(callbacks.get(0)
            .getToolDefinition()
            .name()).isEqualTo("slack_sendMessage");
        assertThat(callbacks.get(1)
            .getToolDefinition()
            .name()).isEqualTo("github_createIssue");

        // Description format mirrors the search-discovery formatter ("title: description") so chat traces
        // look consistent regardless of which path attached the tool.
        assertThat(callbacks.get(0)
            .getToolDefinition()
            .description()).isEqualTo("Slack: Send Message: Send a message to a Slack channel");
    }

    @Test
    void testResolveSkipsBindingsWhoseClusterElementVanishedFromCatalog() {
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        AiHubTaskToolFacade taskToolFacade = mock(AiHubTaskToolFacade.class);
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ConnectionService connectionService = mock(ConnectionService.class);

        AiHubTask task = mock(AiHubTask.class);
        when(task.getId()).thenReturn(7L);
        when(taskService.findByThreadId("thread-1")).thenReturn(Optional.of(task));

        // One healthy binding + one whose cluster element was removed from the catalog (e.g. component author
        // dropped the action between attach time and now). Resolver must register the surviving one and skip
        // the dead one — failing the whole turn on a single dead binding would break the entire task.
        AiHubTaskToolBinding aliveBinding = new AiHubTaskToolBinding(
            11L, 99L, 7L, "slack", 1, "sendMessage", 42L, 0, Map.of());
        AiHubTaskToolBinding deadBinding = new AiHubTaskToolBinding(
            12L, 100L, 7L, "deprecated-component", 1, "removed-action", 50L, 0, Map.of());

        when(taskToolFacade.listTaskTools(7L)).thenReturn(List.of(aliveBinding, deadBinding));

        ClusterElementDefinition aliveDef = mock(ClusterElementDefinition.class);
        when(aliveDef.getDescription()).thenReturn("Send a message");
        when(aliveDef.getTitle()).thenReturn("Slack");
        when(aliveDef.getProperties()).thenReturn(List.of());

        when(clusterElementDefinitionService.getClusterElementDefinition(eq("slack"), eq(1), eq("sendMessage")))
            .thenReturn(aliveDef);
        when(clusterElementDefinitionService.getClusterElementDefinition(
            eq("deprecated-component"), eq(1), eq("removed-action")))
                .thenThrow(new RuntimeException("Cluster element not found"));

        AiHubTaskBindingToolCallbackResolver resolver = newResolver(
            taskService, taskToolFacade, clusterElementDefinitionService, connectionService);

        AiHubToolInvocationContext context = new AiHubToolInvocationContext(
            1L, 10L, (short) 0, "x", 0L, "thread-1");

        List<ToolCallback> callbacks = resolver.resolve(context);

        assertThat(callbacks).hasSize(1);
        assertThat(callbacks.getFirst()
            .getToolDefinition()
            .name()).isEqualTo("slack_sendMessage");
    }

    private static AiHubTaskBindingToolCallbackResolver newResolver(
        AiHubTaskService taskService,
        AiHubTaskToolFacade taskToolFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService) {

        // No user-global "added connectors" in these task-binding tests — exercised separately.
        lenient()
            .when(taskToolFacade.listUserTools(anyLong(), anyLong()))
            .thenReturn(List.of());

        // No external MCP server tools in these task-binding tests — exercised separately.
        AiHubMcpToolCallbackProvider mcpToolCallbackProvider = mock(AiHubMcpToolCallbackProvider.class);

        lenient()
            .when(mcpToolCallbackProvider.resolve(anyLong(), anyLong()))
            .thenReturn(List.of());

        // No skill tools in these task-binding tests — exercised separately.
        AiHubSkillsToolProvider skillsToolCallbackProvider = mock(AiHubSkillsToolProvider.class);

        lenient()
            .when(skillsToolCallbackProvider.resolve(anyLong()))
            .thenReturn(List.of());

        return new AiHubTaskBindingToolCallbackResolver(
            taskService, taskToolFacade, clusterElementDefinitionService, connectionService, mcpToolCallbackProvider,
            skillsToolCallbackProvider);
    }
}
