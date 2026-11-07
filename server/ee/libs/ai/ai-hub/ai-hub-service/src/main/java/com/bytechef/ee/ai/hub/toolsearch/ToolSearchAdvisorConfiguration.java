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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.AiHubToolCallbackWrappers;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.config.AiHubPgVectorConfiguration;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
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
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        @Qualifier("pgVectorJdbcTemplate") ObjectProvider<JdbcTemplate> pgVectorJdbcTemplateProvider,
        PgVectorStoreProperties properties, ObjectProvider<ApplicationProperties> applicationPropertiesProvider) {

        VectorStore toolSearchPgVectorStore = toolSearchPgVectorStoreProvider.getIfAvailable();
        JdbcTemplate pgVectorJdbcTemplate = pgVectorJdbcTemplateProvider.getIfAvailable();

        if (toolSearchPgVectorStore == null || pgVectorJdbcTemplate == null) {
            return null;
        }

        return new ToolSearchCatalogFeeder(
            new VectorToolIndex(toolSearchPgVectorStore),
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

    @Bean(defaultCandidate = false)
    @Nullable
    ToolSearchToolCallingAdvisor aiHubAskToolSearchToolCallAdvisor(
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        ObjectProvider<ObservationRegistry> observationRegistryProvider,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        ObjectProvider<SecurityContextRehydrator> securityContextRehydratorProvider,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, ObjectProvider<AiHubApprovalGate> approvalGateProvider) {

        return buildModeAdvisor(
            toolSearchPgVectorStoreProvider.getIfAvailable(),
            observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP),
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID),
            securityContextRehydratorProvider.getIfAvailable(), toolSearchCatalogWarmup,
            approvalGateProvider.getIfAvailable());
    }

    @Bean(defaultCandidate = false)
    @Nullable
    ToolSearchToolCallingAdvisor aiHubBuildToolSearchToolCallAdvisor(
        @Qualifier("toolSearchPgVectorStore") ObjectProvider<VectorStore> toolSearchPgVectorStoreProvider,
        ObjectProvider<ObservationRegistry> observationRegistryProvider,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        ObjectProvider<SecurityContextRehydrator> securityContextRehydratorProvider,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, ObjectProvider<AiHubApprovalGate> approvalGateProvider) {

        return buildModeAdvisor(
            toolSearchPgVectorStoreProvider.getIfAvailable(),
            observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP),
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID),
            securityContextRehydratorProvider.getIfAvailable(), toolSearchCatalogWarmup,
            approvalGateProvider.getIfAvailable());
    }

    @Bean
    ToolSearchSessionEvictor toolSearchSessionEvictor(
        @Qualifier("aiHubAskToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> askToolSearchToolCallAdvisorProvider,
        @Qualifier("aiHubBuildToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> buildToolSearchToolCallAdvisorProvider) {

        return new ToolSearchSessionEvictor(
            () -> {
                List<ToolSearchToolCallingAdvisor> advisors = new ArrayList<>();

                askToolSearchToolCallAdvisorProvider.ifAvailable(advisors::add);
                buildToolSearchToolCallAdvisorProvider.ifAvailable(advisors::add);

                return advisors;
            });
    }

    static @Nullable ToolSearchToolCallingAdvisor buildModeAdvisor(
        @Nullable VectorStore toolSearchPgVectorStore, ObservationRegistry observationRegistry,
        @Nullable AiHubGlobalToolCatalog globalToolCatalog,
        @Nullable SecurityContextRehydrator securityContextRehydrator,
        ToolSearchCatalogWarmup toolSearchCatalogWarmup, @Nullable AiHubApprovalGate approvalGate) {

        if (globalToolCatalog == null) {
            return null;
        }

        if (toolSearchPgVectorStore == null) {
            log.warn(
                "AI Hub tool search is unavailable, so the {} catalog tools of session {} cannot be reached and the " +
                    "agent runs with its pinned tools only",
                globalToolCatalog.toolCallbacks()
                    .size(),
                globalToolCatalog.sessionId());

            return null;
        }

        ToolIndex searcher = new CatalogToolIndex(toolSearchPgVectorStore, Set.of(globalToolCatalog.sessionId()));

        Supplier<Map<String, ToolCallback>> callbackMapSupplier = MemoizationUtils.memoize(
            () -> wrapCatalogToolCallbacks(globalToolCatalog.toolCallbacks(), securityContextRehydrator, approvalGate));

        return new PinnedToolSearchToolCallingAdvisor(
            buildToolCallingManager(observationRegistry, callbackMapSupplier), searcher, MAX_SEARCH_RESULTS,
            ChatMemory.CONVERSATION_ID, callbackMapSupplier, toolSearchCatalogWarmup::warmUp);
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
