/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.ee.ai.copilot.agent.ClusterElementSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.CodeEditorSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.ConverterSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.CopilotChatClientResolver;
import com.bytechef.ee.ai.copilot.agent.SkillsSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.WorkflowEditorSpringAIAgent;
import com.bytechef.ee.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ee.ai.copilot.tool.AskUserQuestionToolCallback;
import com.bytechef.ee.ai.copilot.tool.CreateConnectionToolCallback;
import com.bytechef.ee.ai.copilot.tool.ListConnectionsForComponentToolCallback;
import com.bytechef.ee.ai.copilot.tool.LookupActionPropertyOptionsToolCallback;
import com.bytechef.ee.ai.copilot.tool.LookupTriggerPropertyOptionsToolCallback;
import com.bytechef.ee.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ee.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ee.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ee.ai.copilot.tool.SelectConnectionToolCallback;
import com.bytechef.ee.ai.copilot.tool.SelectPropertyOptionToolCallback;
import com.bytechef.ee.ai.copilot.tool.SelectTriggerPropertyOptionToolCallback;
import com.bytechef.ee.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.ee.ai.copilot.tool.WorkspaceCopilotConnectionLister;
import com.bytechef.ee.ai.copilot.util.Mode;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.ee.automation.ai.tool.ReadSkillsTools;
import com.bytechef.ee.automation.ai.tool.SkillsTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.FirecrawlTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotConfiguration {

    private final Resource promptWorkflowEditorAskResource;
    private final Resource promptWorkflowEditorBuildResource;
    private final Resource promptCodeEditorAskResource;
    private final Resource promptCodeEditorBuildResource;
    private final Resource promptConverterBuildResource;
    private final Resource promptClusterElementAskResource;
    private final Resource promptClusterElementBuildResource;
    private final Resource promptSkillsAskResource;
    private final Resource promptSkillsBuildResource;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final State state = new State();

    private final ConnectionDefinitionService connectionDefinitionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final PropertyOptionsResolver propertyOptionsResolver;
    private final ObjectProvider<CopilotConnectionLister> connectionListerProvider;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI")
    public CopilotConfiguration(
        @Value("classpath:prompt_workflow_editor_ask.txt") Resource promptWorkflowEditorAskResource,
        @Value("classpath:prompt_workflow_editor_build.txt") Resource promptWorkflowEditorBuildResource,
        @Value("classpath:prompt_code_editor_ask.txt") Resource promptCodeEditorAskResource,
        @Value("classpath:prompt_code_editor_build.txt") Resource promptCodeEditorBuildResource,
        @Value("classpath:prompt_converter_build.txt") Resource promptConverterBuildResource,
        @Value("classpath:prompt_cluster_element_ask.txt") Resource promptClusterElementAskResource,
        @Value("classpath:prompt_cluster_element_build.txt") Resource promptClusterElementBuildResource,
        @Value("classpath:prompt_skills_ask.txt") Resource promptSkillsAskResource,
        @Value("classpath:prompt_skills_build.txt") Resource promptSkillsBuildResource,
        WorkflowValidatorTools workflowValidatorTools, WorkflowInstructionTools workflowInstructionTools,
        ConnectionDefinitionService connectionDefinitionService, WorkspaceConnectionFacade workspaceConnectionFacade,
        ComponentDefinitionService componentDefinitionService, ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, PropertyOptionsResolver propertyOptionsResolver,
        ObjectProvider<CopilotConnectionLister> connectionListerProvider) {

        this.connectionListerProvider = connectionListerProvider;
        this.connectionDefinitionService = connectionDefinitionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
        this.componentDefinitionService = componentDefinitionService;
        this.actionDefinitionService = actionDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.propertyOptionsResolver = propertyOptionsResolver;
        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;
        this.promptWorkflowEditorAskResource = promptWorkflowEditorAskResource;
        this.promptWorkflowEditorBuildResource = promptWorkflowEditorBuildResource;
        this.promptCodeEditorAskResource = promptCodeEditorAskResource;
        this.promptCodeEditorBuildResource = promptCodeEditorBuildResource;
        this.promptConverterBuildResource = promptConverterBuildResource;
        this.promptClusterElementAskResource = promptClusterElementAskResource;
        this.promptClusterElementBuildResource = promptClusterElementBuildResource;
        this.promptSkillsAskResource = promptSkillsAskResource;
        this.promptSkillsBuildResource = promptSkillsBuildResource;
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {
        String name = Source.CODE_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);

        return CodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.CODE_EDITOR.name() + "_" + Mode.BUILD.name();

        return CodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorBuildResource))
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        readProjectWorkflowTools, scriptTools, componentTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.CLUSTER_ELEMENT.name() + "_" + Mode.ASK.name();

        return ClusterElementSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementAskResource))
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.CLUSTER_ELEMENT.name() + "_" + Mode.BUILD.name();

        return ClusterElementSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementBuildResource))
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        readProjectWorkflowTools, clusterElementTools, componentTools, taskTools,
                        workflowValidatorTools, workflowInstructionTools)))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    private List<ToolCallback> wrapTools(SecurityContextRehydrator securityContextRehydrator, List<Object> tools) {
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

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore copilotPgVectorStore) {
        return QuestionAnswerAdvisor.builder(copilotPgVectorStore)
            .build();
    }

    private List<ToolCallback> interactivePickerToolCallbacks() {
        List<CopilotConnectionLister> connectionListers = new ArrayList<>();

        connectionListers.add(new WorkspaceCopilotConnectionLister(workspaceConnectionFacade, propertyOptionsResolver));

        CopilotConnectionLister embeddedConnectionLister = connectionListerProvider.getIfAvailable();

        if (embeddedConnectionLister != null) {
            connectionListers.add(embeddedConnectionLister);
        }

        return List.of(
            new ListConnectionsForComponentToolCallback(
                componentDefinitionService, connectionDefinitionService, ToolStateVisibilityMetrics.NOOP,
                connectionListers),
            new SelectConnectionToolCallback(componentDefinitionService),
            new LookupActionPropertyOptionsToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP),
            new LookupTriggerPropertyOptionsToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP),
            new SelectPropertyOptionToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP),
            new SelectTriggerPropertyOptionToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP),
            new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP),
            new CreateConnectionToolCallback(componentDefinitionService));
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, QuestionAnswerAdvisor questionAnswerAdvisor,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);

        tools.addAll(interactivePickerToolCallbacks());

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .advisor(questionAnswerAdvisor)
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowTools projectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        ScriptTools scriptTools, WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<CopilotChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                projectTools, projectWorkflowTools, componentTools, taskTools, scriptTools, workflowValidatorTools,
                workflowInstructionTools));

        tools.addAll(interactivePickerToolCallbacks());

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorBuildResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ConverterSpringAIAgent converterBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        SecurityContextRehydrator securityContextRehydrator)
        throws AGUIException {

        String name = Source.CONVERTER.name() + "_" + Mode.BUILD.name();

        return ConverterSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptConverterBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        projectTools, projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .build();
    }

    @Bean
    SkillsSpringAIAgent skillsAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools,
        SecurityContextRehydrator securityContextRehydrator)
        throws AGUIException {

        String name = Source.SKILLS.name() + "_" + Mode.ASK.name();

        return SkillsSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsAskResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .build();
    }

    @Bean
    SkillsSpringAIAgent skillsBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools,
        SecurityContextRehydrator securityContextRehydrator)
        throws AGUIException {

        String name = Source.SKILLS.name() + "_" + Mode.BUILD.name();

        return SkillsSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        skillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .build();
    }

    @Bean
    ChatClient codeEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptCodeEditorAskResource));

        if (firecrawlTools.isPresent()) {
            builder.defaultTools(
                readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools,
                firecrawlTools.get());
        } else {
            builder.defaultTools(
                readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools);
        }

        return builder.build();
    }

    @Bean
    ChatClient codeEditorBuildSubAgentChatClient(
        ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptCodeEditorBuildResource))
            .defaultTools(
                readProjectWorkflowTools, scriptTools, componentTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient workflowEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, QuestionAnswerAdvisor questionAnswerAdvisor) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptWorkflowEditorAskResource))
            .defaultAdvisors(questionAnswerAdvisor);

        if (firecrawlTools.isPresent()) {
            builder.defaultTools(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools, firecrawlTools.get());
        } else {
            builder.defaultTools(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools);
        }

        return builder.build();
    }

    @Bean
    ChatClient workflowEditorBuildSubAgentChatClient(
        ChatModel chatModel, ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptWorkflowEditorBuildResource))
            .defaultTools(
                projectTools, projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient converterBuildSubAgentChatClient(
        ChatModel chatModel, ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptConverterBuildResource))
            .defaultTools(
                projectTools, projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient clusterElementAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptClusterElementAskResource))
            .defaultTools(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools, workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient clusterElementBuildSubAgentChatClient(
        ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptClusterElementBuildResource))
            .defaultTools(
                readProjectWorkflowTools, clusterElementTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient skillsAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptSkillsAskResource))
            .defaultTools(
                readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    @Bean
    ChatClient skillsBuildSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(getSystemPrompt(promptSkillsBuildResource))
            .defaultTools(
                skillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    private String getSystemPrompt(Resource systemPromptResource) {
        try {
            InputStream inputStream = systemPromptResource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }
}
