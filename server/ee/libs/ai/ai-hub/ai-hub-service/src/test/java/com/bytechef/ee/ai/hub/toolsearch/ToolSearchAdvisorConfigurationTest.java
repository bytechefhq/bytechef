/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.spring.ai.TurnEndingToolCallingManager;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
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
                null, null, ObservationRegistry.NOOP, catalog, null, warmup, null))
                    .isNull();
    }

    @Test
    void testAdvisorIsAbsentWithoutAModeCatalog() {
        assertThat(
            ToolSearchAdvisorConfiguration.buildModeAdvisor(
                mock(VectorToolIndex.class), mock(VectorStore.class), ObservationRegistry.NOOP, null, null, warmup,
                null))
                    .isNull();
    }

    @Test
    void testAdvisorIsBuiltWithoutTouchingTheCatalogTools() {
        ToolCallback catalogTool = toolCallback("listComponents");

        AiHubGlobalToolCatalog catalog = new AiHubGlobalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID, List.of(catalogTool));

        ToolSearchToolCallingAdvisor advisor = ToolSearchAdvisorConfiguration.buildModeAdvisor(
            mock(VectorToolIndex.class), mock(VectorStore.class), ObservationRegistry.NOOP, catalog, null, warmup,
            null);

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
    void testToolCallingManagerEndsTheTurnLikeTheAgent() {
        assertThat(ToolSearchAdvisorConfiguration.buildToolCallingManager(ObservationRegistry.NOOP, Map::of))
            .isInstanceOf(TurnEndingToolCallingManager.class);
    }

    @Test
    void testFeederIsAbsentWithoutTheCopilotEmbeddingModel() {
        ToolSearchCatalogFeeder feeder = configuration.toolSearchCatalogFeeder(
            provider(mock(VectorStore.class)), provider(mock(JdbcTemplate.class)), provider(null),
            new PgVectorStoreProperties(), provider(ObservationRegistry.NOOP),
            provider((VectorStoreObservationConvention) null), provider(mock(BatchingStrategy.class)));

        assertThat(feeder).isNull();
    }

    @Test
    void testFeederIsBuiltWhenEverythingIsPresent() {
        ToolSearchCatalogFeeder feeder = configuration.toolSearchCatalogFeeder(
            provider(mock(VectorStore.class)), provider(mock(JdbcTemplate.class)),
            provider(mock(EmbeddingModel.class)), new PgVectorStoreProperties(), provider(ObservationRegistry.NOOP),
            provider((VectorStoreObservationConvention) null), provider(mock(BatchingStrategy.class)));

        assertThat(feeder).isNotNull();
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
}
