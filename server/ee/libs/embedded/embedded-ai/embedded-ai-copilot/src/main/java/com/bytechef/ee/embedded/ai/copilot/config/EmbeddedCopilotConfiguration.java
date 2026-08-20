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
import com.bytechef.ai.copilot.agent.WorkflowExecutionSpringAIAgent;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.copilot.agent.EmbeddedCodeWorkflowSpringAIAgent;
import com.bytechef.ee.embedded.ai.tool.IntegrationCodeWorkflowTools;
import com.bytechef.ee.embedded.ai.tool.IntegrationTools;
import com.bytechef.ee.embedded.ai.tool.IntegrationWorkflowExecutionTools;
import com.bytechef.ee.embedded.ai.tool.IntegrationWorkflowTools;
import com.bytechef.ee.embedded.ai.tool.ReadIntegrationCodeWorkflowTools;
import com.bytechef.ee.embedded.ai.tool.ReadIntegrationTools;
import com.bytechef.ee.embedded.ai.tool.ReadIntegrationWorkflowTools;
import com.bytechef.platform.ai.tool.BraveWebSearchTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.FirecrawlTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
 * Registers the embedded Copilot agent beans for the three embedded chat surfaces (workflow editor, code workflow, and
 * workflow execution), each with an ASK and a BUILD variant. The beans join the same {@code List<LocalAgent>} the EE
 * {@code CopilotApiController} resolves by agentId, so the embedded client reaches them through the ordinary
 * {@code /ai/chat/{agentId}} route.
 *
 * <p>
 * The agents reuse the concrete CE agent classes ({@link WorkflowEditorSpringAIAgent},
 * {@link WorkflowExecutionSpringAIAgent}) whose builders accept arbitrary agent ids; the code-workflow pair uses
 * {@link EmbeddedCodeWorkflowSpringAIAgent} (a minimal {@code SpringAIAgent} subclass whose {@code toolContext}
 * override carries the run's security context onto the tool-execution worker thread) because there is no CE
 * code-workflow agent class. Tool catalogs mirror the automation Copilot pairs in {@code CopilotConfiguration},
 * re-keyed from project/workspace tools onto the embedded integration tools from {@code embedded-ai-tool}; the shared,
 * platform-generic tools ({@link ComponentTools}, {@link TaskTools}, {@link WorkflowValidatorTools},
 * {@link WorkflowInstructionTools}, {@link FirecrawlTools}, {@link BraveWebSearchTools}) are reused unchanged.
 * </p>
 *
 * <p>
 * The {@code getSystemPrompt}/{@code wrapTools}/{@code state} helpers are private in the CE
 * {@code CopilotConfiguration}, so minimal local equivalents are replicated here (the same pattern the EE
 * {@code AiHubConfiguration} follows when it builds agents outside {@code CopilotConfiguration}).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class EmbeddedCopilotConfiguration {

    private final Resource promptWorkflowEditorEmbeddedAskResource;
    private final Resource promptWorkflowEditorEmbeddedBuildResource;
    private final Resource promptCodeWorkflowEmbeddedAskResource;
    private final Resource promptCodeWorkflowEmbeddedBuildResource;
    private final Resource promptWorkflowExecutionEmbeddedAskResource;
    private final Resource promptWorkflowExecutionEmbeddedBuildResource;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final State state = new State();

    // Read once at configuration init rather than per delegation: workflowEditorEmbeddedBuildSubAgentChatClientFactory
    // rebuilds its ChatClient on every delegation (to honour a caller-picked ChatModel), and getSystemPrompt(Resource)
    // performs I/O.
    private final String workflowEditorEmbeddedBuildSystemPrompt;

    // CT_CONSTRUCTOR_THROW: the constructor reads and validates the workflow-editor-embedded-build prompt resource up
    // front (see workflowEditorEmbeddedBuildSystemPrompt above) so getSystemPrompt's IllegalStateException surfaces at
    // startup, not on the first delegation; Spring never subclasses this @Configuration in a way that could observe
    // partially-initialized state.
    // RUBY-DISABLED: the prompt resources loaded below had every Ruby reference DELETED, not commented out.
    // readPrompt() below is a verbatim readAllBytes with no comment stripping, so the whole file becomes the
    // system message — a commented-out Ruby section would still be read by the model as content and it would
    // keep offering a language that org.graalvm.polyglot:ruby (stuck at 25.0.0, crashes on the pinned Truffle
    // 25.2.4) can no longer run. The marker therefore lives here, in code the model never sees.
    // Affected: prompt_code_workflow_embedded_build.txt (supported-language line, the whole '### Ruby' contract
    // section, and the createIntegrationCodeWorkflow language list) and prompt_code_workflow_embedded_ask.txt
    // (supported-language line) — the embedded twins of the automation prompts, which drift if only one side is
    // changed. Once a polyglot ruby jar built on Truffle 25.2+ is published (or GraalVM is downgraded), restore
    // both files together with their automation counterparts; the removed text is in git history at this commit.
    // Grep RUBY-DISABLED.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public EmbeddedCopilotConfiguration(
        @Value("classpath:prompt_workflow_editor_embedded_ask.txt") Resource promptWorkflowEditorEmbeddedAskResource,
        @Value("classpath:prompt_workflow_editor_embedded_build.txt") Resource promptWorkflowEditorEmbeddedBuildResource,
        @Value("classpath:prompt_code_workflow_embedded_ask.txt") Resource promptCodeWorkflowEmbeddedAskResource,
        @Value("classpath:prompt_code_workflow_embedded_build.txt") Resource promptCodeWorkflowEmbeddedBuildResource,
        @Value("classpath:prompt_workflow_execution_embedded_ask.txt") Resource promptWorkflowExecutionEmbeddedAskResource,
        @Value("classpath:prompt_workflow_execution_embedded_build.txt") Resource promptWorkflowExecutionEmbeddedBuildResource,
        WorkflowValidatorTools workflowValidatorTools, WorkflowInstructionTools workflowInstructionTools) {

        this.promptWorkflowEditorEmbeddedAskResource = promptWorkflowEditorEmbeddedAskResource;
        this.promptWorkflowEditorEmbeddedBuildResource = promptWorkflowEditorEmbeddedBuildResource;
        this.promptCodeWorkflowEmbeddedAskResource = promptCodeWorkflowEmbeddedAskResource;
        this.promptCodeWorkflowEmbeddedBuildResource = promptCodeWorkflowEmbeddedBuildResource;
        this.promptWorkflowExecutionEmbeddedAskResource = promptWorkflowExecutionEmbeddedAskResource;
        this.promptWorkflowExecutionEmbeddedBuildResource = promptWorkflowExecutionEmbeddedBuildResource;
        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;

        this.workflowEditorEmbeddedBuildSystemPrompt = getSystemPrompt(promptWorkflowEditorEmbeddedBuildResource);
    }

    @Bean
    WorkflowEditorSpringAIAgent workflowEditorEmbeddedAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadIntegrationTools readIntegrationTools,
        ReadIntegrationWorkflowTools readIntegrationWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools,
        WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade,
        @Qualifier("questionAnswerAdvisor") Advisor questionAnswerAdvisor, PermissionService permissionService,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readIntegrationTools, readIntegrationWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        return WorkflowEditorSpringAIAgent.builder()
            .agentId("workflow_editor_embedded_ask")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowEditorEmbeddedAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .advisor(questionAnswerAdvisor)
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .permissionService(permissionService)
            .securityContextRehydrator(securityContextRehydrator)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
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
            .agentId("workflow_editor_embedded_build")
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

    /**
     * Stateless embedded workflow-editor BUILD subagent {@link ChatClient} — the embedded mirror of
     * {@code workflowEditorBuildSubAgentChatClient}, bound to the integration + integration-workflow tools and the
     * embedded BUILD prompt. Contributed to the management MCP server (via
     * {@code ToolCallbackContributorConfiguration}) as the {@code buildIntegrationWorkflow} tool so MCP clients can
     * build integration workflows; not wired into the AI-Hub routing agent.
     */
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

    @Bean
    EmbeddedCodeWorkflowSpringAIAgent codeWorkflowEmbeddedAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadIntegrationCodeWorkflowTools readIntegrationCodeWorkflowTools,
        SecurityContextRehydrator securityContextRehydrator) throws AGUIException {

        return EmbeddedCodeWorkflowSpringAIAgent.builder()
            .agentId("code_workflow_embedded_ask")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeWorkflowEmbeddedAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of(readIntegrationCodeWorkflowTools)))
            .build();
    }

    @Bean
    EmbeddedCodeWorkflowSpringAIAgent codeWorkflowEmbeddedBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, IntegrationCodeWorkflowTools integrationCodeWorkflowTools,
        ReadIntegrationCodeWorkflowTools readIntegrationCodeWorkflowTools,
        SecurityContextRehydrator securityContextRehydrator) throws AGUIException {

        return EmbeddedCodeWorkflowSpringAIAgent.builder()
            .agentId("code_workflow_embedded_build")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeWorkflowEmbeddedBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(integrationCodeWorkflowTools, readIntegrationCodeWorkflowTools)))
            .build();
    }

    @Bean
    WorkflowExecutionSpringAIAgent workflowExecutionEmbeddedAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, IntegrationWorkflowExecutionTools integrationWorkflowExecutionTools,
        ReadIntegrationWorkflowTools readIntegrationWorkflowTools, ComponentTools componentTools,
        Optional<FirecrawlTools> firecrawlTools, Optional<BraveWebSearchTools> braveWebSearchTools,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                integrationWorkflowExecutionTools, readIntegrationWorkflowTools, componentTools, workflowValidatorTools,
                workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        return WorkflowExecutionSpringAIAgent.builder()
            .agentId("workflow_execution_embedded_ask")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowExecutionEmbeddedAskResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    WorkflowExecutionSpringAIAgent workflowExecutionEmbeddedBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, IntegrationWorkflowExecutionTools integrationWorkflowExecutionTools,
        IntegrationWorkflowTools integrationWorkflowTools, TaskTools taskTools,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                integrationWorkflowExecutionTools, integrationWorkflowTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        return WorkflowExecutionSpringAIAgent.builder()
            .agentId("workflow_execution_embedded_build")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowExecutionEmbeddedBuildResource))
            .tools(tools)
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    /**
     * Local equivalent of the private {@code CopilotConfiguration.wrapTools}: wraps each tool's callbacks in a
     * {@link RehydrateContextToolCallback} so the caller's tenant/security context is re-established on the worker
     * thread that executes the tool.
     */
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
