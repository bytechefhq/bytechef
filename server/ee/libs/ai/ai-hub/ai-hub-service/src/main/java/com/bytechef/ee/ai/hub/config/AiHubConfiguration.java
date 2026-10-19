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
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory;
import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory.BuiltInSessionStore;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.RunStopAdvisor;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.approval.AiHubPinnedToolCallbacks;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowTabToolCallback;
import com.bytechef.ee.ai.hub.util.Mode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

/**
 * Declares the AI Hub's two AG-UI {@link com.agui.server.LocalAgent}s, {@code ai_hub_ask} and {@code ai_hub_build}, and
 * the session memory both read and write.
 *
 * <p>
 * Tool lists are built explicitly (spec D3), never by sweeping every {@link ToolCallback} bean. ASK holds the read
 * views of the project, workflow and deployment tools, the read pickers and {@code openWorkflowTab}. BUILD holds the
 * full project tools, the workflow lifecycle tools (no tool that writes workflow content), the deployment write tools,
 * every picker, {@code openWorkflowTab}, and the {@code buildWorkflow} / {@code importWorkflow} delegates, which are
 * the only way it authors workflow content.
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

    @Bean
    AiHubSpringAIAgent aiHubAskSpringAIAgent(
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools,
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks, ProjectWorkflowFacade projectWorkflowFacade,
        SecurityContextRehydrator securityContextRehydrator, AiHubApprovalGate approvalGate,
        ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        List<ToolCallback> toolCallbacks = new ArrayList<>(
            List.of(ToolCallbacks.from(readProjectTools, readProjectWorkflowTools)));

        toolCallbacks.addAll(deploymentFlatCrudToolCallbacks(deploymentToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(interactivePickerToolCallbacks.read());
        toolCallbacks.add(new OpenWorkflowTabToolCallback(projectWorkflowFacade, null));

        return buildAgent(
            AiHubAgentType.AI_HUB_ASK, Mode.ASK, promptAiHubAskResource, toolCallbacks, aiHubSessionMemory, chatModel,
            securityContextRehydrator, approvalGate, inFlightRunRegistryProvider, overrideChatClientResolverProvider);
    }

    @Bean
    AiHubSpringAIAgent aiHubBuildSpringAIAgent(
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools,
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks, IntelligentToolCatalog intelligentToolCatalog,
        WorkspaceFacade workspaceFacade, ProjectWorkflowFacade projectWorkflowFacade,
        AiHubChatArtifactRecorder aiHubChatArtifactRecorder, SecurityContextRehydrator securityContextRehydrator,
        AiHubApprovalGate approvalGate, ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        List<ToolCallback> toolCallbacks = new ArrayList<>(
            List.of(ToolCallbacks.from(projectTools, projectWorkflowLifecycleTools)));

        toolCallbacks.addAll(deploymentFlatCrudToolCallbacks(deploymentToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(interactivePickerToolCallbacks.write());
        toolCallbacks.add(new OpenWorkflowTabToolCallback(projectWorkflowFacade, aiHubChatArtifactRecorder));
        toolCallbacks.addAll(
            intelligentToolCatalog.getByNames(
                INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(toolCallback, workspaceFacade)));

        requireBuildWorkflow(toolCallbacks);

        return buildAgent(
            AiHubAgentType.AI_HUB_BUILD, Mode.BUILD, promptAiHubBuildResource, toolCallbacks, aiHubSessionMemory,
            chatModel, securityContextRehydrator, approvalGate, inFlightRunRegistryProvider,
            overrideChatClientResolverProvider);
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
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider)
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
        builder.advisor(aiHubSessionMemory.createTurnEndingToolResultAdvisor());

        overrideChatClientResolverProvider.ifAvailable(builder::overrideChatClientResolver);

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
