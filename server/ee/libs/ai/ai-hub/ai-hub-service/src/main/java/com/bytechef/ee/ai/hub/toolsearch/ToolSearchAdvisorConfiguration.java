/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.agui.spring.ai.TurnEndingToolCallingManager;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.WorkspaceCopilotConnectionLister;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.commons.util.MemoizationUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.AiHubToolCallbackWrappers;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.config.AiHubPgVectorConfiguration;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpToolCallbackProvider;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnAiHub
public class ToolSearchAdvisorConfiguration {

    static final int MAX_SEARCH_RESULTS = 5;

    private static final Logger log = LoggerFactory.getLogger(ToolSearchAdvisorConfiguration.class);

    @Bean
    @Nullable
    ToolSearchCatalogFeeder toolSearchCatalogFeeder(
        ObjectProvider<ClusterElementDefinitionService> clusterElementDefinitionServiceProvider,
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        @Qualifier("pgVectorJdbcTemplate") ObjectProvider<JdbcTemplate> pgVectorJdbcTemplateProvider,
        PgVectorStoreProperties properties, ObjectProvider<ApplicationProperties> applicationPropertiesProvider) {

        ClusterElementDefinitionService clusterElementDefinitionService =
            clusterElementDefinitionServiceProvider.getIfAvailable();
        VectorStore toolSearchPgVectorStore = toolSearchPgVectorStoreProvider.getIfAvailable();
        JdbcTemplate pgVectorJdbcTemplate = pgVectorJdbcTemplateProvider.getIfAvailable();

        if (clusterElementDefinitionService == null || toolSearchPgVectorStore == null ||
            pgVectorJdbcTemplate == null) {

            return null;
        }

        return new ToolSearchCatalogFeeder(
            clusterElementDefinitionService, new VectorToolIndex(toolSearchPgVectorStore),
            embeddingModelIdentity(applicationPropertiesProvider.getIfAvailable(), properties), pgVectorJdbcTemplate,
            properties.getSchemaName(), AiHubPgVectorConfiguration.vectorTableName(properties));
    }

    @Bean
    ToolSearchCatalogWarmup toolSearchCatalogWarmup(
        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider) {

        return new ToolSearchCatalogWarmup(
            toolSearchCatalogFeederProvider.getIfAvailable(),
            globalToolCatalogProvider.orderedStream()
                .toList());
    }

    @Bean
    @Nullable
    AiHubClusterElementToolCallbacks aiHubClusterElementToolCallbacks(
        ObjectProvider<ClusterElementDefinitionService> clusterElementDefinitionServiceProvider,
        ObjectProvider<ClusterElementDefinitionFacade> clusterElementDefinitionFacadeProvider,
        ObjectProvider<ConnectionDefinitionService> connectionDefinitionServiceProvider,
        ObjectProvider<WorkspaceConnectionFacade> workspaceConnectionFacadeProvider,
        ObjectProvider<PropertyOptionsResolver> propertyOptionsResolverProvider) {

        ClusterElementDefinitionService clusterElementDefinitionService =
            clusterElementDefinitionServiceProvider.getIfAvailable();
        ClusterElementDefinitionFacade clusterElementDefinitionFacade =
            clusterElementDefinitionFacadeProvider.getIfAvailable();
        ConnectionDefinitionService connectionDefinitionService = connectionDefinitionServiceProvider.getIfAvailable();
        WorkspaceConnectionFacade workspaceConnectionFacade = workspaceConnectionFacadeProvider.getIfAvailable();
        PropertyOptionsResolver propertyOptionsResolver = propertyOptionsResolverProvider.getIfAvailable();

        if (clusterElementDefinitionService == null || clusterElementDefinitionFacade == null ||
            connectionDefinitionService == null || workspaceConnectionFacade == null ||
            propertyOptionsResolver == null) {

            log.warn("AI Hub cluster element tools are unavailable: the component and connection services are missing");

            return null;
        }

        return AiHubClusterElementToolCallbacks.of(
            clusterElementDefinitionService, clusterElementDefinitionFacade, connectionDefinitionService,
            new WorkspaceCopilotConnectionLister(workspaceConnectionFacade, propertyOptionsResolver));
    }

