/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.copilot.config.InteractivePickerToolCallbacks;
import com.bytechef.ai.copilot.tool.PersistedWorkflowChecker;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.datatable.DataTableToolCallbacksFactory;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.assetfile.service.AssetFileSystemFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory;
import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory.BuiltInSessionStore;
import com.bytechef.ee.ai.hub.agent.AiHubRoutingAgent;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.AiHubToolCallbackWrappers;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.RunStopAdvisor;
import com.bytechef.ee.ai.hub.agent.WebhookBridgeAgent;
import com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry;
import com.bytechef.ee.ai.hub.agent.WorkflowChatGuard;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.approval.AiHubGatedToolFilterAdvisor;
import com.bytechef.ee.ai.hub.approval.AiHubPinnedToolCallbacks;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.ee.ai.hub.progress.SubagentProgressToolCallback;
import com.bytechef.ee.ai.hub.progress.SubagentStepAdvisor;
import com.bytechef.ee.ai.hub.skill.AiHubSkillsToolProvider;
import com.bytechef.ee.ai.hub.subagent.SubAgentAdvisorContributor;
import com.bytechef.ee.ai.hub.subagent.SubAgentContributingChatClient;
import com.bytechef.ee.ai.hub.subagent.SubAgentSessionMemoryContributor;
import com.bytechef.ee.ai.hub.subagent.WorkspaceAdvisorContributor;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AiHubChatToolToolCallbacks;
import com.bytechef.ee.ai.hub.tool.AiHubSubAgentToolCallbackFactory;
import com.bytechef.ee.ai.hub.tool.CheckWorkflowTools;
import com.bytechef.ee.ai.hub.tool.CreateWorkflowChatToolCallback;
import com.bytechef.ee.ai.hub.tool.ListAiHubChatsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListChatWorkflowsToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenAiAgentTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenCodeWorkflowTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenCustomComponentTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenFileTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowChatTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowTabToolCallback;
import com.bytechef.ee.ai.hub.tool.ResearchToolCallbackFactory;
import com.bytechef.ee.ai.hub.tool.RunChatWorkflowToolCallback;
import com.bytechef.ee.ai.hub.tool.SearchDocsTools;
import com.bytechef.ee.ai.hub.tool.UserRequestForwardingToolCallback;
import com.bytechef.ee.ai.hub.tool.memory.AiHubAutoMemoryToolsAdvisor;
import com.bytechef.ee.ai.hub.toolsearch.AiHubChatBindingToolCallbackResolver;
import com.bytechef.ee.ai.hub.toolsearch.AiHubGlobalToolCatalog;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.automation.ai.tool.ApiCollectionToolCallbacksFactory;
import com.bytechef.ee.automation.ai.tool.PromoteToEnvironmentToolCallback;
import com.bytechef.ee.automation.ai.tool.componentrule.ComponentRuleToolCallbacksFactory;
import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.vectorstore.VectorStore;
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
 * see a tool the approval gate gates, by default or by a workspace rule ({@link AiHubGatedToolFilterAdvisor}).
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
    static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of(
        "buildWorkflow", "importWorkflow", "buildCustomComponent", "buildCodeWorkflow");
    static final Set<String> ASK_INTELLIGENT_TOOL_NAMES = Set.of("explainCodeWorkflow", "explainCustomComponent");

    static final Set<String> ASK_PINNED_TOOL_NAMES = Set.of(
        "askUserQuestion", "checkWorkflow", "explainCodeWorkflow", "explainCustomComponent", "getWorkflow",
        "listProjects", "listWorkflows", "openAiAgentTab", "openCodeWorkflowTab", "openCustomComponentTab",
        "openFileTab", "openWorkflowChatTab", "openWorkflowTab", "searchProjects", "searchWorkflows");

    static final Set<String> BUILD_PINNED_TOOL_NAMES = Set.of(
        "askUserQuestion", "buildCodeWorkflow", "buildCustomComponent", "buildWorkflow", "checkWorkflow",
        "createConnection", "createProject", "createProjectWorkflow", "data_analyst", "getWorkflow", "image_generator",
        "importWorkflow", "listConnectionsForComponent", "listProjects", "listWorkflows", "openAiAgentTab",
        "openCodeWorkflowTab", "openCustomComponentTab", "openFileTab", "openWorkflowChatTab", "openWorkflowTab",
        "saveWorkflowTestConnection", "searchProjects", "searchWorkflows", "selectClusterElementPropertyOption",
        "selectConnection", "selectPropertyOption", "selectTriggerPropertyOption", "slide_builder");

    private static final String BUILD_WORKFLOW_TOOL_NAME = "buildWorkflow";
    private static final String UPDATE_WORKFLOW_TOOL_NAME = "updateWorkflow";

    private final Resource promptAiHubAskResource;
    private final Resource promptAiHubAutoMemoryToolsReadOnlyResource;
    private final Resource promptAiHubAutoMemoryToolsResource;
    private final Resource promptAiHubBuildResource;
    private final State state = new State();

    @SuppressFBWarnings("EI")
    public AiHubConfiguration(
        @Value("classpath:prompt_ai_hub_ask.txt") Resource promptAiHubAskResource,
        @Value("classpath:prompt/ai_hub_auto_memory_tools_read_only_system_prompt.md") //
        Resource promptAiHubAutoMemoryToolsReadOnlyResource,
        @Value("classpath:prompt/ai_hub_auto_memory_tools_system_prompt.md") Resource promptAiHubAutoMemoryToolsResource,
        @Value("classpath:prompt_ai_hub_build.txt") Resource promptAiHubBuildResource) {

        this.promptAiHubAskResource = promptAiHubAskResource;
        this.promptAiHubAutoMemoryToolsReadOnlyResource = promptAiHubAutoMemoryToolsReadOnlyResource;
        this.promptAiHubAutoMemoryToolsResource = promptAiHubAutoMemoryToolsResource;
        this.promptAiHubBuildResource = promptAiHubBuildResource;
    }

    @Bean(defaultCandidate = false)
    AiHubModeToolCallbacks aiHubAskToolCallbacks(
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        WorkflowExecutionTools workflowExecutionTools, ObjectProvider<ComponentTools> componentToolsProvider,
        ObjectProvider<PersistedWorkflowChecker> persistedWorkflowCheckerProvider,
        ObjectProvider<ProjectService> projectServiceProvider,
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider,
        ObjectProvider<AssetFileToolCallbacksFactory> assetFileToolCallbacksFactoryProvider,
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider,
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider,
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider,
        ObjectProvider<ApiCollectionToolCallbacksFactory> apiCollectionToolCallbacksFactoryProvider,
        ObjectProvider<ComponentRuleToolCallbacksFactory> componentRuleToolCallbacksFactoryProvider,
        ObjectProvider<AiAgentFacade> aiAgentFacadeProvider, ObjectProvider<AssetFileFacade> assetFileFacadeProvider,
        ObjectProvider<CustomComponentFacade> customComponentFacadeProvider,
        ObjectProvider<ProjectCodeWorkflowFacade> projectCodeWorkflowFacadeProvider,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks, IntelligentToolCatalog intelligentToolCatalog,
        WorkspaceFacade workspaceFacade, ProjectWorkflowFacade projectWorkflowFacade, AiHubChatService chatService,
        AiHubChatAccessPolicy chatAccessPolicy, AiHubToolApprovalPolicy toolApprovalPolicy,
        AiHubSessionMemory aiHubSessionMemory,
        ObjectProvider<EmbeddingProviderStatusProvider> embeddingProviderStatusProviderProvider,
        @Qualifier("copilotPgVectorStore") ObjectProvider<VectorStore> copilotPgVectorStoreProvider,
        ObjectProvider<AiGuardrails> aiGuardrailsProvider, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<AiHubChatToolToolCallbacks> chatToolToolCallbacksProvider) {

        List<ToolCallback> toolCallbacks = new ArrayList<>(
            List.of(ToolCallbacks.from(readProjectTools, readProjectWorkflowTools, workflowExecutionTools)));

        toolCallbacks.addAll(componentDefinitionToolCallbacks(componentToolsProvider));
        toolCallbacks.addAll(checkWorkflowToolCallbacks(persistedWorkflowCheckerProvider, projectServiceProvider));
        toolCallbacks.addAll(
            searchDocsToolCallbacks(embeddingProviderStatusProviderProvider, copilotPgVectorStoreProvider));

        toolCallbacks.addAll(deploymentFlatCrudToolCallbacks(deploymentToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(aiAgentFlatCrudToolCallbacks(aiAgentToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(assetFileFlatCrudToolCallbacks(assetFileToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(dataTableFlatCrudToolCallbacks(dataTableToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(knowledgeBaseFlatCrudToolCallbacks(knowledgeBaseToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(contextStoreFlatCrudToolCallbacks(contextStoreToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(apiCollectionFlatCrudToolCallbacks(apiCollectionToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(componentRuleFlatCrudToolCallbacks(componentRuleToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(openAiAgentTabToolCallbacks(aiAgentFacadeProvider, null));
        toolCallbacks.addAll(openFileTabToolCallbacks(assetFileFacadeProvider, null));
        toolCallbacks.addAll(interactivePickerToolCallbacks.read());
        toolCallbacks.add(new OpenWorkflowTabToolCallback(projectWorkflowFacade, null));
        toolCallbacks.addAll(openCustomComponentTabToolCallbacks(customComponentFacadeProvider, null));
        toolCallbacks.addAll(openCodeWorkflowTabToolCallbacks(projectCodeWorkflowFacadeProvider, null));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback(chatService, chatAccessPolicy));
        toolCallbacks.add(new ListAiHubChatsToolCallback(chatService));

        AiGuardrails aiGuardrails = aiGuardrailsProvider.getIfAvailable();
        AiGuardrailMetrics aiGuardrailMetrics = aiGuardrailMetrics(aiGuardrails, meterRegistryProvider);

        toolCallbacks.addAll(
            intelligentToolCallbacks(
                intelligentToolCatalog, ASK_INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.ASK, toolApprovalPolicy,
                workspaceFacade, aiHubSessionMemory, aiGuardrails, aiGuardrailMetrics));

        chatToolToolCallbacksProvider.ifAvailable(
            chatToolToolCallbacks -> toolCallbacks.addAll(chatToolToolCallbacks.read()));

        return new AiHubModeToolCallbacks(toolCallbacks, ASK_PINNED_TOOL_NAMES);
    }

    @Bean(defaultCandidate = false)
    AiHubModeToolCallbacks aiHubBuildToolCallbacks(
        ProjectTools projectTools, ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools,
        WorkflowExecutionTools workflowExecutionTools, ObjectProvider<ComponentTools> componentToolsProvider,
        ObjectProvider<PersistedWorkflowChecker> persistedWorkflowCheckerProvider,
        ObjectProvider<ProjectService> projectServiceProvider,
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider,
        ObjectProvider<AssetFileToolCallbacksFactory> assetFileToolCallbacksFactoryProvider,
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider,
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider,
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider,
        ObjectProvider<ApiCollectionToolCallbacksFactory> apiCollectionToolCallbacksFactoryProvider,
        ObjectProvider<ComponentRuleToolCallbacksFactory> componentRuleToolCallbacksFactoryProvider,
        ObjectProvider<AiAgentFacade> aiAgentFacadeProvider, ObjectProvider<AssetFileFacade> assetFileFacadeProvider,
        ObjectProvider<CustomComponentFacade> customComponentFacadeProvider,
        ObjectProvider<ProjectCodeWorkflowFacade> projectCodeWorkflowFacadeProvider,
        InteractivePickerToolCallbacks interactivePickerToolCallbacks, IntelligentToolCatalog intelligentToolCatalog,
        WorkspaceFacade workspaceFacade, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectDeploymentFacade projectDeploymentFacade,
        ObjectProvider<EnvironmentPromotionFacade> environmentPromotionFacadeProvider,
        ObjectProvider<EnvironmentService> environmentServiceProvider, AiHubChatService chatService,
        AiHubChatAccessPolicy chatAccessPolicy, AiHubChatArtifactRecorder aiHubChatArtifactRecorder,
        AiHubToolApprovalPolicy toolApprovalPolicy, AiHubSessionMemory aiHubSessionMemory,
        WorkflowFacade workflowFacade,
        ObjectProvider<EmbeddingProviderStatusProvider> embeddingProviderStatusProviderProvider,
        @Qualifier("copilotPgVectorStore") ObjectProvider<VectorStore> copilotPgVectorStoreProvider,
        ObjectProvider<AiGuardrails> aiGuardrailsProvider, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<AiHubChatToolToolCallbacks> chatToolToolCallbacksProvider,
        ObjectProvider<AiHubSubAgentToolCallbackFactory> subAgentToolCallbackFactoriesProvider) {

        List<ToolCallback> toolCallbacks = new ArrayList<>(
            buildProjectToolCallbacks(projectTools, projectWorkflowLifecycleTools, workflowExecutionTools));

        toolCallbacks.addAll(componentDefinitionToolCallbacks(componentToolsProvider));
        toolCallbacks.addAll(checkWorkflowToolCallbacks(persistedWorkflowCheckerProvider, projectServiceProvider));
        toolCallbacks.addAll(
            searchDocsToolCallbacks(embeddingProviderStatusProviderProvider, copilotPgVectorStoreProvider));

        toolCallbacks.addAll(deploymentFlatCrudToolCallbacks(deploymentToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(aiAgentFlatCrudToolCallbacks(aiAgentToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(assetFileFlatCrudToolCallbacks(assetFileToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(dataTableFlatCrudToolCallbacks(dataTableToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(knowledgeBaseFlatCrudToolCallbacks(knowledgeBaseToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(contextStoreFlatCrudToolCallbacks(contextStoreToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(apiCollectionFlatCrudToolCallbacks(apiCollectionToolCallbacksFactoryProvider, true));
        toolCallbacks.addAll(componentRuleFlatCrudToolCallbacks(componentRuleToolCallbacksFactoryProvider, true));
        toolCallbacks
            .addAll(promoteToEnvironmentToolCallbacks(environmentPromotionFacadeProvider, environmentServiceProvider));
        toolCallbacks.addAll(openAiAgentTabToolCallbacks(aiAgentFacadeProvider, aiHubChatArtifactRecorder));
        toolCallbacks.addAll(openFileTabToolCallbacks(assetFileFacadeProvider, aiHubChatArtifactRecorder));
        toolCallbacks.addAll(interactivePickerToolCallbacks.write());
        toolCallbacks.add(new OpenWorkflowTabToolCallback(projectWorkflowFacade, aiHubChatArtifactRecorder));
        toolCallbacks.addAll(
            openCustomComponentTabToolCallbacks(customComponentFacadeProvider, aiHubChatArtifactRecorder));
        toolCallbacks.addAll(
            openCodeWorkflowTabToolCallbacks(projectCodeWorkflowFacadeProvider, aiHubChatArtifactRecorder));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback(chatService, chatAccessPolicy));
        toolCallbacks.add(new ListAiHubChatsToolCallback(chatService));
        toolCallbacks.add(new ListChatWorkflowsToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new CreateWorkflowChatToolCallback(chatService, projectDeploymentFacade));
        toolCallbacks.add(
            new RunChatWorkflowToolCallback(projectDeploymentFacade, workflowFacade, aiHubChatArtifactRecorder));

        AiGuardrails aiGuardrails = aiGuardrailsProvider.getIfAvailable();
        AiGuardrailMetrics aiGuardrailMetrics = aiGuardrailMetrics(aiGuardrails, meterRegistryProvider);

        toolCallbacks.addAll(
            intelligentToolCallbacks(
                intelligentToolCatalog, INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, toolApprovalPolicy,
                workspaceFacade, aiHubSessionMemory, aiGuardrails, aiGuardrailMetrics));
        toolCallbacks.addAll(subAgentToolCallbacks(subAgentToolCallbackFactoriesProvider));

        chatToolToolCallbacksProvider.ifAvailable(
            chatToolToolCallbacks -> toolCallbacks.addAll(chatToolToolCallbacks.write()));

        requireBuildWorkflow(toolCallbacks);

        return new AiHubModeToolCallbacks(toolCallbacks, BUILD_PINNED_TOOL_NAMES);
    }

    /**
     * The LLM delegate the {@code ai_hub_ask} {@link AiHubRoutingAgent} wraps. It shares the router's agent id, so it
     * is not a default candidate: unqualified {@code List<LocalAgent>} injection sees only the router, while
     * {@code @Qualifier("aiHubAskSpringAIAgent")} injection still resolves it.
     */
    @Bean(defaultCandidate = false)
    AiHubSpringAIAgent aiHubAskSpringAIAgent(
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel,
        @Qualifier("aiHubAskToolCallbacks") AiHubModeToolCallbacks aiHubAskToolCallbacks,
        SecurityContextRehydrator securityContextRehydrator, AiHubApprovalGate approvalGate,
        ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<WebSearchToolCallbackProvider> webSearchToolCallbackProviderProvider,
        ObjectProvider<ResearchToolCallbackFactory> researchToolCallbackFactoryProvider,
        @Qualifier("aiHubAskToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        ObjectProvider<AiGuardrails> aiGuardrailsProvider, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<AiHubChatBindingToolCallbackResolver> chatBindingToolCallbackResolverProvider,
        ObjectProvider<AiAutoMemoryService> aiAutoMemoryServiceProvider,
        ObjectProvider<AiHubSkillsToolProvider> skillsToolProviderProvider)
        throws AGUIException {

        AiGuardrails aiGuardrails = aiGuardrailsProvider.getIfAvailable();

        return buildAgent(
            AiHubAgentType.AI_HUB_ASK, Mode.ASK, promptAiHubAskResource, aiHubAskToolCallbacks.pinned(),
            aiHubSessionMemory, chatModel, securityContextRehydrator, approvalGate, inFlightRunRegistryProvider,
            overrideChatClientResolverProvider, webSearchToolCallbackProviderProvider,
            researchToolCallbackFactoryProvider, toolSearchToolCallAdvisorProvider, aiGuardrails,
            aiGuardrailMetrics(aiGuardrails, meterRegistryProvider),
            chatBindingToolCallbackResolverProvider, aiAutoMemoryServiceProvider,
            skillsToolProviderProvider);
    }

    @Bean(defaultCandidate = false)
    AiHubSpringAIAgent aiHubBuildSpringAIAgent(
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel,
        @Qualifier("aiHubBuildToolCallbacks") AiHubModeToolCallbacks aiHubBuildToolCallbacks,
        SecurityContextRehydrator securityContextRehydrator, AiHubApprovalGate approvalGate,
        ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<WebSearchToolCallbackProvider> webSearchToolCallbackProviderProvider,
        ObjectProvider<ResearchToolCallbackFactory> researchToolCallbackFactoryProvider,
        @Qualifier("aiHubBuildToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        ObjectProvider<AiGuardrails> aiGuardrailsProvider, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<AiHubChatBindingToolCallbackResolver> chatBindingToolCallbackResolverProvider,
        ObjectProvider<AiAutoMemoryService> aiAutoMemoryServiceProvider,
        ObjectProvider<AiHubSkillsToolProvider> skillsToolProviderProvider)
        throws AGUIException {

        AiGuardrails aiGuardrails = aiGuardrailsProvider.getIfAvailable();

        return buildAgent(
            AiHubAgentType.AI_HUB_BUILD, Mode.BUILD, promptAiHubBuildResource, aiHubBuildToolCallbacks.pinned(),
            aiHubSessionMemory, chatModel, securityContextRehydrator, approvalGate, inFlightRunRegistryProvider,
            overrideChatClientResolverProvider, webSearchToolCallbackProviderProvider,
            researchToolCallbackFactoryProvider, toolSearchToolCallAdvisorProvider, aiGuardrails,
            aiGuardrailMetrics(aiGuardrails, meterRegistryProvider),
            chatBindingToolCallbackResolverProvider, aiAutoMemoryServiceProvider,
            skillsToolProviderProvider);
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
        ObjectProvider<JobFacade> jobFacadeProvider, InFlightAiHubRunRegistry inFlightRunRegistry,
        ObjectProvider<AssetFileSystemFacade> assetFileSystemFacadeProvider)
        throws AGUIException {

        return new WebhookBridgeAgent(
            webhookWorkflowExecutor, chatService, webhookResumeRegistry, jsonMapper, workflowChatMetrics,
            aiHubSessionMemory, workflowChatGuard, jobFacadeProvider.getIfAvailable(), inFlightRunRegistry,
            assetFileSystemFacadeProvider.getIfAvailable());
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

    @Bean
    AiHubSessionMemory aiHubSessionMemory(
        Environment environment, @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate) {

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, jdbcTemplate);

        return new AiHubSessionMemory(builtInSessionStore.sessionRepository(), builtInSessionStore.closeable());
    }

    @Bean
    AiHubGlobalToolCatalog aiHubAskGlobalToolCatalog(
        @Qualifier("aiHubAskToolCallbacks") AiHubModeToolCallbacks aiHubAskToolCallbacks,
        ObjectProvider<ComponentTools> componentToolsProvider, ObjectProvider<TaskTools> taskToolsProvider,
        ObjectProvider<TaskDispatcherTools> taskDispatcherToolsProvider) {

        return new AiHubGlobalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID,
            catalogToolCallbacks(
                aiHubAskToolCallbacks, componentToolsProvider, taskToolsProvider,
                taskDispatcherToolsProvider));
    }

    @Bean
    AiHubGlobalToolCatalog aiHubBuildGlobalToolCatalog(
        @Qualifier("aiHubBuildToolCallbacks") AiHubModeToolCallbacks aiHubBuildToolCallbacks,
        ObjectProvider<ComponentTools> componentToolsProvider, ObjectProvider<TaskTools> taskToolsProvider,
        ObjectProvider<TaskDispatcherTools> taskDispatcherToolsProvider) {

        return new AiHubGlobalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID,
            catalogToolCallbacks(
                aiHubBuildToolCallbacks, componentToolsProvider, taskToolsProvider,
                taskDispatcherToolsProvider));
    }

    @Bean
    AiHubPinnedToolCallbacks aiHubPinnedToolCallbacks(
        @Qualifier("aiHubAskSpringAIAgent") AiHubSpringAIAgent aiHubAskSpringAIAgent,
        @Qualifier("aiHubBuildSpringAIAgent") AiHubSpringAIAgent aiHubBuildSpringAIAgent) {

        return mode -> mode == Mode.BUILD
            ? aiHubBuildSpringAIAgent.pinnedToolCallbacks()
            : aiHubAskSpringAIAgent.pinnedToolCallbacks();
    }

    static List<ToolCallback> buildProjectToolCallbacks(
        ProjectTools projectTools, ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools,
        WorkflowExecutionTools workflowExecutionTools) {

        ToolCallback[] toolCallbacks = ToolCallbacks.from(
            projectTools, projectWorkflowLifecycleTools, workflowExecutionTools);

        return Arrays.stream(toolCallbacks)
            .filter(toolCallback -> !isUpdateWorkflow(toolCallback))
            .toList();
    }

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

    static List<ToolCallback> catalogToolCallbacks(
        AiHubModeToolCallbacks modeToolCallbacks, ObjectProvider<ComponentTools> componentToolsProvider,
        ObjectProvider<TaskTools> taskToolsProvider, ObjectProvider<TaskDispatcherTools> taskDispatcherToolsProvider) {

        Map<String, ToolCallback> toolCallbacks = new LinkedHashMap<>();

        addAbsent(toolCallbacks, modeToolCallbacks.unpinned());

        ComponentTools componentTools = componentToolsProvider.getIfAvailable();

        if (componentTools != null) {
            addAbsent(toolCallbacks, List.of(ToolCallbacks.from(componentTools)));
        }

        TaskTools taskTools = taskToolsProvider.getIfAvailable();

        if (taskTools != null) {
            addAbsent(toolCallbacks, List.of(ToolCallbacks.from(taskTools)));
        }

        TaskDispatcherTools taskDispatcherTools = taskDispatcherToolsProvider.getIfAvailable();

        if (taskDispatcherTools != null) {
            addAbsent(toolCallbacks, List.of(ToolCallbacks.from(taskDispatcherTools)));
        }

        Set<String> pinnedToolNames = modeToolCallbacks.pinnedToolNames();

        return toolCallbacks.values()
            .stream()
            .filter(toolCallback -> !pinnedToolNames.contains(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();
    }

    private static void addAbsent(Map<String, ToolCallback> toolCallbacksByName, List<ToolCallback> toolCallbacks) {
        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            toolCallbacksByName.putIfAbsent(toolDefinition.name(), toolCallback);
        }
    }

    static List<ToolCallback> checkWorkflowToolCallbacks(
        ObjectProvider<PersistedWorkflowChecker> persistedWorkflowCheckerProvider,
        ObjectProvider<ProjectService> projectServiceProvider) {

        PersistedWorkflowChecker persistedWorkflowChecker = persistedWorkflowCheckerProvider.getIfAvailable();
        ProjectService projectService = projectServiceProvider.getIfAvailable();

        if (persistedWorkflowChecker == null || projectService == null) {
            return List.of();
        }

        return List.of(ToolCallbacks.from(new CheckWorkflowTools(persistedWorkflowChecker, projectService)));
    }

    static List<ToolCallback> searchDocsToolCallbacks(
        ObjectProvider<EmbeddingProviderStatusProvider> embeddingProviderStatusProviderProvider,
        ObjectProvider<VectorStore> copilotPgVectorStoreProvider) {

        EmbeddingProviderStatusProvider embeddingProviderStatusProvider =
            embeddingProviderStatusProviderProvider.getIfAvailable();
        VectorStore copilotPgVectorStore = copilotPgVectorStoreProvider.getIfAvailable();

        if (embeddingProviderStatusProvider == null || copilotPgVectorStore == null) {
            return List.of();
        }

        return List.of(ToolCallbacks.from(new SearchDocsTools(embeddingProviderStatusProvider, copilotPgVectorStore)));
    }

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

    static List<ToolCallback> aiAgentFlatCrudToolCallbacks(
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider, boolean writable) {

        AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory = aiAgentToolCallbacksFactoryProvider
            .getIfAvailable();

        if (aiAgentToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? aiAgentToolCallbacksFactory.writeToolCallbacks()
            : aiAgentToolCallbacksFactory.readToolCallbacks();
    }

    static List<ToolCallback> assetFileFlatCrudToolCallbacks(
        ObjectProvider<AssetFileToolCallbacksFactory> assetFileToolCallbacksFactoryProvider, boolean writable) {

        AssetFileToolCallbacksFactory assetFileToolCallbacksFactory = assetFileToolCallbacksFactoryProvider
            .getIfAvailable();

        if (assetFileToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? assetFileToolCallbacksFactory.writeToolCallbacks()
            : assetFileToolCallbacksFactory.readToolCallbacks();
    }

    static List<ToolCallback> dataTableFlatCrudToolCallbacks(
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider, boolean writable) {

        DataTableToolCallbacksFactory dataTableToolCallbacksFactory = dataTableToolCallbacksFactoryProvider
            .getIfAvailable();

        if (dataTableToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? dataTableToolCallbacksFactory.writeToolCallbacks()
            : dataTableToolCallbacksFactory.readToolCallbacks();
    }

    static List<ToolCallback> knowledgeBaseFlatCrudToolCallbacks(
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider, boolean writable) {

        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = knowledgeBaseToolCallbacksFactoryProvider
            .getIfAvailable();

        if (knowledgeBaseToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? knowledgeBaseToolCallbacksFactory.writeToolCallbacks()
            : knowledgeBaseToolCallbacksFactory.readToolCallbacks();
    }

    static List<ToolCallback> contextStoreFlatCrudToolCallbacks(
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider, boolean writable) {

        ContextStoreToolCallbacksFactory contextStoreToolCallbacksFactory = contextStoreToolCallbacksFactoryProvider
            .getIfAvailable();

        if (contextStoreToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? contextStoreToolCallbacksFactory.writeToolCallbacks()
            : contextStoreToolCallbacksFactory.readToolCallbacks();
    }

    static List<ToolCallback> promoteToEnvironmentToolCallbacks(
        ObjectProvider<EnvironmentPromotionFacade> environmentPromotionFacadeProvider,
        ObjectProvider<EnvironmentService> environmentServiceProvider) {

        EnvironmentPromotionFacade environmentPromotionFacade = environmentPromotionFacadeProvider.getIfAvailable();
        EnvironmentService environmentService = environmentServiceProvider.getIfAvailable();

        if (environmentPromotionFacade == null || environmentService == null) {
            return List.of();
        }

        return Arrays.stream(PromotionResourceType.values())
            .map(promotionResourceType -> (ToolCallback) new PromoteToEnvironmentToolCallback(
                promotionResourceType, environmentPromotionFacade, environmentService))
            .toList();
    }

    static List<ToolCallback> apiCollectionFlatCrudToolCallbacks(
        ObjectProvider<ApiCollectionToolCallbacksFactory> apiCollectionToolCallbacksFactoryProvider, boolean writable) {

        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory = apiCollectionToolCallbacksFactoryProvider
            .getIfAvailable();

        if (apiCollectionToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? apiCollectionToolCallbacksFactory.writeToolCallbacks()
            : apiCollectionToolCallbacksFactory.readToolCallbacks();
    }

    static List<ToolCallback> componentRuleFlatCrudToolCallbacks(
        ObjectProvider<ComponentRuleToolCallbacksFactory> componentRuleToolCallbacksFactoryProvider, boolean writable) {

        ComponentRuleToolCallbacksFactory componentRuleToolCallbacksFactory = componentRuleToolCallbacksFactoryProvider
            .getIfAvailable();

        if (componentRuleToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? componentRuleToolCallbacksFactory.writeToolCallbacks()
            : componentRuleToolCallbacksFactory.readToolCallbacks();
    }

    static List<ToolCallback> subAgentToolCallbacks(
        ObjectProvider<AiHubSubAgentToolCallbackFactory> subAgentToolCallbackFactoriesProvider) {

        return subAgentToolCallbackFactoriesProvider.orderedStream()
            .map(AiHubSubAgentToolCallbackFactory::create)
            .toList();
    }

    static List<ToolCallback> openAiAgentTabToolCallbacks(
        ObjectProvider<AiAgentFacade> aiAgentFacadeProvider,
        @Nullable AiHubChatArtifactRecorder aiHubChatArtifactRecorder) {

        AiAgentFacade aiAgentFacade = aiAgentFacadeProvider.getIfAvailable();

        if (aiAgentFacade == null) {
            return List.of();
        }

        return List.of(new OpenAiAgentTabToolCallback(aiAgentFacade, aiHubChatArtifactRecorder));
    }

    static List<ToolCallback> intelligentToolCallbacks(
        IntelligentToolCatalog intelligentToolCatalog, Set<String> toolNames, IntelligentToolVariant variant,
        AiHubToolApprovalPolicy toolApprovalPolicy, WorkspaceFacade workspaceFacade,
        AiHubSessionMemory aiHubSessionMemory, @Nullable AiGuardrails aiGuardrails,
        @Nullable AiGuardrailMetrics aiGuardrailMetrics) {

        return intelligentToolCatalog.getByNames(
            toolNames, variant,
            (chatClient, definition) -> wrapDelegate(
                SubagentStepAdvisor.attach(AiHubGatedToolFilterAdvisor.filter(chatClient, toolApprovalPolicy)),
                definition, aiHubSessionMemory, aiGuardrails, aiGuardrailMetrics),
            (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(
                SubagentProgressToolCallback.wrap(new UserRequestForwardingToolCallback(toolCallback)),
                workspaceFacade));
    }

    static List<ToolCallback> openCustomComponentTabToolCallbacks(
        ObjectProvider<CustomComponentFacade> customComponentFacadeProvider,
        @Nullable AiHubChatArtifactRecorder aiHubChatArtifactRecorder) {

        CustomComponentFacade customComponentFacade = customComponentFacadeProvider.getIfAvailable();

        if (customComponentFacade == null) {
            return List.of();
        }

        return List.of(new OpenCustomComponentTabToolCallback(customComponentFacade, aiHubChatArtifactRecorder));
    }

    static List<ToolCallback> openCodeWorkflowTabToolCallbacks(
        ObjectProvider<ProjectCodeWorkflowFacade> projectCodeWorkflowFacadeProvider,
        @Nullable AiHubChatArtifactRecorder aiHubChatArtifactRecorder) {

        ProjectCodeWorkflowFacade projectCodeWorkflowFacade = projectCodeWorkflowFacadeProvider.getIfAvailable();

        if (projectCodeWorkflowFacade == null) {
            return List.of();
        }

        return List.of(new OpenCodeWorkflowTabToolCallback(projectCodeWorkflowFacade, aiHubChatArtifactRecorder));
    }

    static List<ToolCallback> openFileTabToolCallbacks(
        ObjectProvider<AssetFileFacade> assetFileFacadeProvider,
        @Nullable AiHubChatArtifactRecorder aiHubChatArtifactRecorder) {

        AssetFileFacade assetFileFacade = assetFileFacadeProvider.getIfAvailable();

        if (assetFileFacade == null) {
            return List.of();
        }

        return List.of(new OpenFileTabToolCallback(assetFileFacade, aiHubChatArtifactRecorder));
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

    private static ChatClient wrapDelegate(
        ChatClient chatClient, IntelligentToolDefinition definition, AiHubSessionMemory aiHubSessionMemory,
        @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics) {

        AgentType agentType = definition.agentType();

        return SubAgentContributingChatClient.wrap(
            chatClient, delegateContributors(agentType.key(), aiHubSessionMemory, aiGuardrails, aiGuardrailMetrics));
    }

    static List<SubAgentAdvisorContributor> delegateContributors(
        String agentTypeKey, AiHubSessionMemory aiHubSessionMemory, @Nullable AiGuardrails aiGuardrails,
        @Nullable AiGuardrailMetrics aiGuardrailMetrics) {

        List<SubAgentAdvisorContributor> contributors = new ArrayList<>();

        if (aiGuardrails != null && aiGuardrailMetrics != null) {
            contributors.add(new WorkspaceAdvisorContributor(aiGuardrails, aiGuardrailMetrics));
        }

        contributors.add(new SubAgentSessionMemoryContributor(aiHubSessionMemory, agentTypeKey));

        return List.copyOf(contributors);
    }

    private AiHubSpringAIAgent buildAgent(
        AiHubAgentType agentType, Mode mode, Resource systemPromptResource, List<ToolCallback> toolCallbacks,
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel,
        SecurityContextRehydrator securityContextRehydrator, AiHubApprovalGate approvalGate,
        ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<WebSearchToolCallbackProvider> webSearchToolCallbackProviderProvider,
        ObjectProvider<ResearchToolCallbackFactory> researchToolCallbackFactoryProvider,
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics,
        ObjectProvider<AiHubChatBindingToolCallbackResolver> chatBindingToolCallbackResolverProvider,
        ObjectProvider<AiAutoMemoryService> aiAutoMemoryServiceProvider,
        ObjectProvider<AiHubSkillsToolProvider> skillsToolProviderProvider)
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
        toolSearchToolCallAdvisorProvider.ifAvailable(builder::advisor);
        aiAutoMemoryServiceProvider.ifAvailable(
            aiAutoMemoryService -> builder.advisor(
                autoMemoryToolsAdvisor(aiAutoMemoryService, mode, securityContextRehydrator)));

        overrideChatClientResolverProvider.ifAvailable(builder::overrideChatClientResolver);
        webSearchToolCallbackProviderProvider.ifAvailable(builder::webSearchToolCallbackProvider);
        chatBindingToolCallbackResolverProvider.ifAvailable(builder::chatToolBindingResolver);

        if (aiGuardrails != null) {
            builder.aiGuardrails(aiGuardrails, aiGuardrailMetrics);
        }

        skillsToolProviderProvider.ifAvailable(builder::skillsToolProvider);
        researchToolCallbackFactoryProvider.ifAvailable(builder::researchToolCallbackFactory);

        return builder.build();
    }

    private static @Nullable AiGuardrailMetrics aiGuardrailMetrics(
        @Nullable AiGuardrails aiGuardrails, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        if (aiGuardrails == null) {
            return null;
        }

        return new AiGuardrailMetrics(meterRegistryProvider.getIfAvailable(), GuardrailSurface.AI_HUB);
    }

    private AiHubAutoMemoryToolsAdvisor autoMemoryToolsAdvisor(
        AiAutoMemoryService aiAutoMemoryService, Mode mode, SecurityContextRehydrator securityContextRehydrator) {

        boolean readOnly = mode != Mode.BUILD;

        return AiHubAutoMemoryToolsAdvisor.builder()
            .aiAutoMemoryService(aiAutoMemoryService)
            .memorySystemPrompt(
                readOnly ? promptAiHubAutoMemoryToolsReadOnlyResource : promptAiHubAutoMemoryToolsResource)
            .readOnly(readOnly)
            .toolCallbackWrapper(
                toolCallback -> AiHubToolCallbackWrappers.wrap(toolCallback, securityContextRehydrator))
            .build();
    }

    private static String readSystemPrompt(Resource systemPromptResource) {
        try (InputStream inputStream = systemPromptResource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }

    private static boolean isUpdateWorkflow(ToolCallback toolCallback) {
        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        return UPDATE_WORKFLOW_TOOL_NAME.equals(toolDefinition.name());
    }
}
