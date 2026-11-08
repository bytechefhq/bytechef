/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.repository.AiHubChatComponentRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatConnectorRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatToolRepository;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AiHubChatToolFacadeImplTest {

    @Test
    void testAttachComponentReturnsExistingIdOnIdempotentRetry() {
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatComponent existing = new AiHubChatComponent(1L, "slack", 1, 42L, 0);

        existing.setId(99L);

        when(componentRepository.findIdempotencyMatch(1L, "slack", 1, 42L, 0)).thenReturn(Optional.of(existing));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

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
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        long result = facade.attachComponent(1L, "slack", 1, 42L, 0);

        assertThat(result).isEqualTo(101L);
    }

    @Test
    void testAddToolUpsertsByName() {
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
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        long result = facade.addTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        assertThat(result).isEqualTo(7L);
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
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        long result = facade.addTool(99L, "sendMessage", Map.of("channel", "#engineering"));

        assertThat(result).isEqualTo(11L);
    }

    @Test
    void testRemoveToolDelegatesToRepository() {
        AiHubChatComponentRepository componentRepository =
            mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        facade.removeTool(7L);

        verify(toolRepository).deleteById(7L);
    }

    @Test
    void testDetachComponentDoesNotThrowForUserGlobalConnector() {
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

        when(componentRepository.findById(55L)).thenReturn(Optional.of(userConnector));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        facade.detachComponent(55L);

        verify(componentRepository).deleteById(55L);
    }

    @Test
    void testListChatToolsJoinsComponentContextIntoBindings() {
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
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

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
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        assertThat(facade.listChatTools(1L)).isEmpty();

        verify(toolRepository, never()).findAllByChatComponentId(any(Long.class));
    }

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
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        facade.setChatConnectorEnabled(7L, 99L, false);

        ArgumentCaptor<AiHubChatConnector> captor = ArgumentCaptor.forClass(AiHubChatConnector.class);

        verify(connectorRepository).save(captor.capture());

        AiHubChatConnector saved = captor.getValue();

        assertThat(saved.getChatId()).isEqualTo(7L);
        assertThat(saved.getComponentName()).isEqualTo("slack");
        assertThat(saved.isEnabled()).isFalse();

        verify(componentRepository, never()).save(any(AiHubChatComponent.class));
    }

    @Test
    void testListChatDisabledConnectorsReportsOnlySwitchedOffOnes() {
        AiHubChatComponentRepository componentRepository = mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        when(connectorRepository.findAllByChatId(7L)).thenReturn(
            List.of(new AiHubChatConnector(7L, "slack", false), new AiHubChatConnector(7L, "github", true)));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        assertThat(facade.listChatDisabledConnectors(7L)).containsExactly("slack");
    }

    @Test
    void testSetToolRequiresApprovalFlipsTheFlag() {
        AiHubChatComponentRepository componentRepository = mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatTool tool = new AiHubChatTool(99L, "sendMessage", Map.of());

        tool.setId(5L);
        tool.setRequiresApproval(false);

        when(toolRepository.findById(5L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(AiHubChatTool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        facade.setToolRequiresApproval(5L, true);

        ArgumentCaptor<AiHubChatTool> captor = ArgumentCaptor.forClass(AiHubChatTool.class);

        verify(toolRepository).save(captor.capture());

        AiHubChatTool saved = captor.getValue();

        assertThat(saved.isRequiresApproval()).isTrue();
    }

    @Test
    void testSetToolRequiresApprovalThrowsForUnknownId() {
        AiHubChatComponentRepository componentRepository = mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        when(toolRepository.findById(404L)).thenReturn(Optional.empty());

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        assertThatThrownBy(() -> facade.setToolRequiresApproval(404L, true))
            .isInstanceOf(NotFoundException.class);

        verify(toolRepository, never()).save(any(AiHubChatTool.class));
    }

    @Test
    void testSetToolRequiresApprovalByNameFlipsTheFlagOnExistingRow() {
        AiHubChatComponentRepository componentRepository = mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        AiHubChatTool existing = new AiHubChatTool(99L, "sendMessage", Map.of("channel", "#general"));

        existing.setId(7L);
        existing.setRequiresApproval(false);

        when(toolRepository.findByChatComponentIdAndName(99L, "sendMessage")).thenReturn(Optional.of(existing));
        when(toolRepository.save(any(AiHubChatTool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        facade.setToolRequiresApproval(99L, "sendMessage", true);

        ArgumentCaptor<AiHubChatTool> captor = ArgumentCaptor.forClass(AiHubChatTool.class);

        verify(toolRepository).save(captor.capture());

        AiHubChatTool saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(7L);
        assertThat(saved.isRequiresApproval()).isTrue();
        assertThat(saved.getParameters()
            .get("channel")).isEqualTo("#general");
    }

    @Test
    void testSetToolRequiresApprovalByNameCreatesRowWhenNoneExists() {
        AiHubChatComponentRepository componentRepository = mock(AiHubChatComponentRepository.class);
        AiHubChatToolRepository toolRepository = mock(AiHubChatToolRepository.class);
        AiHubChatConnectorRepository connectorRepository = mock(AiHubChatConnectorRepository.class);

        when(toolRepository.findByChatComponentIdAndName(99L, "sendMessage")).thenReturn(Optional.empty());
        when(toolRepository.save(any(AiHubChatTool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiHubChatToolFacadeImpl facade =
            new AiHubChatToolFacadeImpl(componentRepository, connectorRepository, toolRepository);

        facade.setToolRequiresApproval(99L, "sendMessage", true);

        ArgumentCaptor<AiHubChatTool> captor = ArgumentCaptor.forClass(AiHubChatTool.class);

        verify(toolRepository).save(captor.capture());

        AiHubChatTool saved = captor.getValue();

        assertThat(saved.getChatComponentId()).isEqualTo(99L);
        assertThat(saved.getName()).isEqualTo("sendMessage");
        assertThat(saved.isRequiresApproval()).isTrue();
    }
}
