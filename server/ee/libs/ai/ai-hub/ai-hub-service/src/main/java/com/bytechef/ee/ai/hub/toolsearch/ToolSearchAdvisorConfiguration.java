/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.agui.spring.ai.TurnEndingToolCallingManager;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.commons.util.MemoizationUtils;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.AiHubToolCallbackWrappers;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.config.AiHubPgVectorConfiguration;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import io.micrometer.observation.ObservationRegistry;
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
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
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
    VectorToolIndex toolSearchVectorToolIndex(
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider) {

        VectorStore toolSearchPgVectorStore = toolSearchPgVectorStoreProvider.getIfAvailable();

        return toolSearchPgVectorStore == null ? null : new VectorToolIndex(toolSearchPgVectorStore);
    }

    @Bean
    @Nullable
    ToolSearchCatalogFeeder toolSearchCatalogFeeder(
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        @Qualifier("pgVectorJdbcTemplate") ObjectProvider<JdbcTemplate> pgVectorJdbcTemplateProvider,
        @Qualifier("copilotEmbeddingModel") ObjectProvider<EmbeddingModel> copilotEmbeddingModelProvider,
        PgVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        ObjectProvider<BatchingStrategy> batchingStrategyProvider) {

        JdbcTemplate pgVectorJdbcTemplate = pgVectorJdbcTemplateProvider.getIfAvailable();
        EmbeddingModel copilotEmbeddingModel = copilotEmbeddingModelProvider.getIfAvailable();
        BatchingStrategy batchingStrategy = batchingStrategyProvider.getIfAvailable();

        if (toolSearchPgVectorStoreProvider.getIfAvailable() == null || pgVectorJdbcTemplate == null ||
            copilotEmbeddingModel == null || batchingStrategy == null) {

            log.info(
                "AI Hub tool catalog indexing disabled: it needs the tool search pgvector store and a fixed-key " +
                    "copilot embedding model (bytechef.ai.copilot.embedding.provider)");

            return null;
        }

        VectorStore loaderVectorStore = AiHubPgVectorConfiguration.buildToolSearchVectorStore(
            pgVectorJdbcTemplate, copilotEmbeddingModel, properties, observationRegistry, customObservationConvention,
            batchingStrategy, false);

        return new ToolSearchCatalogFeeder(
            new VectorToolIndex(loaderVectorStore), pgVectorJdbcTemplate, properties.getSchemaName(),
            AiHubPgVectorConfiguration.vectorTableName(properties));
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

    @Bean(defaultCandidate = false)
    @Nullable
    ToolSearchToolCallingAdvisor aiHubAskToolSearchToolCallAdvisor(
        ObjectProvider<VectorToolIndex> toolSearchVectorToolIndexProvider,
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        ObjectProvider<ObservationRegistry> observationRegistryProvider,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        ObjectProvider<SecurityContextRehydrator> securityContextRehydratorProvider,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, ObjectProvider<AiHubApprovalGate> approvalGateProvider) {

        return buildModeAdvisor(
            toolSearchVectorToolIndexProvider.getIfAvailable(), toolSearchPgVectorStoreProvider.getIfAvailable(),
            observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP),
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID),
            securityContextRehydratorProvider.getIfAvailable(), toolSearchCatalogWarmup,
            approvalGateProvider.getIfAvailable());
    }

    @Bean(defaultCandidate = false)
    @Nullable
    ToolSearchToolCallingAdvisor aiHubBuildToolSearchToolCallAdvisor(
        ObjectProvider<VectorToolIndex> toolSearchVectorToolIndexProvider,
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        ObjectProvider<ObservationRegistry> observationRegistryProvider,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        ObjectProvider<SecurityContextRehydrator> securityContextRehydratorProvider,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, ObjectProvider<AiHubApprovalGate> approvalGateProvider) {

        return buildModeAdvisor(
            toolSearchVectorToolIndexProvider.getIfAvailable(), toolSearchPgVectorStoreProvider.getIfAvailable(),
            observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP),
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID),
            securityContextRehydratorProvider.getIfAvailable(), toolSearchCatalogWarmup,
            approvalGateProvider.getIfAvailable());
    }

    static @Nullable ToolSearchToolCallingAdvisor buildModeAdvisor(
        @Nullable VectorToolIndex vectorToolIndex, @Nullable VectorStore toolSearchPgVectorStore,
        ObservationRegistry observationRegistry, @Nullable AiHubGlobalToolCatalog globalToolCatalog,
        @Nullable SecurityContextRehydrator securityContextRehydrator,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, @Nullable AiHubApprovalGate approvalGate) {

        if (vectorToolIndex == null || toolSearchPgVectorStore == null || globalToolCatalog == null) {
            return null;
        }

        ToolIndex searcher = new MultiSessionToolIndex(
            vectorToolIndex, toolSearchPgVectorStore, Set.of(globalToolCatalog.sessionId()));

        Supplier<Map<String, ToolCallback>> callbackMapSupplier = MemoizationUtils.memoize(
            () -> wrapCatalogToolCallbacks(globalToolCatalog.toolCallbacks(), securityContextRehydrator, approvalGate));

        return new PinnedToolSearchToolCallingAdvisor(
            buildToolCallingManager(observationRegistry, callbackMapSupplier), searcher, MAX_SEARCH_RESULTS,
            ChatMemory.CONVERSATION_ID, callbackMapSupplier, toolSearchCatalogWarmup::warmUp);
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
                    new DefaultToolCallingManager(
                        observationRegistry, new MapToolCallbackResolver(callbackMapSupplier.get()),
                        new DefaultToolExecutionExceptionProcessor(false)))),
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
