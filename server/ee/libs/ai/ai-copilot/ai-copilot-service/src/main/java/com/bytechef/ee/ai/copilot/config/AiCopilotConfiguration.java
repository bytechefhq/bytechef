/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.mcp.tool.automation.impl.ProjectToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ProjectWorkflowToolsImpl;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Copilot.Anthropic;
import com.bytechef.config.ApplicationProperties.Ai.Copilot.OpenAi;
import com.bytechef.config.ApplicationProperties.Ai.Copilot.Vectorstore;
import com.bytechef.ee.ai.copilot.agent.CodeEditorSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.WorkflowEditorSpringAIAgent;
import com.bytechef.ee.ai.copilot.util.Source;
import com.github.mizosoft.methanol.Methanol;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotConfiguration {

    private final String anthropicApiKey;
    private final String anthropicChatModel;
    private final Double anthropicChatTemperature;
    private final String anthropicEmbeddingModel;
    private final String openAiApiKey;
    private final String openAiChatModel;
    private final Double openAiChatTemperature;
    private final String openAiEmbeddingModel;
    private final Vectorstore.PgVector pgVector;
    private final Resource systemPromptResource;

    @SuppressFBWarnings("EI")
    public AiCopilotConfiguration(
        ApplicationProperties applicationProperties,
        @Value("classpath:system_prompt.txt") Resource systemPromptResource) {

        ApplicationProperties.Ai.Copilot copilot = applicationProperties.getAi()
            .getCopilot();

        Anthropic anthropic = copilot.getAnthropic();

        this.anthropicApiKey = anthropic.getApiKey();

        Anthropic.Chat.Options anthropicChatOptions = anthropic.getChat()
            .getOptions();

        this.anthropicChatModel = anthropicChatOptions.getModel();
        this.anthropicChatTemperature = anthropicChatOptions.getTemperature();

        Anthropic.Embedding.OpenAi.Options anthropicEmbeddingOpenAiOptions = anthropic.getEmbedding()
            .getOpenAi()
            .getOptions();

        this.anthropicEmbeddingModel = anthropicEmbeddingOpenAiOptions.getModel();

        OpenAi openAi = copilot.getOpenAi();

        this.openAiApiKey = openAi.getApiKey();

        OpenAi.Chat.Options openAiChatOptions = openAi.getChat()
            .getOptions();

        this.openAiChatModel = openAiChatOptions.getModel();
        this.openAiChatTemperature = openAiChatOptions.getTemperature();

        OpenAi.Embedding.Options openAiEmbeddingOptions = openAi.getEmbedding()
            .getOptions();

        this.openAiEmbeddingModel = openAiEmbeddingOptions.getModel();

        Vectorstore vectorstore = copilot.getVectorstore();

        this.pgVector = vectorstore.getPgVector();

        this.systemPromptResource = systemPromptResource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "anthropic")
    AnthropicApi anthropicApi() {
        return AnthropicApi.builder()
            .apiKey(anthropicApiKey)
            .restClientBuilder(getRestClientBuilder())
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "anthropic")
    AnthropicChatModel anthropicChatModel() {
        return AnthropicChatModel.builder()
            .anthropicApi(anthropicApi())
            .defaultOptions(
                AnthropicChatOptions.builder()
                    .model(anthropicChatModel)
                    .temperature(anthropicChatTemperature)
                    .maxTokens(64000)
                    .build())
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "anthropic")
    OpenAiEmbeddingModel anthropicOpenAiEmbeddingModel(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingModel(
            openAiApi,
            MetadataMode.ALL,
            OpenAiEmbeddingOptions.builder()
                .model(anthropicEmbeddingModel)
                .build());
    }

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(500)
            .build();
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorSpringAIAgent(ChatMemory chatMemory, ChatModel chatModel) throws AGUIException {
        String name = Source.CODE_EDITOR.name();

        return CodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .state(new State())
            .build();
    }

    @Bean
    OpenAiApi openAiApi() {
        return OpenAiApi.builder()
            .apiKey(openAiApiKey)
            .restClientBuilder(getRestClientBuilder())
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "openai")
    OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model(openAiChatModel)
                    .temperature(openAiChatTemperature)
                    .build())
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "openai")
    OpenAiEmbeddingModel openAiEmbeddingModel(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingModel(
            openAiApi,
            MetadataMode.ALL,
            OpenAiEmbeddingOptions.builder()
                .model(openAiEmbeddingModel)
                .build());
    }

    DataSource pgVectorDataSource() {
        return DataSourceBuilder.create()
            .type(org.postgresql.ds.PGSimpleDataSource.class)
            .url(pgVector.getUrl())
            .username(pgVector.getUsername())
            .password(pgVector.getPassword())
            .build();
    }

    JdbcTemplate pgVectorJdbcTemplate() {
        return new JdbcTemplate(pgVectorDataSource());
    }

    @Bean
    public PgVectorStore vectorStore(
        EmbeddingModel embeddingModel, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        var initializeSchema = properties.isInitializeSchema();

        return PgVectorStore.builder(pgVectorJdbcTemplate(), embeddingModel)
            .schemaName(properties.getSchemaName())
            .idType(properties.getIdType())
            .vectorTableName(properties.getTableName())
            .vectorTableValidationsEnabled(properties.isSchemaValidation())
            .dimensions(properties.getDimensions())
            .distanceType(properties.getDistanceType())
            .removeExistingVectorStoreTable(properties.isRemoveExistingVectorStoreTable())
            .indexType(properties.getIndexType())
            .initializeSchema(initializeSchema)
            .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
            .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
            .batchingStrategy(batchingStrategy)
            .maxDocumentBatchSize(properties.getMaxDocumentBatchSize())
            .build();
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectToolsImpl projectTools,
        ProjectWorkflowToolsImpl projectWorkflowTools, TaskTools taskTools, WorkflowService workflowService)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name();

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(systemPromptResource))
            .state(new State())
            .tools(new ArrayList<>(List.of(projectTools, projectWorkflowTools, taskTools)))
            .workflowService(workflowService)
            .build();
    }

    private static RestClient.Builder getRestClientBuilder() {
        HttpClient httpClient = Methanol.newBuilder()
            .autoAcceptEncoding(true)
            .connectTimeout(Duration.ofSeconds(60))
            .defaultHeaders(httpHeaders -> {
                httpHeaders.setHeader("Accept-Encoding", "gzip, deflate");
            })
            .headersTimeout(Duration.ofSeconds(60))
            .readTimeout(Duration.ofSeconds(60))
            .requestTimeout(Duration.ofSeconds(60))
            .build();

        JdkClientHttpRequestFactory jdkClientHttpRequestFactory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
            .requestFactory(jdkClientHttpRequestFactory);
    }

    private String getSystemPrompt(Resource systemPromptResource) {
        try {
            InputStream inputStream = systemPromptResource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), e);
        }
    }
}
