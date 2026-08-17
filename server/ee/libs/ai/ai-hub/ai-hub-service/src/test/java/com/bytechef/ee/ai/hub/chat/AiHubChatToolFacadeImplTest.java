/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.repository.AiHubChatComponentRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatConnectorRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatToolRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatToolFacadeImplTest {

    @Test
    void testAttachComponentReturnsExistingIdOnIdempotentRetry() {
        // Pin the idempotency contract: re-attaching the same (chat, component, version, connection,
        // environment) hits the existing row and returns its id without inserting. Without this, the chat
        // affordance "add Slack to this chat" would either fail with a unique-constraint violation or
        // proliferate duplicate rows on every retry.
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatComponent existing = new AiHubChatComponent(1L, "slack", 1, 42L, 0);

        existing.setId(99L);

        when(componentRepository.findIdempotencyMatch(1L, "slack", 1, 42L, 0)).thenReturn(Optional.of(existing));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        long result = facade.attachComponent(1L, "slack", 1, 42L, 0);

        assertThat(result).isEqualTo(99L);

        verify(componentRepository, never()).save(any());
    }

    @Test
    void testAttachComponentInsertsWhenNoExistingMatch() {
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        when(componentRepository.findIdempotencyMatch(1L, "slack", 1, 42L, 0)).thenReturn(Optional.empty());

        AiHubChatComponent saved = new AiHubChatComponent(1L, "slack", 1, 42L, 0);

        saved.setId(101L);

        when(componentRepository.save(any(AiHubChatComponent.class))).thenReturn(saved);

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        long result = facade.attachComponent(1L, "slack", 1, 42L, 0);

        assertThat(result).isEqualTo(101L);
    }

    @Test
    void testAddToolUpsertsByName() {
        // Pin upsert-by-name contract: calling addTool twice with different parameters for the same
        // (componentId, name) updates the existing tool's parameters in place rather than creating a duplicate.
        // The chat affordance "configure sendMessage with channel #engineering" must be replay-safe — the user
        // adjusting params mid-chat should land on the same row.
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatTool existing =
            new AiHubChatTool(99L, "sendMessage", Map.of("channel", "#general"));

        existing.setId(7L);

        when(toolRepository.findByChatComponentIdAndName(99L, "sendMessage")).thenReturn(
            Optional.of(existing));
        when(toolRepository.save(any(AiHubChatTool.class))).thenAnswer(inv -> inv.getArgument(0));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        long result = facade.addTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        assertThat(result).isEqualTo(7L);
        // Verify the parameters got updated on the existing instance, not a fresh row. Map<String,?> wildcard
        // capture defeats AssertJ's containsEntry inference, so dereference + cast for the assertion.
        assertThat(existing.getParameters()
            .get("channel")).isEqualTo("#engineering");
    }

    @Test
    void testAddToolInsertsWhenNoExistingMatch() {
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        when(toolRepository.findByChatComponentIdAndName(99L, "sendMessage")).thenReturn(Optional.empty());

        AiHubChatTool saved =
            new AiHubChatTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        saved.setId(11L);

        when(toolRepository.save(any(AiHubChatTool.class))).thenReturn(saved);

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        long result = facade.addTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        assertThat(result).isEqualTo(11L);
    }

    @Test
    void testRemoveToolDelegatesToRepository() {
        // Idempotent contract: deleteById on a non-existent id is a no-op in Spring Data JDBC. The facade
        // doesn't add a pre-check (a check would race with concurrent deletion). We simply verify the
        // delegation happens; the no-op-ness comes from the repository contract itself.
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        facade.removeTool(7L);

        verify(toolRepository).deleteById(7L);
    }

    @Test
    void testDetachComponentDoesNotThrowForUserGlobalConnector() {
        // User-global connectors (added on the Connectors page) carry a null chatId — they are scoped by
        // (userId, workspaceId), not a chat. detachComponent is shared by chat-bound and user-global rows, so
        // it must not unbox the null chatId into the per-chat refresh path. Regression for the NPE raised by
        // removeAiHubUserConnector ("Cannot invoke Long.longValue() because getChatId() is null").
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatComponent userConnector = new AiHubChatComponent();

        userConnector.setId(55L);
        userConnector.setUserId(3L);
        userConnector.setWorkspaceId(8L);
        userConnector.setComponentName("slack");
        userConnector.setComponentVersion(1);
        // chatId stays null => user-global connector.

        when(componentRepository.findById(55L)).thenReturn(Optional.of(userConnector));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        facade.detachComponent(55L);

        verify(componentRepository).deleteById(55L);
    }

    @Test
    void testListChatToolsJoinsComponentContextIntoBindings() {
        // The dynamic agent-tool resolver consumes AiHubChatToolBinding directly — every field needed
        // to
        // construct a callable ClusterElementToolCallback is on the binding, with no further DB round-trips.
        // Pin that the join produces (componentName, version, connectionId, environment) per tool.
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatComponent slackComponent =
            new AiHubChatComponent(1L, "slack", 1, 42L, 0);

        slackComponent.setId(99L);

        when(componentRepository.findAllByChatId(1L)).thenReturn(List.of(slackComponent));

        AiHubChatTool sendTool =
            new AiHubChatTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        sendTool.setId(7L);

        when(toolRepository.findAllByChatComponentId(99L)).thenReturn(List.of(sendTool));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        List<AiHubChatToolBinding> bindings = facade.listChatTools(1L);

        assertThat(bindings).hasSize(1);

        AiHubChatToolBinding binding = bindings.getFirst();

        assertThat(binding.chatToolId()).isEqualTo(7L);
        assertThat(binding.componentName()).isEqualTo("slack");
        assertThat(binding.componentVersion()).isEqualTo(1);
        assertThat(binding.connectionId()).isEqualTo(42L);
        assertThat(binding.environment()).isEqualTo(0);
        assertThat(binding.clusterElementName()).isEqualTo("sendMessage");
        assertThat(binding.parameters()
            .get("channel")).isEqualTo("#engineering");
    }

    @Test
    void testListChatToolsReturnsEmptyForBareChat() {
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        when(componentRepository.findAllByChatId(1L)).thenReturn(List.of());

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        assertThat(facade.listChatTools(1L)).isEmpty();

        verify(toolRepository, never()).findAllByChatComponentId(any(Long.class));
    }

    /**
     * Switching a connector off in one chat must not touch ai_hub_chat_component — not the user-global row (that is
     * availability, which the Connectors page owns) and not a chat-scoped one either. A chat-scoped row is an
     * attachment whose tools {@code listChatTools} serves unconditionally, so parking the flag there would leave the
     * tools live while the composer switch showed OFF.
     */
    @Test
    void testSetChatConnectorEnabledWritesParticipationWithoutTouchingComponents() {
        AiHubChatComponentRepository componentRepository = mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatComponent userConnector = new AiHubChatComponent();

        userConnector.setId(99L);
        userConnector.setComponentName("slack");
        userConnector.setComponentVersion(1);

        when(componentRepository.findById(99L)).thenReturn(Optional.of(userConnector));
        when(connectorRepository.findByChatIdAndComponentName(7L, "slack")).thenReturn(Optional.empty());

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        facade.setChatConnectorEnabled(7L, 99L, false);

        ArgumentCaptor<AiHubChatConnector> captor = ArgumentCaptor.forClass(AiHubChatConnector.class);

        verify(connectorRepository).save(captor.capture());

        AiHubChatConnector saved = captor.getValue();

        assertThat(saved.getChatId()).isEqualTo(7L);
        assertThat(saved.getComponentName()).isEqualTo("slack");
        assertThat(saved.isEnabled()).isFalse();

        verify(componentRepository, never()).save(any(AiHubChatComponent.class));
    }

    /**
     * Absence of a row means participating, so only the switched-off connectors are reported. A caller subtracts this
     * set from the user-global tools; reporting an enabled row here would silently suppress a live connector.
     */
    @Test
    void testListChatDisabledConnectorsReportsOnlySwitchedOffOnes() {
        AiHubChatComponentRepository componentRepository = mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        when(connectorRepository.findAllByChatId(7L)).thenReturn(
            List.of(new AiHubChatConnector(7L, "slack", false), new AiHubChatConnector(7L, "github", true)));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository, null);

        assertThat(facade.listChatDisabledConnectors(7L)).containsExactly("slack");
    }
}
