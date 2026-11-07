/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.config.AiHubPgVectorConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.ai.tool.toolsearch.ToolSearchRequest;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
@Testcontainers
class ToolSearchCatalogFeederIntTest {

    private static final String[] KEYWORDS = {
        "component", "task", "dispatcher", "project"
    };

    @Container
    private static final PostgreSQLContainer<?> PGVECTOR_CONTAINER = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"));

    private static JdbcTemplate jdbcTemplate;
    private static PgVectorStoreProperties properties;
    private static PgVectorStore pgVectorStore;

    @BeforeAll
    static void beforeAll() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            PGVECTOR_CONTAINER.getJdbcUrl(), PGVECTOR_CONTAINER.getUsername(), PGVECTOR_CONTAINER.getPassword());

        jdbcTemplate = new JdbcTemplate(dataSource);
        properties = new PgVectorStoreProperties();

        properties.setDimensions(KEYWORDS.length);

        AiHubPgVectorConfiguration.buildToolSearchVectorStore(
            jdbcTemplate, new KeywordEmbeddingModel(), properties, provider(ObservationRegistry.NOOP),
            provider(null), singleBatch(), true)
            .afterPropertiesSet();

        pgVectorStore = AiHubPgVectorConfiguration.buildToolSearchVectorStore(
            jdbcTemplate, new KeywordEmbeddingModel(), properties, provider(ObservationRegistry.NOOP),
            provider(null), singleBatch(), false);
    }

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("DELETE FROM " + qualifiedVectorTableName());
        jdbcTemplate.execute("DROP TABLE IF EXISTS public." + ToolSearchCatalogFeeder.META_TABLE_NAME);
    }

    @Test
    void testPopulateIndexesEveryToolOnce() {
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID,
            List.of(toolCallback("listComponents", "List every component"), toolCallback("getTask", "Get a task")));

        assertThat(rowCount(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID)).isEqualTo(2);
    }

    @Test
    void testUnchangedCatalogIsNotReindexedAfterARestart() {
        List<ToolCallback> toolCallbacks = List.of(toolCallback("listComponents", "List every component"));

        newFeeder().populateGlobalTools(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, toolCallbacks);

        List<String> idsBefore = rowIds(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID);

        newFeeder().populateGlobalTools(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, toolCallbacks);

        assertThat(rowIds(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID)).isEqualTo(idsBefore);
    }

    @Test
    void testChangedCatalogReplacesTheRowsOfAnEarlierRun() {
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
            List.of(toolCallback("listComponents", "List every component"), toolCallback("getTask", "Get a task")));

        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
            List.of(toolCallback("listTaskDispatchers", "List every task dispatcher")));

        assertThat(rowCount(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID)).isEqualTo(1);
    }

    @Test
    void testPopulatingOneModeLeavesTheOtherModeAlone() {
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID,
            List.of(toolCallback("listComponents", "List every component")));
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
            List.of(toolCallback("getTask", "Get a task")));

        assertThat(rowCount(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID)).isEqualTo(1);
        assertThat(rowCount(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID)).isEqualTo(1);
    }

    @Test
    void testSearchFindsCatalogAndConversationToolsButNotTheOtherMode() {
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID,
            List.of(toolCallback("listComponents", "List every component")));
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
            List.of(toolCallback("searchComponents", "Search for a component")));

        VectorToolIndex vectorToolIndex = new VectorToolIndex(pgVectorStore);

        vectorToolIndex.indexTools(
            "conversation-1",
            List.of(ToolReference.builder()
                .toolName("getAction")
                .summary("Get a component action")
                .build()));

        MultiSessionToolIndex multiSessionToolIndex = new MultiSessionToolIndex(
            vectorToolIndex, pgVectorStore, Set.of(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID));

        List<String> toolNames = multiSessionToolIndex.search(
            new ToolSearchRequest("conversation-1", "component", 5, null))
            .toolReferences()
            .stream()
            .map(ToolReference::toolName)
            .toList();

        assertThat(toolNames).containsExactlyInAnyOrder("listComponents", "getAction");
    }

    private static ToolSearchCatalogFeeder newFeeder() {
        return new ToolSearchCatalogFeeder(
            new VectorToolIndex(pgVectorStore), jdbcTemplate, properties.getSchemaName(),
            AiHubPgVectorConfiguration.vectorTableName(properties));
    }

    private static String qualifiedVectorTableName() {
        return properties.getSchemaName() + "." + AiHubPgVectorConfiguration.vectorTableName(properties);
    }

    private static int rowCount(String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + qualifiedVectorTableName() + " WHERE metadata->>'sessionId' = ?", Integer.class,
            sessionId);

        return count == null ? 0 : count;
    }

    private static List<String> rowIds(String sessionId) {
        return jdbcTemplate.queryForList(
            "SELECT id FROM " + qualifiedVectorTableName() + " WHERE metadata->>'sessionId' = ? ORDER BY id",
            String.class, sessionId);
    }

    private static ToolCallback toolCallback(String name, String description) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition())
            .thenReturn(ToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build());

        return toolCallback;
    }

    private static BatchingStrategy singleBatch() {
        return documents -> List.of(documents);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable(any()))
            .thenAnswer(invocation -> value);
        when(objectProvider.getIfUnique(any()))
            .thenAnswer(invocation -> value);

        return objectProvider;
    }

    private static final class KeywordEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            List<String> instructions = request.getInstructions();

            for (int index = 0; index < instructions.size(); index++) {
                embeddings.add(new Embedding(embed(instructions.get(index)), index));
            }

            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        @Override
        public float[] embed(String text) {
            String lowerCaseText = text == null ? "" : text.toLowerCase(Locale.ROOT);
            float[] vector = new float[KEYWORDS.length];
            float sumOfSquares = 0;

            for (int index = 0; index < KEYWORDS.length; index++) {
                vector[index] = lowerCaseText.contains(KEYWORDS[index]) ? 1F : 0.01F;
                sumOfSquares += vector[index] * vector[index];
            }

            float norm = (float) Math.sqrt(sumOfSquares);

            for (int index = 0; index < vector.length; index++) {
                vector[index] = vector[index] / norm;
            }

            return vector;
        }

        @Override
        public int dimensions() {
            return KEYWORDS.length;
        }
    }
}