    @Bean
    @Nullable
    AiHubChatBindingToolCallbackResolver aiHubChatBindingToolCallbackResolver(
        ObjectProvider<AiHubChatService> chatServiceProvider,
        ObjectProvider<AiHubChatToolFacade> chatToolFacadeProvider,
        ObjectProvider<ClusterElementDefinitionService> clusterElementDefinitionServiceProvider,
        ObjectProvider<ClusterElementDefinitionFacade> clusterElementDefinitionFacadeProvider,
        ObjectProvider<ConnectionDefinitionService> connectionDefinitionServiceProvider,
        ObjectProvider<WorkspaceConnectionFacade> workspaceConnectionFacadeProvider,
        ObjectProvider<PropertyOptionsResolver> propertyOptionsResolverProvider,
        ObjectProvider<AiHubMcpToolCallbackProvider> mcpToolCallbackProviderProvider) {

        AiHubChatService chatService = chatServiceProvider.getIfAvailable();
        AiHubChatToolFacade chatToolFacade = chatToolFacadeProvider.getIfAvailable();
        ClusterElementDefinitionService clusterElementDefinitionService =
            clusterElementDefinitionServiceProvider.getIfAvailable();
        ClusterElementDefinitionFacade clusterElementDefinitionFacade =
            clusterElementDefinitionFacadeProvider.getIfAvailable();
        ConnectionDefinitionService connectionDefinitionService = connectionDefinitionServiceProvider.getIfAvailable();
        WorkspaceConnectionFacade workspaceConnectionFacade = workspaceConnectionFacadeProvider.getIfAvailable();
        PropertyOptionsResolver propertyOptionsResolver = propertyOptionsResolverProvider.getIfAvailable();

        if (chatService == null || chatToolFacade == null || clusterElementDefinitionService == null ||
            clusterElementDefinitionFacade == null || connectionDefinitionService == null ||
            workspaceConnectionFacade == null || propertyOptionsResolver == null) {

            log.warn("AI Hub chat tools are unavailable: the chat, component and connection services are missing");

            return null;
        }

        return new AiHubChatBindingToolCallbackResolver(
            chatService, chatToolFacade, clusterElementDefinitionService, clusterElementDefinitionFacade,
            connectionDefinitionService,
            new WorkspaceCopilotConnectionLister(workspaceConnectionFacade, propertyOptionsResolver),
            mcpToolCallbackProviderProvider.getIfAvailable());
    }

