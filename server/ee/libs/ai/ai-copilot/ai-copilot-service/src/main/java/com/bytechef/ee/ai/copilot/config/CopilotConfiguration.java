/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.anthropic.client.AnthropicClient;
import com.bytechef.ai.mcp.tool.automation.impl.ClusterElementTools;
import com.bytechef.ai.mcp.tool.automation.impl.ProjectToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ProjectWorkflowToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ReadProjectToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ReadProjectWorkflowToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ScriptTools;
import com.bytechef.ai.mcp.tool.platform.ComponentTools;
import com.bytechef.ai.mcp.tool.platform.FirecrawlTools;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Anthropic;
import com.bytechef.config.ApplicationProperties.Ai.OpenAi;
import com.bytechef.ee.ai.copilot.agent.ClusterElementSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.CodeEditorSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.WorkflowEditorSpringAIAgent;
import com.bytechef.ee.ai.copilot.model.SafeAnthropicChatModel;
import com.bytechef.ee.ai.copilot.util.Mode;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotConfiguration {

    private final String anthropicChatModel;
    private final Double anthropicChatTemperature;
    private final String openAiChatModel;
    private final Double openAiChatTemperature;
    private final String openAiChatReasoningEffort;
    private final String openAiChatVerbosity;
    private final Resource promptWorkflowEditorAskResource;
    private final Resource promptWorkflowEditorBuildResource;
    private final Resource promptCodeEditorAskResource;
    private final Resource promptCodeEditorBuildResource;
    private final Resource promptClusterElementAskResource;
    private final Resource promptClusterElementBuildResource;
    private final State state = new State();

    @SuppressFBWarnings("EI")
    public CopilotConfiguration(
        ApplicationProperties applicationProperties,
        @Value("classpath:prompt_workflow_editor_ask.txt") Resource promptWorkflowEditorAskResource,
        @Value("classpath:prompt_workflow_editor_build.txt") Resource promptWorkflowEditorBuildResource,
        @Value("classpath:prompt_code_editor_ask.txt") Resource promptCodeEditorAskResource,
        @Value("classpath:prompt_code_editor_build.txt") Resource promptCodeEditorBuildResource,
        @Value("classpath:prompt_cluster_element_ask.txt") Resource promptClusterElementAskResource,
        @Value("classpath:prompt_cluster_element_build.txt") Resource promptClusterElementBuildResource) {

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        Anthropic anthropic = ai.getAnthropic();

        Anthropic.Chat.Options anthropicChatOptions = anthropic.getChat()
            .getOptions();

        this.anthropicChatModel = anthropicChatOptions.getModel();
        this.anthropicChatTemperature = anthropicChatOptions.getTemperature();

        OpenAi openAi = ai.getOpenAi();

        OpenAi.Chat.Options openAiChatOptions = openAi.getChat()
            .getOptions();

        this.openAiChatModel = openAiChatOptions.getModel();
        this.openAiChatTemperature = openAiChatOptions.getTemperature();
        this.openAiChatReasoningEffort = openAiChatOptions.getReasoningEffect()
            .name()
            .toLowerCase();
        this.openAiChatVerbosity = openAiChatOptions.getVerbosity()
            .name()
            .toLowerCase();
        this.promptWorkflowEditorAskResource = promptWorkflowEditorAskResource;
        this.promptWorkflowEditorBuildResource = promptWorkflowEditorBuildResource;
        this.promptCodeEditorAskResource = promptCodeEditorAskResource;
        this.promptCodeEditorBuildResource = promptCodeEditorBuildResource;
        this.promptClusterElementAskResource = promptClusterElementAskResource;
        this.promptClusterElementBuildResource = promptClusterElementBuildResource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "anthropic")
    ChatModel anthropicChatModel(AnthropicClient anthropicClient) {
        AnthropicChatModel delegate = AnthropicChatModel.builder()
            .anthropicClient(anthropicClient)
            .options(
                AnthropicChatOptions.builder()
                    .model(anthropicChatModel)
                    .temperature(anthropicChatTemperature)
                    .maxTokens(64000)
                    .build())
            .toolCallingManager(
                ToolCallingManager.builder()
                    .build())
            .observationRegistry(ObservationRegistry.NOOP)
            .build();

        return new SafeAnthropicChatModel(delegate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot.memory", name = "provider", havingValue = "in_memory")
    ChatMemory inMemoryChatMemory() {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(500)
            .build();
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowToolsImpl readProjectWorkflowToolsImpl,
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools) throws AGUIException {
        String name = Source.CODE_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowToolsImpl, componentTools));

        firecrawlTools.ifPresent(tools::add);

        return CodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorAskResource))
            .tools(tools)
            .state(state)
            .build();
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowToolsImpl readProjectWorkflowToolsImpl,
        ComponentTools componentTools)
        throws AGUIException {
        String name = Source.CODE_EDITOR.name() + "_" + Mode.BUILD.name();

        return CodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorBuildResource))
            .tools(List.of(readProjectWorkflowToolsImpl, scriptTools, componentTools))
            .state(state)
            .build();
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowToolsImpl readProjectWorkflowToolsImpl,
        ComponentTools componentTools, TaskTools taskTools) throws AGUIException {

        String name = Source.CLUSTER_ELEMENT.name() + "_" + Mode.ASK.name();

        return ClusterElementSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementAskResource))
            .tools(List.of(readProjectWorkflowToolsImpl, componentTools, taskTools))
            .state(state)
            .build();
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowToolsImpl readProjectWorkflowToolsImpl, ComponentTools componentTools, TaskTools taskTools)
        throws AGUIException {

        String name = Source.CLUSTER_ELEMENT.name() + "_" + Mode.BUILD.name();

        return ClusterElementSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementBuildResource))
            .tools(List.of(readProjectWorkflowToolsImpl, clusterElementTools, componentTools, taskTools))
            .state(state)
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
                    .reasoningEffort(openAiChatReasoningEffort)
                    .verbosity(openAiChatVerbosity)
                    .build())
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot.memory", name = "provider", havingValue = "jdbc")
    ChatMemory jdbcChatMemory(JdbcTemplate jdbcTemplate) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(JdbcChatMemoryRepository
                .builder()
                .jdbcTemplate(jdbcTemplate)
                .dialect(JdbcChatMemoryRepositoryDialect.from(jdbcTemplate.getDataSource()))
                .dataSource(jdbcTemplate.getDataSource())
                .build())
            .maxMessages(500)
            .build();
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore copilotPgVectorStore) {
        return QuestionAnswerAdvisor.builder(copilotPgVectorStore)
            .build();
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectToolsImpl readProjectToolsImpl,
        ReadProjectWorkflowToolsImpl readProjectWorkflowToolsImpl, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, QuestionAnswerAdvisor questionAnswerAdvisor)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(readProjectToolsImpl, readProjectWorkflowToolsImpl, componentTools, taskTools));

        firecrawlTools.ifPresent(tools::add);

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorAskResource))
            .state(state)
            .tools(tools)
            .advisor(questionAnswerAdvisor)
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .build();
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectToolsImpl projectToolsImpl,
        ProjectWorkflowToolsImpl projectWorkflowToolsImpl, TaskTools taskTools, ScriptTools scriptTools,
        WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name() + "_" + Mode.BUILD.name();

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorBuildResource))
            .state(state)
            .tools(List.of(projectToolsImpl, projectWorkflowToolsImpl, taskTools, scriptTools))
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .build();
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
