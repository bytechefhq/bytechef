/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.WorkflowEditorSpringAIAgent;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.tool.IntegrationTools;
import com.bytechef.ee.embedded.ai.tool.IntegrationWorkflowTools;
import com.bytechef.ee.embedded.ai.tool.ReadIntegrationTools;
import com.bytechef.ee.embedded.ai.tool.ReadIntegrationWorkflowTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class EmbeddedCopilotConfiguration {

    private final State state = new State();
    private final WorkflowInstructionTools workflowInstructionTools;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final String workflowEditorEmbeddedAskSystemPrompt;
    private final String workflowEditorEmbeddedBuildSystemPrompt;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public EmbeddedCopilotConfiguration(
        @Value("classpath:prompt_workflow_editor_embedded_ask.txt") Resource promptWorkflowEditorEmbeddedAskResource,
        @Value("classpath:prompt_workflow_editor_embedded_build.txt") Resource promptWorkflowEditorEmbeddedBuildResource,
        WorkflowInstructionTools workflowInstructionTools, WorkflowValidatorTools workflowValidatorTools) {

        this.workflowInstructionTools = workflowInstructionTools;
        this.workflowValidatorTools = workflowValidatorTools;

        this.workflowEditorEmbeddedAskSystemPrompt = getSystemPrompt(promptWorkflowEditorEmbeddedAskResource);
        this.workflowEditorEmbeddedBuildSystemPrompt = getSystemPrompt(promptWorkflowEditorEmbeddedBuildResource);
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorEmbeddedAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadIntegrationTools readIntegrationTools,
        ReadIntegrationWorkflowTools readIntegrationWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        @Qualifier("questionAnswerAdvisor") Advisor questionAnswerAdvisor, PermissionService permissionService,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readIntegrationTools, readIntegrationWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        WorkflowEditorSpringAIAgent springAIAgent = WorkflowEditorSpringAIAgent.builder()
            .agentId(CopilotAgentType.WORKFLOW_EDITOR_EMBEDDED_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(workflowEditorEmbeddedAskSystemPrompt)
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .advisor(questionAnswerAdvisor)
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .permissionService(permissionService)
            .securityContextRehydrator(securityContextRehydrator)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorEmbeddedBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, IntegrationTools integrationTools,
        IntegrationWorkflowTools integrationWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        PermissionService permissionService, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                integrationTools, integrationWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(CopilotAgentType.WORKFLOW_EDITOR_EMBEDDED_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(workflowEditorEmbeddedBuildSystemPrompt)
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .permissionService(permissionService)
            .securityContextRehydrator(securityContextRehydrator)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ChatClient workflowEditorEmbeddedBuildSubAgentChatClient(
        ChatModel chatModel, IntegrationTools integrationTools, IntegrationWorkflowTools integrationWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return buildWorkflowEditorEmbeddedBuildSubAgentChatClient(
            chatModel, integrationTools, integrationWorkflowTools, componentTools, taskTools);
    }

    @Bean
    IntelligentToolChatClientFactory workflowEditorEmbeddedBuildSubAgentChatClientFactory(
        @Qualifier("workflowEditorEmbeddedBuildSubAgentChatClient") //
        ChatClient workflowEditorEmbeddedBuildSubAgentChatClient,
        IntegrationTools integrationTools, IntegrationWorkflowTools integrationWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return candidateChatModel -> candidateChatModel == null
            ? workflowEditorEmbeddedBuildSubAgentChatClient
            : buildWorkflowEditorEmbeddedBuildSubAgentChatClient(
                candidateChatModel, integrationTools, integrationWorkflowTools, componentTools, taskTools);
    }

    private ChatClient buildWorkflowEditorEmbeddedBuildSubAgentChatClient(
        ChatModel chatModel, IntegrationTools integrationTools, IntegrationWorkflowTools integrationWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(workflowEditorEmbeddedBuildSystemPrompt)
            .defaultTools(
                integrationTools, integrationWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    private static List<ToolCallback> wrapTools(
        SecurityContextRehydrator securityContextRehydrator, List<Object> tools) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (Object tool : tools) {
            if (tool instanceof ToolCallback toolCallback) {
                toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
            } else {
                for (ToolCallback toolCallback : ToolCallbacks.from(tool)) {
                    toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
                }
            }
        }

        return toolCallbacks;
    }

    private static String getSystemPrompt(Resource systemPromptResource) {
        try (InputStream inputStream = systemPromptResource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }
}
