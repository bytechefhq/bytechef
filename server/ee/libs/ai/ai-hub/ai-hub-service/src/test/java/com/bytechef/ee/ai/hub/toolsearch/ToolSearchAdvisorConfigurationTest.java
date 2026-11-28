/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.spring.ai.TurnEndingToolCallingManager;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatDeletedEvent;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolSearchAdvisorConfigurationTest {

    private final ToolSearchAdvisorConfiguration configuration = new ToolSearchAdvisorConfiguration();
    private final ToolSearchCatalogWarmup warmup = new ToolSearchCatalogWarmup(null, List.of());

    @Test
    void testAdvisorIsAbsentWithoutTheToolSearchVectorStore() {
        AiHubGlobalToolCatalog catalog = new AiHubGlobalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, List.of(toolCallback("listComponents")));

        assertThat(
            ToolSearchAdvisorConfiguration.buildModeAdvisor(
                Mode.ASK, null, ObservationRegistry.NOOP, catalog, null, null, warmup, null))
                    .isNull();
    }

    @Test
    void testAdvisorIsAbsentWithoutAModeCatalog() {
        assertThat(
            ToolSearchAdvisorConfiguration.buildModeAdvisor(
                Mode.ASK, mock(VectorStore.class), ObservationRegistry.NOOP, null, null, null, warmup, null))
                    .isNull();
    }

    @Test
    void testAdvisorIsBuiltWithoutTouchingTheCatalogTools() {
        ToolCallback catalogTool = toolCallback("listComponents");

        AiHubGlobalToolCatalog catalog = new AiHubGlobalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID, List.of(catalogTool));

        ToolSearchToolCallingAdvisor advisor = ToolSearchAdvisorConfiguration.buildModeAdvisor(
            Mode.BUILD, mock(VectorStore.class), ObservationRegistry.NOOP, catalog, null, null, warmup, null);

        assertThat(advisor).isInstanceOf(PinnedToolSearchToolCallingAdvisor.class);

        verify(catalogTool, never()).getToolDefinition();
    }

    @Test
    void testCatalogToolsAreWrappedAndKeyedByName() {
        ToolCallback listComponents = toolCallback("listComponents");
        ToolCallback getTask = toolCallback("getTask");

        when(listComponents.call(anyString(), any())).thenReturn("[]");

        Map<String, ToolCallback> callbackMap = ToolSearchAdvisorConfiguration.wrapCatalogToolCallbacks(
            List.of(listComponents, getTask), null, null);

        assertThat(callbackMap).containsOnlyKeys("listComponents", "getTask");
        assertThat(callbackMap.get("listComponents")).isNotSameAs(listComponents);
        assertThat(callbackMap.get("listComponents")
            .call("{}", null)).isEqualTo("[]");
    }

    @Test
    void testBuildModeReachesEveryClusterElementToolAndAskModeOnlyTheOnesThatRead() {
        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);
        AiHubClusterElementToolCallbacks clusterElementToolCallbacks = clusterElementToolCallbacks(
            clusterElementDefinitionService);
        AiHubGlobalToolCatalog catalog = new AiHubGlobalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID, List.of(toolCallback("listComponents")));

        assertThat(
            ToolSearchAdvisorConfiguration.catalogCallbackMap(
                Mode.BUILD, catalog, clusterElementToolCallbacks, null, null))
                    .containsOnlyKeys("listComponents", "slack_sendMessage", "slack_listChannels");
        assertThat(
            ToolSearchAdvisorConfiguration.catalogCallbackMap(
                Mode.ASK, catalog, clusterElementToolCallbacks, null, null))
                    .containsOnlyKeys("listComponents", "slack_listChannels");
        verify(clusterElementDefinitionService, never()).getClusterElementDefinition(
            anyString(), anyInt(), anyString());
    }

    @Test
    void testAnUnapprovedClusterElementToolThatWritesIsHeldForApproval() {
        AiHubToolApprovalService approvalService = mock(AiHubToolApprovalService.class);
        AiHubChatService chatService = mock(AiHubChatService.class);
        AiHubChat chat = new AiHubChat(3L);

        chat.setId(11L);
        chat.setThreadId("thread-1");
        chat.setWorkspaceId(7L);

        when(chatService.findNotDeletedByThreadId("thread-1")).thenReturn(Optional.of(chat));
        when(approvalService.findPending(11L)).thenReturn(Optional.empty());
        when(approvalService.createPending(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClusterElementDefinitionFacade clusterElementDefinitionFacade = mock(ClusterElementDefinitionFacade.class);
        AiHubApprovalGate approvalGate = new AiHubApprovalGate(
            workspaceId -> new AiHubToolApprovalPolicy.Decision(Set.of(), Set.of()), approvalService, chatService);

        Map<String, ToolCallback> callbackMap = ToolSearchAdvisorConfiguration.catalogCallbackMap(
            Mode.BUILD,
            new AiHubGlobalToolCatalog(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID, List.of()),
            clusterElementToolCallbacks(mock(ClusterElementDefinitionService.class), clusterElementDefinitionFacade),
            null, approvalGate);

        String result = callbackMap.get("slack_sendMessage")
            .call(
                "{\"text\":\"hi\"}",
                new ToolContext(
                    AgentToolInvocationContext.builder()
                        .workspaceId(7L)
                        .userId(3L)
                        .environmentId(1L)
                        .conversationId("thread-1")
                        .build()
                        .toToolContext()));

        assertThat(result).contains("\"awaitingApproval\":true");
        verify(approvalService).createPending(any());
        verify(clusterElementDefinitionFacade, never()).executeTool(
            anyString(), anyInt(), anyString(), any(), any());
    }

    @Test
    void testSearchCoversTheClusterElementSessionsOfTheMode() {
        AiHubGlobalToolCatalog catalog = new AiHubGlobalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, List.of());
        AiHubClusterElementToolCallbacks clusterElementToolCallbacks = new AiHubClusterElementToolCallbacks(Map::of);

        assertThat(ToolSearchAdvisorConfiguration.searchSessionIds(Mode.ASK, catalog, clusterElementToolCallbacks))
            .containsExactlyInAnyOrder(
                ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, ToolSearchCatalogFeeder.CLUSTER_ELEMENT_READ_SESSION_ID);
        assertThat(ToolSearchAdvisorConfiguration.searchSessionIds(Mode.BUILD, catalog, clusterElementToolCallbacks))
            .contains(
                ToolSearchCatalogFeeder.CLUSTER_ELEMENT_READ_SESSION_ID,
                ToolSearchCatalogFeeder.CLUSTER_ELEMENT_WRITE_SESSION_ID);
        assertThat(ToolSearchAdvisorConfiguration.searchSessionIds(Mode.BUILD, catalog, null))
            .containsExactly(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID);
    }

    @Test
    void testToolCallingManagerEndsTheTurnLikeTheAgent() {
        assertThat(ToolSearchAdvisorConfiguration.buildToolCallingManager(ObservationRegistry.NOOP, Map::of))
            .isInstanceOf(TurnEndingToolCallingManager.class);
    }

    @Test
    void testFeederIsAbsentWithoutTheToolSearchVectorStore() {
        ToolSearchCatalogFeeder feeder = configuration.toolSearchCatalogFeeder(
            provider(mock(ClusterElementDefinitionService.class)), provider(null), provider(mock(JdbcTemplate.class)),
            new PgVectorStoreProperties(), provider(null));

        assertThat(feeder).isNull();
    }

    @Test
    void testFeederIsBuiltWhenEverythingIsPresent() {
        ToolSearchCatalogFeeder feeder = configuration.toolSearchCatalogFeeder(
            provider(mock(ClusterElementDefinitionService.class)), provider(mock(VectorStore.class)),
            provider(mock(JdbcTemplate.class)), new PgVectorStoreProperties(), provider(null));

        assertThat(feeder).isNotNull();
    }

    @Test
    void testEmbeddingModelIdentityNamesTheCopilotProviderAndModel() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ai.getCopilot()
            .getEmbedding()
            .setProvider(ApplicationProperties.Ai.Copilot.Embedding.Provider.OLLAMA);
        ai.getProvider()
            .getEmbedding()
            .getOllama()
            .getOptions()
            .setModel("nomic-embed-text");

        PgVectorStoreProperties properties = new PgVectorStoreProperties();

        properties.setDimensions(768);

        assertThat(ToolSearchAdvisorConfiguration.embeddingModelIdentity(applicationProperties, properties))
            .isEqualTo("OLLAMA:nomic-embed-text:768");
    }

    @Test
    void testSessionEvictorClearsTheDeletedChatsSessionOnEveryAdvisor() {
        ToolSearchToolCallingAdvisor askAdvisor = mock(ToolSearchToolCallingAdvisor.class);
        ToolSearchToolCallingAdvisor buildAdvisor = mock(ToolSearchToolCallingAdvisor.class);

        ToolSearchSessionEvictor evictor = new ToolSearchSessionEvictor(() -> List.of(askAdvisor, buildAdvisor));

        evictor.onChatDeleted(new AiHubChatDeletedEvent(1L, "thread-1"));

        verify(askAdvisor).evictSession("thread-1");
        verify(buildAdvisor).evictSession("thread-1");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(value);
        when(objectProvider.getIfAvailable(any())).thenAnswer(invocation -> value);
        when(objectProvider.getIfUnique(any())).thenAnswer(invocation -> value);

        return objectProvider;
    }

    private static ToolCallback toolCallback(String name) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build());

        return toolCallback;
    }

    private static AiHubClusterElementToolCallbacks clusterElementToolCallbacks(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return clusterElementToolCallbacks(clusterElementDefinitionService, mock(ClusterElementDefinitionFacade.class));
    }

    private static AiHubClusterElementToolCallbacks clusterElementToolCallbacks(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade) {

        ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);

        when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
            .thenThrow(new NoSuchElementException("no connection"));

        CopilotConnectionLister connectionLister = mock(CopilotConnectionLister.class);

        return new AiHubClusterElementToolCallbacks(
            () -> Map.of(
                "slack_sendMessage",
                new ClusterElementToolCallback(
                    "slack_sendMessage", "Send a message", "slack", 1, "sendMessage", clusterElementDefinitionService,
                    clusterElementDefinitionFacade, connectionDefinitionService, connectionLister),
                "slack_listChannels",
                new ClusterElementToolCallback(
                    "slack_listChannels", "List channels", "slack", 1, "listChannels",
                    clusterElementDefinitionService, clusterElementDefinitionFacade, connectionDefinitionService,
                    connectionLister)));
    }
}
