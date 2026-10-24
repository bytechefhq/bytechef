/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.config.InteractivePickerToolCallbacks;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory;
import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory.BuiltInSessionStore;
import com.bytechef.ee.ai.hub.agent.AiHubRoutingAgent;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.RunStopAdvisor;
import com.bytechef.ee.ai.hub.agent.WebhookBridgeAgent;
import com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry;
import com.bytechef.ee.ai.hub.agent.WorkflowChatGuard;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.approval.AiHubGatedToolFilterAdvisor;
import com.bytechef.ee.ai.hub.approval.AiHubPinnedToolCallbacks;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.CreateWorkflowChatToolCallback;
import com.bytechef.ee.ai.hub.tool.ListChatWorkflowsToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowChatTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowTabToolCallback;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Declares the AI Hub's two AG-UI {@link com.agui.server.LocalAgent}s, {@code ai_hub_ask} and {@code ai_hub_build}, and
 * the session memory both read and write. Each is an {@link AiHubRoutingAgent} that sends workflow chat turns to the
 * {@link WebhookBridgeAgent} and every other turn to its mode's {@link AiHubSpringAIAgent}.
 *
 * <p>
 * Tool lists are built explicitly (spec D3), never by sweeping every {@link ToolCallback} bean. ASK holds the read
 * views of the project, workflow and deployment tools, the read pickers, {@code openWorkflowTab} and
 * {@code openWorkflowChatTab}. BUILD holds the full project tools, the workflow lifecycle tools (no tool that writes
 * workflow content), the deployment write tools, every picker, {@code openWorkflowTab}, the workflow chat tools
 * ({@code openWorkflowChatTab}, {@code listChatWorkflows}, {@code createWorkflowChat}), and the {@code buildWorkflow} /
 * {@code importWorkflow} delegates, which are the only way it authors workflow content. The delegates' sub-agents never
 * see a tool the approval gate gates by default ({@link AiHubGatedToolFilterAdvisor}).
 * </p>
 *
 * <p>
 * Gated on {@code bytechef.ai.hub.enabled} AND {@code bytechef.ai.copilot.enabled}: the delegates come from the
 * Copilot's sub-agents, so a hub without the Copilot cannot author workflows.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnAiHub
public class AiHubConfiguration {

    static final Set<String> COMPONENT_DEFINITION_TOOL_NAMES = Set.of("getAction", "getClusterElement", "getTrigger");
    static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of("buildWorkflow", "importWorkflow");

    private static final String BUILD_WORKFLOW_TOOL_NAME = "buildWorkflow";

    private final Resource promptAiHubAskResource;
    private final Resource promptAiHubBuildResource;
    private final State state = new State();

    @SuppressFBWarnings("EI")
    public AiHubConfiguration(
        @Value("classpath:prompt_ai_hub_ask.txt") Resource promptAiHubAskResource,
        @Value("classpath:prompt_ai_hub_build.txt") Resource promptAiHubBuildResource) {

        this.promptAiHubAskResource = promptAiHubAskResource;
        this.promptAiHubBuildResource = promptAiHubBuildResource;
    }

