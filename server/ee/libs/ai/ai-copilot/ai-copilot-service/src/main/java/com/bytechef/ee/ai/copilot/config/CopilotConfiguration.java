/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.mcp.tool.automation.ClusterElementTools;
import com.bytechef.ai.mcp.tool.automation.ProjectTools;
import com.bytechef.ai.mcp.tool.automation.ProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.ReadProjectTools;
import com.bytechef.ai.mcp.tool.automation.ReadProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.ReadSkillsTools;
import com.bytechef.ai.mcp.tool.automation.ScriptTools;
import com.bytechef.ai.mcp.tool.automation.SkillsTools;
import com.bytechef.ai.mcp.tool.platform.ComponentTools;
import com.bytechef.ai.mcp.tool.platform.FirecrawlTools;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.ai.mcp.tool.platform.WorkflowInstructionTools;
import com.bytechef.ai.mcp.tool.platform.WorkflowValidatorTools;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.ai.copilot.agent.ClusterElementSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.CodeEditorSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.ConverterSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.SkillsSpringAIAgent;
import com.bytechef.ee.ai.copilot.agent.WorkflowEditorSpringAIAgent;
import com.bytechef.ee.ai.copilot.util.Mode;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

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
        WorkflowValidatorTools workflowValidatorTools, WorkflowInstructionTools workflowInstructionTools) {

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
        ComponentTools componentTools, Optional<FirecrawlTools> firecrawlTools) throws AGUIException {
        String name = Source.CODE_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

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
        ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools)
        throws AGUIException {

        String name = Source.CODE_EDITOR.name() + "_" + Mode.BUILD.name();

        return CodeEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorBuildResource))
            .tools(
                List.of(
                    readProjectWorkflowTools, scriptTools, componentTools, workflowValidatorTools,
                    workflowInstructionTools))
            .state(state)
            .build();
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) throws AGUIException {

        String name = Source.CLUSTER_ELEMENT.name() + "_" + Mode.ASK.name();

        return ClusterElementSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementAskResource))
            .tools(
                List.of(
                    readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                    workflowInstructionTools))
            .state(state)
            .build();
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools)
        throws AGUIException {

        String name = Source.CLUSTER_ELEMENT.name() + "_" + Mode.BUILD.name();

        return ClusterElementSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementBuildResource))
            .tools(
                List.of(
                    readProjectWorkflowTools, clusterElementTools, componentTools, taskTools, workflowValidatorTools,
                    workflowInstructionTools))
            .state(state)
            .build();
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore copilotPgVectorStore) {
        return QuestionAnswerAdvisor.builder(copilotPgVectorStore)
            .build();
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, QuestionAnswerAdvisor questionAnswerAdvisor)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name() + "_" + Mode.ASK.name();

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

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
        ChatMemory chatMemory, ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade)
        throws AGUIException {

        String name = Source.WORKFLOW_EDITOR.name() + "_" + Mode.BUILD.name();

        return WorkflowEditorSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorBuildResource))
            .state(state)
            .tools(
                List.of(
                    projectTools, projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools,
                    workflowInstructionTools))
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .build();
    }

    @Bean
    ConverterSpringAIAgent converterBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectTools projectToolsImpl,
        ProjectWorkflowTools projectWorkflowToolsImpl, TaskTools taskTools, ScriptTools scriptTools)
        throws AGUIException {

        String name = Source.CONVERTER.name() + "_" + Mode.BUILD.name();

        return ConverterSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptConverterBuildResource))
            .state(state)
            .tools(
                List.of(
                    projectToolsImpl, projectWorkflowToolsImpl, taskTools, scriptTools, workflowValidatorTools,
                    workflowInstructionTools))
            .build();
    }

    @Bean
    SkillsSpringAIAgent skillsAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools)
        throws AGUIException {

        String name = Source.SKILLS.name() + "_" + Mode.ASK.name();

        return SkillsSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsAskResource))
            .state(state)
            .tools(
                List.of(
                    readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                    workflowInstructionTools))
            .build();
    }

    @Bean
    SkillsSpringAIAgent skillsBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools)
        throws AGUIException {

        String name = Source.SKILLS.name() + "_" + Mode.BUILD.name();

        return SkillsSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsBuildResource))
            .state(state)
            .tools(
                List.of(
                    skillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                    workflowInstructionTools))
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
