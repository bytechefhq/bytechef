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

import com.bytechef.ee.ai.hub.chat.AiHubChatDeletedEvent;
import com.bytechef.ee.ai.hub.config.AiHubPgVectorConfiguration;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
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
    void testSearchFindsTheModeCatalogToolsButNotTheOtherMode() {
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID,
            List.of(toolCallback("listComponents", "List every component")));
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
            List.of(toolCallback("searchComponents", "Search for a component")));

        CatalogToolIndex catalogToolIndex = new CatalogToolIndex(
            pgVectorStore, Set.of(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID));

        List<String> toolNames = catalogToolIndex.search(
            new ToolSearchRequest("conversation-1", "component", 5, null))
            .toolReferences()
            .stream()
            .map(ToolReference::toolName)
            .toList();

        assertThat(toolNames).containsExactly("listComponents");
    }

    @Test
    void testChangedEmbeddingModelReindexesAnUnchangedCatalog() {
        List<ToolCallback> toolCallbacks = List.of(toolCallback("listComponents", "List every component"));

        newFeeder().populateGlobalTools(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, toolCallbacks);

        List<String> idsBefore = rowIds(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID);

        newFeeder("OLLAMA:nomic-embed-text:4").populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, toolCallbacks);

        assertThat(rowIds(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID)).hasSize(1)
            .doesNotContainAnyElementsOf(idsBefore);
    }

    @Test
    void testATurnWritesNoRowsForItsConversation() {
        ToolSearchToolCallingAdvisor advisor = ToolSearchAdvisorConfiguration.buildModeAdvisor(
            Mode.BUILD, pgVectorStore, ObservationRegistry.NOOP,
            new AiHubGlobalToolCatalog(
                ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
                List.of(toolCallback("listComponents", "List every component"))),
            null, null, new ToolSearchCatalogWarmup(null, List.of()), null);

        assertThat(advisor).isInstanceOf(PinnedToolSearchToolCallingAdvisor.class);

        ChatClientRequest chatClientRequest = ChatClientRequest.builder()
            .prompt(
                new Prompt(
                    List.of(new SystemMessage("system"), new UserMessage("hi")),
                    ToolCallingChatOptions.builder()
                        .toolCallbacks(
                            toolCallback("listProjects", "List every project"),
                            toolCallback("getWorkflow", "Get a workflow"))
                        .build()))
            .context(ChatMemory.CONVERSATION_ID, "conversation-1")
            .build();

        ((PinnedToolSearchToolCallingAdvisor) advisor).doInitializeLoop(
            chatClientRequest, mock(CallAdvisorChain.class));

        assertThat(rowCount("conversation-1")).isZero();
        assertThat(rowCount(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID)).isZero();
    }

    @Test
    void testDeletingAChatClearsTheRowsOfItsSession() {
        new VectorToolIndex(pgVectorStore).indexTools(
            "conversation-1",
            List.of(
                ToolReference.builder()
                    .toolName("getAction")
                    .summary("Get a component action")
                    .build()));
        new VectorToolIndex(pgVectorStore).indexTools(
            "conversation-2",
            List.of(
                ToolReference.builder()
                    .toolName("getAction")
                    .summary("Get a component action")
                    .build()));
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
            List.of(toolCallback("listComponents", "List every component")));

        ToolSearchToolCallingAdvisor advisor = ToolSearchAdvisorConfiguration.buildModeAdvisor(
            Mode.BUILD, pgVectorStore, ObservationRegistry.NOOP,
            new AiHubGlobalToolCatalog(
                ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
                List.of(toolCallback("listComponents", "List every component"))),
            null, null, new ToolSearchCatalogWarmup(null, List.of()), null);

        new ToolSearchSessionEvictor(() -> List.of(advisor))
            .onChatDeleted(new AiHubChatDeletedEvent(1L, "conversation-1"));

        assertThat(rowCount("conversation-1")).isZero();
        assertThat(rowCount("conversation-2")).isEqualTo(1);
        assertThat(rowCount(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID)).isEqualTo(1);
    }

    @Test
    void testAThreadsChatToolsAreFoundOnlyByThatThreadsSearch() {
        String threadOneSessionId = ToolSearchCatalogFeeder.userSessionId(Mode.BUILD, "thread-1");

        newFeeder().populateUserTools(
            threadOneSessionId, List.of(toolCallback("search_docs", "Search the project docs")));
        newFeeder().populateGlobalTools(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
            List.of(toolCallback("listComponents", "List every component")));

        CatalogToolIndex catalogToolIndex = new CatalogToolIndex(
            pgVectorStore, Set.of(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID),
            threadId -> ToolSearchCatalogFeeder.userSessionId(Mode.BUILD, threadId));

        assertThat(searchToolNames(catalogToolIndex, "thread-1", "project")).containsExactly("search_docs");
        assertThat(searchToolNames(catalogToolIndex, "thread-2", "project")).isEmpty();
        assertThat(searchToolNames(catalogToolIndex, "thread-2", "component")).containsExactly("listComponents");
    }

    @Test
    void testUnchangedChatToolsAreNotReembeddedAndRemovingThemClearsTheSession() {
        String sessionId = ToolSearchCatalogFeeder.userSessionId(Mode.ASK, "thread-1");
        List<ToolCallback> toolCallbacks = List.of(toolCallback("search_docs", "Search the project docs"));

        newFeeder().populateUserTools(sessionId, toolCallbacks);

        List<String> idsBefore = rowIds(sessionId);

        newFeeder().populateUserTools(sessionId, toolCallbacks);

        assertThat(rowIds(sessionId)).isEqualTo(idsBefore)
            .hasSize(1);

        newFeeder().populateUserTools(sessionId, List.of());

        assertThat(rowCount(sessionId)).isZero();
    }

    @Test
    void testAThreadWithoutChatToolsWritesNoMetaRow() {
        String sessionId = ToolSearchCatalogFeeder.userSessionId(Mode.ASK, "thread-1");

        newFeeder().populateUserTools(sessionId, List.of());

        Integer metaRows = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM public." + ToolSearchCatalogFeeder.META_TABLE_NAME + " WHERE session_id = ?",
            Integer.class, sessionId);

        assertThat(metaRows).isZero();
    }

    @Test
    void testDeletingAChatClearsItsChatToolSessionsInBothModes() {
        ToolSearchCatalogFeeder toolSearchCatalogFeeder = newFeeder();

        toolSearchCatalogFeeder.populateUserTools(
            ToolSearchCatalogFeeder.userSessionId(Mode.ASK, "thread-1"),
            List.of(toolCallback("search_docs", "Search the project docs")));
        toolSearchCatalogFeeder.populateUserTools(
            ToolSearchCatalogFeeder.userSessionId(Mode.BUILD, "thread-1"),
            List.of(toolCallback("create_page", "Create a project page")));
        toolSearchCatalogFeeder.populateUserTools(
            ToolSearchCatalogFeeder.userSessionId(Mode.BUILD, "thread-2"),
            List.of(toolCallback("create_page", "Create a project page")));

        new ToolSearchSessionEvictor(List::of, () -> toolSearchCatalogFeeder)
            .onChatDeleted(new AiHubChatDeletedEvent(1L, "thread-1"));

        assertThat(rowCount(ToolSearchCatalogFeeder.userSessionId(Mode.ASK, "thread-1"))).isZero();
        assertThat(rowCount(ToolSearchCatalogFeeder.userSessionId(Mode.BUILD, "thread-1"))).isZero();
        assertThat(rowCount(ToolSearchCatalogFeeder.userSessionId(Mode.BUILD, "thread-2"))).isEqualTo(1);

        toolSearchCatalogFeeder.populateUserTools(
            ToolSearchCatalogFeeder.userSessionId(Mode.ASK, "thread-1"),
            List.of(toolCallback("search_docs", "Search the project docs")));

        assertThat(rowCount(ToolSearchCatalogFeeder.userSessionId(Mode.ASK, "thread-1"))).isEqualTo(1);
    }

    private static List<String> searchToolNames(CatalogToolIndex catalogToolIndex, String threadId, String query) {
        return catalogToolIndex.search(new ToolSearchRequest(threadId, query, 5, null))
            .toolReferences()
            .stream()
            .map(ToolReference::toolName)
            .toList();
    }

    private static ToolSearchCatalogFeeder newFeeder() {
        return newFeeder("OPENAI:text-embedding-3-small:4");
    }

    private static ToolSearchCatalogFeeder newFeeder(String embeddingModelIdentity) {
        return new ToolSearchCatalogFeeder(
            mock(ClusterElementDefinitionService.class), new VectorToolIndex(pgVectorStore), embeddingModelIdentity,
            jdbcTemplate, properties.getSchemaName(),
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