    /**
     * The LLM delegate the {@code ai_hub_ask} {@link AiHubRoutingAgent} wraps. It shares the router's agent id, so it
     * is not a default candidate: unqualified {@code List<LocalAgent>} injection sees only the router, while
     * {@code @Qualifier("aiHubAskSpringAIAgent")} injection still resolves it.
     */
    @Bean(defaultCandidate = false)
    AiHubSpringAIAgent aiHubAskSpringAIAgent(
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, WorkflowExecutionTools workflowExecutionTools,
        ObjectProvider<ComponentTools> componentToolsProvider,
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks, ProjectWorkflowFacade projectWorkflowFacade,
        AiHubChatService chatService, AiHubChatAccessPolicy chatAccessPolicy,
        SecurityContextRehydrator securityContextRehydrator, AiHubApprovalGate approvalGate,
        ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<WebSearchToolCallbackProvider> webSearchToolCallbackProviderProvider)
        throws AGUIException {

        List<ToolCallback> toolCallbacks = new ArrayList<>(
            List.of(ToolCallbacks.from(readProjectTools, readProjectWorkflowTools, workflowExecutionTools)));

        toolCallbacks.addAll(componentDefinitionToolCallbacks(componentToolsProvider));

        toolCallbacks.addAll(deploymentFlatCrudToolCallbacks(deploymentToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(interactivePickerToolCallbacks.read());
        toolCallbacks.add(new OpenWorkflowTabToolCallback(projectWorkflowFacade, null));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback(chatService, chatAccessPolicy));

        return buildAgent(
            AiHubAgentType.AI_HUB_ASK, Mode.ASK, promptAiHubAskResource, toolCallbacks, aiHubSessionMemory, chatModel,
            securityContextRehydrator, approvalGate, inFlightRunRegistryProvider, overrideChatClientResolverProvider,
            webSearchToolCallbackProviderProvider);
    }

    /**
     * The LLM delegate the {@code ai_hub_build} {@link AiHubRoutingAgent} wraps; not a default candidate for the same
     * reason as {@code aiHubAskSpringAIAgent}.
     */
    @Bean(defaultCandidate = false)
    AiHubSpringAIAgent aiHubBuildSpringAIAgent(
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools, WorkflowExecutionTools workflowExecutionTools,
        ObjectProvider<ComponentTools> componentToolsProvider,
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks, IntelligentToolCatalog intelligentToolCatalog,
        WorkspaceFacade workspaceFacade, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectDeploymentFacade projectDeploymentFacade, AiHubChatService chatService,
        AiHubChatAccessPolicy chatAccessPolicy, AiHubChatArtifactRecorder aiHubChatArtifactRecorder,
        SecurityContextRehydrator securityContextRehydrator,
        AiHubApprovalGate approvalGate, ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<WebSearchToolCallbackProvider> webSearchToolCallbackProviderProvider)
        throws AGUIException {

        List<ToolCallback> toolCallbacks = new ArrayList<>(
            List.of(ToolCallbacks.from(projectTools, projectWorkflowLifecycleTools, workflowExecutionTools)));

        toolCallbacks.addAll(componentDefinitionToolCallbacks(componentToolsProvider));

        toolCallbacks.addAll(deploymentFlatCrudToolCallbacks(deploymentToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(interactivePickerToolCallbacks.write());
        toolCallbacks.add(new OpenWorkflowTabToolCallback(projectWorkflowFacade, aiHubChatArtifactRecorder));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback(chatService, chatAccessPolicy));
        toolCallbacks.add(new ListChatWorkflowsToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new CreateWorkflowChatToolCallback(chatService, projectDeploymentFacade));
        toolCallbacks.addAll(
            intelligentToolCatalog.getByNames(
                INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD,
                (chatClient, definition) -> AiHubGatedToolFilterAdvisor.filter(chatClient),
                (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(toolCallback, workspaceFacade)));

        requireBuildWorkflow(toolCallbacks);

        return buildAgent(
            AiHubAgentType.AI_HUB_BUILD, Mode.BUILD, promptAiHubBuildResource, toolCallbacks, aiHubSessionMemory,
            chatModel, securityContextRehydrator, approvalGate, inFlightRunRegistryProvider,
            overrideChatClientResolverProvider, webSearchToolCallbackProviderProvider);
    }

    /**
     * The agent that serves workflow chat turns through the chat's webhook trigger. It is a
     * {@link com.agui.server.LocalAgent} but not a default candidate: the unqualified {@code List<LocalAgent>} the chat
     * controllers dispatch from holds only agents a URL may address, and only the routers reach this one, by name.
     */
    @Bean(defaultCandidate = false)
    WebhookBridgeAgent webhookBridgeAgent(
        WebhookWorkflowExecutor webhookWorkflowExecutor, AiHubChatService chatService,
        WebhookResumeRegistry webhookResumeRegistry, JsonMapper jsonMapper, WorkflowChatMetrics workflowChatMetrics,
        AiHubSessionMemory aiHubSessionMemory, WorkflowChatGuard workflowChatGuard,
        ObjectProvider<JobFacade> jobFacadeProvider, InFlightAiHubRunRegistry inFlightRunRegistry)
        throws AGUIException {

        return new WebhookBridgeAgent(
            webhookWorkflowExecutor, chatService, webhookResumeRegistry, jsonMapper, workflowChatMetrics,
            aiHubSessionMemory, workflowChatGuard, jobFacadeProvider.getIfAvailable(), inFlightRunRegistry);
    }

    @Bean
    AiHubRoutingAgent aiHubAskRoutingAgent(
        @Qualifier("aiHubAskSpringAIAgent") AiHubSpringAIAgent aiHubAskSpringAIAgent,
        @Qualifier("webhookBridgeAgent") ObjectProvider<WebhookBridgeAgent> webhookBridgeAgentProvider,
        AiHubChatService chatService)
        throws AGUIException {

        return new AiHubRoutingAgent(
            AiHubAgentType.AI_HUB_ASK.key(), aiHubAskSpringAIAgent, webhookBridgeAgentProvider.getIfAvailable(),
            chatService);
    }

    @Bean
    AiHubRoutingAgent aiHubBuildRoutingAgent(
        @Qualifier("aiHubBuildSpringAIAgent") AiHubSpringAIAgent aiHubBuildSpringAIAgent,
        @Qualifier("webhookBridgeAgent") ObjectProvider<WebhookBridgeAgent> webhookBridgeAgentProvider,
        AiHubChatService chatService)
        throws AGUIException {

        return new AiHubRoutingAgent(
            AiHubAgentType.AI_HUB_BUILD.key(), aiHubBuildSpringAIAgent, webhookBridgeAgentProvider.getIfAvailable(),
            chatService);
    }

    /**
     * The AI Hub's session-based conversation memory over the application session backend
     * ({@code bytechef.ai.memory.provider}, jdbc by default). AutoCloseable: Spring disposes the owning Redis/S3 client
     * on shutdown.
     */
    @Bean
    AiHubSessionMemory aiHubSessionMemory(
        Environment environment, @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate) {

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, jdbcTemplate);

        return new AiHubSessionMemory(builtInSessionStore.sessionRepository(), builtInSessionStore.closeable());
    }

    /**
     * The unwrapped pinned tools of the agent serving each mode, where the approval facade re-finds an approved call's
     * tool.
     */
    @Bean
    AiHubPinnedToolCallbacks aiHubPinnedToolCallbacks(
        @Qualifier("aiHubAskSpringAIAgent") AiHubSpringAIAgent aiHubAskSpringAIAgent,
        @Qualifier("aiHubBuildSpringAIAgent") AiHubSpringAIAgent aiHubBuildSpringAIAgent) {

        return mode -> mode == Mode.BUILD
            ? aiHubBuildSpringAIAgent.pinnedToolCallbacks()
            : aiHubAskSpringAIAgent.pinnedToolCallbacks();
    }

    /**
     * The read-only component definition lookups the hub uses to check a failed task's parameters against the
     * definition. An absent {@link ComponentTools} resolves to an empty list.
     */
    static List<ToolCallback> componentDefinitionToolCallbacks(ObjectProvider<ComponentTools> componentToolsProvider) {
        ComponentTools componentTools = componentToolsProvider.getIfAvailable();

        if (componentTools == null) {
            return List.of();
        }

        return Arrays.stream(ToolCallbacks.from(componentTools))
            .filter(toolCallback -> COMPONENT_DEFINITION_TOOL_NAMES.contains(toolCallback.getToolDefinition()
                .name()))
            .toList();
    }

    /**
     * The project-deployment tools put flat on the hub agents: {@code listProjectDeployments} on ASK, plus the six
     * mutations ({@code create}, {@code update}, {@code delete}, {@code rollback}, {@code toggle},
     * {@code promoteWorkflow}) on BUILD. An absent factory resolves to an empty list.
     */
    static List<ToolCallback> deploymentFlatCrudToolCallbacks(
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider, boolean writable) {

        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = deploymentToolCallbacksFactoryProvider
            .getIfAvailable();

        if (deploymentToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? deploymentToolCallbacksFactory.writeToolCallbacks()
            : deploymentToolCallbacksFactory.readToolCallbacks();
    }

    private static void requireBuildWorkflow(List<ToolCallback> toolCallbacks) {
        boolean hasBuildWorkflow = toolCallbacks.stream()
            .anyMatch(toolCallback -> BUILD_WORKFLOW_TOOL_NAME.equals(toolCallback.getToolDefinition()
                .name()));

        if (!hasBuildWorkflow) {
            throw new IllegalStateException(
                "The AI Hub BUILD agent has no buildWorkflow tool, so it could not author workflows. buildWorkflow " +
                    "comes from the Copilot's workflow-building sub-agent: with bytechef.ai.hub.enabled=true, " +
                    "bytechef.ai.copilot.enabled=true must also be set and the Copilot's sub-agent chat clients " +
                    "must be available. Set bytechef.ai.hub.enabled=false to run without the AI Hub.");
        }
    }

    private AiHubSpringAIAgent buildAgent(
        AiHubAgentType agentType, Mode mode, Resource systemPromptResource, List<ToolCallback> toolCallbacks,
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel,
        SecurityContextRehydrator securityContextRehydrator, AiHubApprovalGate approvalGate,
        ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<WebSearchToolCallbackProvider> webSearchToolCallbackProviderProvider)
        throws AGUIException {

        AiHubSpringAIAgent.Builder builder = AiHubSpringAIAgent.builder()
            .agentId(agentType.key())
            .chatModel(chatModel)
            .systemMessage(readSystemPrompt(systemPromptResource))
            .toolCallbacks(toolCallbacks)
            .state(state)
            .mode(mode)
            .securityContextRehydrator(securityContextRehydrator)
            .approvalGate(approvalGate);

        inFlightRunRegistryProvider
            .ifAvailable(inFlightRunRegistry -> builder.advisor(new RunStopAdvisor(inFlightRunRegistry)));
        builder.advisor(aiHubSessionMemory.createResentUserMessageAdvisor());
        builder.advisor(aiHubSessionMemory.createSessionMemoryAdvisor());
        builder.advisor(aiHubSessionMemory.createTurnEndingToolResultAdvisor(approvalGate::recordToolCallId));

        overrideChatClientResolverProvider.ifAvailable(builder::overrideChatClientResolver);
        webSearchToolCallbackProviderProvider.ifAvailable(builder::webSearchToolCallbackProvider);

        return builder.build();
    }

    private static String readSystemPrompt(Resource systemPromptResource) {
        try (InputStream inputStream = systemPromptResource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }
}
