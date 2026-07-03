/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.task.repository.AiHubTaskComponentRepository;
import com.bytechef.ee.ai.hub.task.repository.AiHubTaskToolRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubTaskToolFacadeImplTest {

    @Test
    void testAttachComponentReturnsExistingIdOnIdempotentRetry() {
        // Pin the idempotency contract: re-attaching the same (task, component, version, connection,
        // environment) hits the existing row and returns its id without inserting. Without this, the chat
        // affordance "add Slack to this task" would either fail with a unique-constraint violation or
        // proliferate duplicate rows on every retry.
        AiHubTaskComponentRepository componentRepository =
            mock(AiHubTaskComponentRepository.class);
        AiHubTaskToolRepository toolRepository = mock(AiHubTaskToolRepository.class);

        AiHubTaskComponent existing = new AiHubTaskComponent(1L, "slack", 1, 42L, 0);

        existing.setId(99L);

        when(componentRepository.findIdempotencyMatch(1L, "slack", 1, 42L, 0)).thenReturn(Optional.of(existing));

        AiHubTaskToolFacadeImpl facade =
            new AiHubTaskToolFacadeImpl(componentRepository, toolRepository, null);

        long result = facade.attachComponent(1L, "slack", 1, 42L, 0);

        assertThat(result).isEqualTo(99L);

        verify(componentRepository, never()).save(any());
    }

    @Test
    void testAttachComponentInsertsWhenNoExistingMatch() {
        AiHubTaskComponentRepository componentRepository =
            mock(AiHubTaskComponentRepository.class);
        AiHubTaskToolRepository toolRepository = mock(AiHubTaskToolRepository.class);

        when(componentRepository.findIdempotencyMatch(1L, "slack", 1, 42L, 0)).thenReturn(Optional.empty());

        AiHubTaskComponent saved = new AiHubTaskComponent(1L, "slack", 1, 42L, 0);

        saved.setId(101L);

        when(componentRepository.save(any(AiHubTaskComponent.class))).thenReturn(saved);

        AiHubTaskToolFacadeImpl facade =
            new AiHubTaskToolFacadeImpl(componentRepository, toolRepository, null);

        long result = facade.attachComponent(1L, "slack", 1, 42L, 0);

        assertThat(result).isEqualTo(101L);
    }

    @Test
    void testAddToolUpsertsByName() {
        // Pin upsert-by-name contract: calling addTool twice with different parameters for the same
        // (componentId, name) updates the existing tool's parameters in place rather than creating a duplicate.
        // The chat affordance "configure sendMessage with channel #engineering" must be replay-safe — the user
        // adjusting params mid-task should land on the same row.
        AiHubTaskComponentRepository componentRepository =
            mock(AiHubTaskComponentRepository.class);
        AiHubTaskToolRepository toolRepository = mock(AiHubTaskToolRepository.class);

        AiHubTaskTool existing =
            new AiHubTaskTool(99L, "sendMessage", Map.of("channel", "#general"));

        existing.setId(7L);

        when(toolRepository.findByTaskComponentIdAndName(99L, "sendMessage")).thenReturn(
            Optional.of(existing));
        when(toolRepository.save(any(AiHubTaskTool.class))).thenAnswer(inv -> inv.getArgument(0));

        AiHubTaskToolFacadeImpl facade =
            new AiHubTaskToolFacadeImpl(componentRepository, toolRepository, null);

        long result = facade.addTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        assertThat(result).isEqualTo(7L);
        // Verify the parameters got updated on the existing instance, not a fresh row. Map<String,?> wildcard
        // capture defeats AssertJ's containsEntry inference, so dereference + cast for the assertion.
        assertThat(existing.getParameters()
            .get("channel")).isEqualTo("#engineering");
    }

    @Test
    void testAddToolInsertsWhenNoExistingMatch() {
        AiHubTaskComponentRepository componentRepository =
            mock(AiHubTaskComponentRepository.class);
        AiHubTaskToolRepository toolRepository = mock(AiHubTaskToolRepository.class);

        when(toolRepository.findByTaskComponentIdAndName(99L, "sendMessage")).thenReturn(Optional.empty());

        AiHubTaskTool saved =
            new AiHubTaskTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        saved.setId(11L);

        when(toolRepository.save(any(AiHubTaskTool.class))).thenReturn(saved);

        AiHubTaskToolFacadeImpl facade =
            new AiHubTaskToolFacadeImpl(componentRepository, toolRepository, null);

        long result = facade.addTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        assertThat(result).isEqualTo(11L);
    }

    @Test
    void testRemoveToolDelegatesToRepository() {
        // Idempotent contract: deleteById on a non-existent id is a no-op in Spring Data JDBC. The facade
        // doesn't add a pre-check (a check would race with concurrent deletion). We simply verify the
        // delegation happens; the no-op-ness comes from the repository contract itself.
        AiHubTaskComponentRepository componentRepository =
            mock(AiHubTaskComponentRepository.class);
        AiHubTaskToolRepository toolRepository = mock(AiHubTaskToolRepository.class);

        AiHubTaskToolFacadeImpl facade =
            new AiHubTaskToolFacadeImpl(componentRepository, toolRepository, null);

        facade.removeTool(7L);

        verify(toolRepository).deleteById(7L);
    }

    @Test
    void testDetachComponentDoesNotThrowForUserGlobalConnector() {
        // User-global connectors (added on the Connectors page) carry a null taskId — they are scoped by
        // (userId, workspaceId), not a task. detachComponent is shared by task-bound and user-global rows, so
        // it must not unbox the null taskId into the per-task refresh path. Regression for the NPE raised by
        // removeAiHubUserConnector ("Cannot invoke Long.longValue() because getTaskId() is null").
        AiHubTaskComponentRepository componentRepository =
            mock(AiHubTaskComponentRepository.class);
        AiHubTaskToolRepository toolRepository = mock(AiHubTaskToolRepository.class);

        AiHubTaskComponent userConnector = new AiHubTaskComponent();

        userConnector.setId(55L);
        userConnector.setUserId(3L);
        userConnector.setWorkspaceId(8L);
        userConnector.setComponentName("slack");
        userConnector.setComponentVersion(1);
        // taskId stays null => user-global connector.

        when(componentRepository.findById(55L)).thenReturn(Optional.of(userConnector));

        AiHubTaskToolFacadeImpl facade =
            new AiHubTaskToolFacadeImpl(componentRepository, toolRepository, null);

        facade.detachComponent(55L);

        verify(componentRepository).deleteById(55L);
    }

    @Test
    void testListTaskToolsJoinsComponentContextIntoBindings() {
        // The dynamic agent-tool resolver consumes AiHubTaskToolBinding directly — every field needed
        // to
        // construct a callable ClusterElementToolCallback is on the binding, with no further DB round-trips.
        // Pin that the join produces (componentName, version, connectionId, environment) per tool.
        AiHubTaskComponentRepository componentRepository =
            mock(AiHubTaskComponentRepository.class);
        AiHubTaskToolRepository toolRepository = mock(AiHubTaskToolRepository.class);

        AiHubTaskComponent slackComponent =
            new AiHubTaskComponent(1L, "slack", 1, 42L, 0);

        slackComponent.setId(99L);

        when(componentRepository.findAllByTaskId(1L)).thenReturn(List.of(slackComponent));

        AiHubTaskTool sendTool =
            new AiHubTaskTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        sendTool.setId(7L);

        when(toolRepository.findAllByTaskComponentId(99L)).thenReturn(List.of(sendTool));

        AiHubTaskToolFacadeImpl facade =
            new AiHubTaskToolFacadeImpl(componentRepository, toolRepository, null);

        List<AiHubTaskToolBinding> bindings = facade.listTaskTools(1L);

        assertThat(bindings).hasSize(1);

        AiHubTaskToolBinding binding = bindings.getFirst();

        assertThat(binding.taskToolId()).isEqualTo(7L);
        assertThat(binding.componentName()).isEqualTo("slack");
        assertThat(binding.componentVersion()).isEqualTo(1);
        assertThat(binding.connectionId()).isEqualTo(42L);
        assertThat(binding.environment()).isEqualTo(0);
        assertThat(binding.clusterElementName()).isEqualTo("sendMessage");
        assertThat(binding.parameters()
            .get("channel")).isEqualTo("#engineering");
    }

    @Test
    void testListTaskToolsReturnsEmptyForBareTask() {
        AiHubTaskComponentRepository componentRepository =
            mock(AiHubTaskComponentRepository.class);
        AiHubTaskToolRepository toolRepository = mock(AiHubTaskToolRepository.class);

        when(componentRepository.findAllByTaskId(1L)).thenReturn(List.of());

        AiHubTaskToolFacadeImpl facade =
            new AiHubTaskToolFacadeImpl(componentRepository, toolRepository, null);

        assertThat(facade.listTaskTools(1L)).isEmpty();

        verify(toolRepository, never()).findAllByTaskComponentId(any(Long.class));
    }
}
