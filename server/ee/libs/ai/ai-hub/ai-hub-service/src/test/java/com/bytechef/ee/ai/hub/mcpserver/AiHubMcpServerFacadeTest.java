/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerRepository;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerToolRepository;
import com.bytechef.encryption.Encryption;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AiHubMcpServerFacadeTest {

    private static final long OWNER_USER_ID = 10L;
    private static final long WORKSPACE_ID = 1L;

    private final AiHubMcpServerRepository mcpServerRepository = mock(AiHubMcpServerRepository.class);
    private final AiHubMcpServerToolRepository mcpServerToolRepository = mock(AiHubMcpServerToolRepository.class);
    private final AiHubMcpClientManager clientManager = mock(AiHubMcpClientManager.class);
    private final Encryption encryption = mock(Encryption.class);

    @Test
    void testAddMcpServerRejectsCloudMetadataUrl() {
        AiHubMcpServerFacadeImpl facade = newFacade(List.of());

        assertThatThrownBy(
            () -> facade.addMcpServer(
                OWNER_USER_ID, WORKSPACE_ID, 0, "metadata", "http://169.254.169.254/latest/meta-data/", "secret"))
                    .isInstanceOf(UrlValidationException.class);

        verify(mcpServerRepository, never()).save(any());
        verify(encryption, never()).encrypt(anyString());
    }

    @Test
    void testAddMcpServerRejectsLoopbackAndNonHttpUrls() {
        AiHubMcpServerFacadeImpl facade = newFacade(List.of());

        assertThatThrownBy(
            () -> facade.addMcpServer(OWNER_USER_ID, WORKSPACE_ID, 0, "local", "http://127.0.0.1:8080/mcp", null))
                .isInstanceOf(UrlValidationException.class);
        assertThatThrownBy(
            () -> facade.addMcpServer(OWNER_USER_ID, WORKSPACE_ID, 0, "file", "file:///etc/passwd", null))
                .isInstanceOf(UrlValidationException.class);

        verify(mcpServerRepository, never()).save(any());
    }

    @Test
    void testAddMcpServerAcceptsAnAllowlistedPrivateHostAndStoresTheTokenEncrypted() {
        when(encryption.encrypt("secret")).thenReturn("encrypted-secret");
        when(mcpServerRepository.save(any(AiHubMcpServer.class))).thenAnswer(invocation -> {
            AiHubMcpServer mcpServer = invocation.getArgument(0);

            mcpServer.setId(5L);

            return mcpServer;
        });

        AiHubMcpServerFacadeImpl facade = newFacade(List.of("localhost"));

        long mcpServerId = facade.addMcpServer(
            OWNER_USER_ID, WORKSPACE_ID, 0, "local", "http://localhost:8080/mcp", "secret");

        ArgumentCaptor<AiHubMcpServer> mcpServerArgumentCaptor = ArgumentCaptor.forClass(AiHubMcpServer.class);

        verify(mcpServerRepository).save(mcpServerArgumentCaptor.capture());

        AiHubMcpServer savedMcpServer = mcpServerArgumentCaptor.getValue();

        assertThat(mcpServerId).isEqualTo(5L);
        assertThat(savedMcpServer.getAuthToken()).isEqualTo("encrypted-secret");
        assertThat(savedMcpServer.getUserId()).isEqualTo(OWNER_USER_ID);
        assertThat(savedMcpServer.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
    }

    @Test
    void testAddMcpServerStoresNoTokenWhenNoneIsGiven() {
        when(mcpServerRepository.save(any(AiHubMcpServer.class))).thenAnswer(invocation -> {
            AiHubMcpServer mcpServer = invocation.getArgument(0);

            mcpServer.setId(6L);

            return mcpServer;
        });

        newFacade(List.of("localhost")).addMcpServer(
            OWNER_USER_ID, WORKSPACE_ID, 0, "local", "http://localhost:8080/mcp", " ");

        ArgumentCaptor<AiHubMcpServer> mcpServerArgumentCaptor = ArgumentCaptor.forClass(AiHubMcpServer.class);

        verify(mcpServerRepository).save(mcpServerArgumentCaptor.capture());

        AiHubMcpServer savedMcpServer = mcpServerArgumentCaptor.getValue();

        assertThat(savedMcpServer.getAuthToken()).isNull();
        verify(encryption, never()).encrypt(anyString());
    }

    @Test
    void testOperationsOnAnotherUsersServerAreRefused() {
        AiHubMcpServer otherUsersMcpServer = mcpServer(5L, 20L, WORKSPACE_ID);

        when(mcpServerRepository.findById(5L)).thenReturn(Optional.of(otherUsersMcpServer));

        AiHubMcpServerFacadeImpl facade = newFacade(List.of());

        assertThat(facade.removeMcpServer(OWNER_USER_ID, WORKSPACE_ID, 5L)).isFalse();
        assertThat(facade.setMcpServerEnabled(OWNER_USER_ID, WORKSPACE_ID, 5L, false)).isFalse();
        assertThat(facade.setMcpServerToolEnabled(OWNER_USER_ID, WORKSPACE_ID, 5L, "search", false)).isFalse();
        assertThat(facade.listMcpServerTools(OWNER_USER_ID, WORKSPACE_ID, 5L)).isEmpty();

        verify(mcpServerRepository, never()).deleteById(anyLong());
        verify(mcpServerRepository, never()).save(any());
        verify(mcpServerToolRepository, never()).save(any());
        verify(clientManager, never()).listTools(anyLong(), anyString(), any());
        verify(encryption, never()).decrypt(anyString());
    }

    @Test
    void testOperationsOnTheOwnServerInAnotherWorkspaceAreRefused() {
        when(mcpServerRepository.findById(5L)).thenReturn(Optional.of(mcpServer(5L, OWNER_USER_ID, 2L)));

        AiHubMcpServerFacadeImpl facade = newFacade(List.of());

        assertThat(facade.removeMcpServer(OWNER_USER_ID, WORKSPACE_ID, 5L)).isFalse();

        verify(mcpServerRepository, never()).deleteById(anyLong());
    }

    @Test
    void testRemoveOwnServerDeletesItAndDropsTheCachedClient() {
        when(mcpServerRepository.findById(5L)).thenReturn(Optional.of(mcpServer(5L, OWNER_USER_ID, WORKSPACE_ID)));
        when(encryption.decrypt("encrypted-secret")).thenReturn("secret");

        assertThat(newFacade(List.of()).removeMcpServer(OWNER_USER_ID, WORKSPACE_ID, 5L)).isTrue();

        verify(mcpServerRepository).deleteById(5L);
        verify(clientManager).invalidate(5L, "https://mcp.example.com/mcp", "secret");
    }

    private AiHubMcpServerFacadeImpl newFacade(List<String> allowedHosts) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Hub.McpServer mcpServer = ai.getHub()
            .getMcpServer();

        mcpServer.setAllowedHosts(allowedHosts);

        return new AiHubMcpServerFacadeImpl(
            mcpServerRepository, mcpServerToolRepository, clientManager, encryption,
            new AiHubMcpServerUrlGuard(applicationProperties));
    }

    private static AiHubMcpServer mcpServer(long id, long userId, long workspaceId) {
        AiHubMcpServer mcpServer = new AiHubMcpServer(
            userId, workspaceId, 0, "server", "https://mcp.example.com/mcp", "encrypted-secret");

        mcpServer.setId(id);

        return mcpServer;
    }
}