    @Bean(defaultCandidate = false)
    @Nullable
    ToolSearchToolCallingAdvisor aiHubAskToolSearchToolCallAdvisor(
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        ObjectProvider<ObservationRegistry> observationRegistryProvider,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        ObjectProvider<SecurityContextRehydrator> securityContextRehydratorProvider,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, ObjectProvider<AiHubApprovalGate> approvalGateProvider,
        ObjectProvider<AiHubClusterElementToolCallbacks> clusterElementToolCallbacksProvider,
        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider) {

        return buildModeAdvisor(
            Mode.ASK, toolSearchPgVectorStoreProvider.getIfAvailable(),
            observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP),
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID),
            clusterElementToolCallbacksProvider.getIfAvailable(), securityContextRehydratorProvider.getIfAvailable(),
            toolSearchCatalogWarmup, approvalGateProvider.getIfAvailable(),
            toolSearchCatalogFeederProvider.getIfAvailable());
    }

    @Bean(defaultCandidate = false)
    @Nullable
    ToolSearchToolCallingAdvisor aiHubBuildToolSearchToolCallAdvisor(
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        ObjectProvider<ObservationRegistry> observationRegistryProvider,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        ObjectProvider<SecurityContextRehydrator> securityContextRehydratorProvider,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, ObjectProvider<AiHubApprovalGate> approvalGateProvider,
        ObjectProvider<AiHubClusterElementToolCallbacks> clusterElementToolCallbacksProvider,
        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider) {

        return buildModeAdvisor(
            Mode.BUILD, toolSearchPgVectorStoreProvider.getIfAvailable(),
            observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP),
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID),
            clusterElementToolCallbacksProvider.getIfAvailable(), securityContextRehydratorProvider.getIfAvailable(),
            toolSearchCatalogWarmup, approvalGateProvider.getIfAvailable(),
            toolSearchCatalogFeederProvider.getIfAvailable());
    }

    @Bean
    ToolSearchSessionEvictor toolSearchSessionEvictor(
        @Qualifier("aiHubAskToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> askToolSearchToolCallAdvisorProvider,
        @Qualifier("aiHubBuildToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> buildToolSearchToolCallAdvisorProvider,
        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider) {

        return new ToolSearchSessionEvictor(
            () -> {
                List<ToolSearchToolCallingAdvisor> advisors = new ArrayList<>();

                askToolSearchToolCallAdvisorProvider.ifAvailable(advisors::add);
                buildToolSearchToolCallAdvisorProvider.ifAvailable(advisors::add);

                return advisors;
            },
            toolSearchCatalogFeederProvider::getIfAvailable);
    }

    static @Nullable ToolSearchToolCallingAdvisor buildModeAdvisor(
        Mode mode, @Nullable VectorStore toolSearchPgVectorStore, ObservationRegistry observationRegistry,
        @Nullable AiHubGlobalToolCatalog globalToolCatalog,
        @Nullable AiHubClusterElementToolCallbacks clusterElementToolCallbacks,
        @Nullable SecurityContextRehydrator securityContextRehydrator,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, @Nullable AiHubApprovalGate approvalGate) {

        return buildModeAdvisor(
            mode, toolSearchPgVectorStore, observationRegistry, globalToolCatalog, clusterElementToolCallbacks,
            securityContextRehydrator, toolSearchCatalogWarmup, approvalGate, null);
    }

    static @Nullable ToolSearchToolCallingAdvisor buildModeAdvisor(
        Mode mode, @Nullable VectorStore toolSearchPgVectorStore, ObservationRegistry observationRegistry,
        @Nullable AiHubGlobalToolCatalog globalToolCatalog,
        @Nullable AiHubClusterElementToolCallbacks clusterElementToolCallbacks,
        @Nullable SecurityContextRehydrator securityContextRehydrator,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, @Nullable AiHubApprovalGate approvalGate,
        @Nullable ToolSearchCatalogFeeder toolSearchCatalogFeeder) {

        if (globalToolCatalog == null) {
            return null;
        }

        if (toolSearchPgVectorStore == null) {
            log.warn(
                "AI Hub tool search is unavailable, so the {} catalog tools of session {} and the cluster element " +
                    "tools cannot be reached and the agent runs with its pinned tools only",
                globalToolCatalog.toolCallbacks()
                    .size(),
                globalToolCatalog.sessionId());

            return null;
        }

        ToolIndex searcher = new CatalogToolIndex(
            toolSearchPgVectorStore, searchSessionIds(mode, globalToolCatalog, clusterElementToolCallbacks),
            toolSearchCatalogFeeder == null ? null : threadId -> ToolSearchCatalogFeeder.userSessionId(mode, threadId));

        Supplier<Map<String, ToolCallback>> callbackMapSupplier = MemoizationUtils.memoize(
            () -> catalogCallbackMap(
                mode, globalToolCatalog, clusterElementToolCallbacks, securityContextRehydrator, approvalGate));

        return new PinnedToolSearchToolCallingAdvisor(
            buildToolCallingManager(observationRegistry, callbackMapSupplier), searcher, MAX_SEARCH_RESULTS,
            ChatMemory.CONVERSATION_ID, callbackMapSupplier, toolSearchCatalogWarmup::warmUp,
            userToolIndexer(mode, toolSearchCatalogFeeder));
    }

    static PinnedToolSearchToolCallingAdvisor.UserToolIndexer userToolIndexer(
        Mode mode, @Nullable ToolSearchCatalogFeeder toolSearchCatalogFeeder) {

        if (toolSearchCatalogFeeder == null) {
            return (threadId, toolCallbacks) -> {};
        }

        return (threadId, toolCallbacks) -> toolSearchCatalogFeeder.populateUserTools(
            ToolSearchCatalogFeeder.userSessionId(mode, threadId), toolCallbacks);
    }

    static Map<String, ToolCallback> catalogCallbackMap(
        Mode mode, AiHubGlobalToolCatalog globalToolCatalog,
        @Nullable AiHubClusterElementToolCallbacks clusterElementToolCallbacks,
        @Nullable SecurityContextRehydrator securityContextRehydrator, @Nullable AiHubApprovalGate approvalGate) {

        Map<String, ToolCallback> callbackMap = wrapCatalogToolCallbacks(
            globalToolCatalog.toolCallbacks(), securityContextRehydrator, approvalGate);

        if (clusterElementToolCallbacks == null) {
            return callbackMap;
        }

        Map<String, ClusterElementToolCallback> modeClusterElementToolCallbacks =
            clusterElementToolCallbacks.toolCallbacks(mode);

        for (Map.Entry<String, ClusterElementToolCallback> entry : modeClusterElementToolCallbacks.entrySet()) {
            callbackMap.putIfAbsent(
                entry.getKey(),
                AiHubToolCallbackWrappers.wrap(entry.getValue(), securityContextRehydrator, approvalGate));
        }

        return callbackMap;
    }

    static Set<String> searchSessionIds(
        Mode mode, AiHubGlobalToolCatalog globalToolCatalog,
        @Nullable AiHubClusterElementToolCallbacks clusterElementToolCallbacks) {

        if (clusterElementToolCallbacks == null) {
            return Set.of(globalToolCatalog.sessionId());
        }

        if (mode == Mode.BUILD) {
            return Set.of(
                globalToolCatalog.sessionId(), ToolSearchCatalogFeeder.CLUSTER_ELEMENT_READ_SESSION_ID,
                ToolSearchCatalogFeeder.CLUSTER_ELEMENT_WRITE_SESSION_ID);
        }

        return Set.of(globalToolCatalog.sessionId(), ToolSearchCatalogFeeder.CLUSTER_ELEMENT_READ_SESSION_ID);
    }

    static String embeddingModelIdentity(
        @Nullable ApplicationProperties applicationProperties, PgVectorStoreProperties properties) {

        String dimensions = String.valueOf(properties.getDimensions());

        if (applicationProperties == null) {
            return "unknown:" + dimensions;
        }

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Copilot.Embedding.Provider provider = ai.getCopilot()
            .getEmbedding()
            .getProvider();

        ApplicationProperties.Ai.Provider.Embedding embedding = ai.getProvider()
            .getEmbedding();

        String model = provider == ApplicationProperties.Ai.Copilot.Embedding.Provider.OLLAMA
            ? embedding.getOllama()
                .getOptions()
                .getModel()
            : embedding.getOpenAi()
                .getOptions()
                .getModel();

        return provider + ":" + model + ":" + dimensions;
    }

    static Map<String, ToolCallback> wrapCatalogToolCallbacks(
        List<ToolCallback> toolCallbacks, @Nullable SecurityContextRehydrator securityContextRehydrator,
        @Nullable AiHubApprovalGate approvalGate) {

        Map<String, ToolCallback> callbackMap = new LinkedHashMap<>();

        for (ToolCallback toolCallback : toolCallbacks) {
            ToolCallback wrapped =
                AiHubToolCallbackWrappers.wrap(toolCallback, securityContextRehydrator, approvalGate);

            callbackMap.put(
                wrapped.getToolDefinition()
                    .name(),
                wrapped);
        }

        return callbackMap;
    }

    static ToolCallingManager buildToolCallingManager(
        ObservationRegistry observationRegistry, Supplier<Map<String, ToolCallback>> callbackMapSupplier) {

        return new TurnEndingToolCallingManager(
            new LazyToolCallingManager(
                () -> new UnknownToolRecoveringToolCallingManager(
                    new RunCatalogToolCallingManager(
                        new DefaultToolCallingManager(
                            observationRegistry, new MapToolCallbackResolver(callbackMapSupplier.get()),
                            new DefaultToolExecutionExceptionProcessor(false))))),
            AiHubSpringAIAgent::isTurnEnding);
    }

    private static @Nullable AiHubGlobalToolCatalog findCatalog(
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider, String sessionId) {

        return globalToolCatalogProvider.orderedStream()
            .filter(catalog -> sessionId.equals(catalog.sessionId()))
            .findFirst()
            .orElse(null);
    }
}
