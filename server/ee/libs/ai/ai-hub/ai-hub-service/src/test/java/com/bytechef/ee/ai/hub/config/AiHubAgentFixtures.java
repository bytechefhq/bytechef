/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.config.InteractivePickerToolCallbacks;
import com.bytechef.ai.copilot.tool.PersistedWorkflowChecker;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScopedProjectGuard;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.datatable.DataTableToolCallbacksFactory;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.toolsearch.AiHubGlobalToolCatalog;
import com.bytechef.ee.automation.ai.tool.ApiCollectionToolCallbacksFactory;
import com.bytechef.ee.automation.ai.tool.componentrule.ComponentRuleToolCallbacksFactory;
import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubAgentFixtures {

    public final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);
    public final AiHubToolApprovalService approvalService = mock(AiHubToolApprovalService.class);
    public final AiHubChatService chatService = mock(AiHubChatService.class);
    public final AiHubChatAccessPolicy chatAccessPolicy = mock(AiHubChatAccessPolicy.class);
    public final AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);
    public final CustomComponentFacade customComponentFacade = mock(CustomComponentFacade.class);
    public final ProjectCodeWorkflowFacade projectCodeWorkflowFacade = mock(ProjectCodeWorkflowFacade.class);
    public final ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory =
        new ApiCollectionToolCallbacksFactory(mock(ApiCollectionFacade.class));
    public final ComponentRuleToolCallbacksFactory componentRuleToolCallbacksFactory =
        new ComponentRuleToolCallbacksFactory(
            mock(ComponentRuleService.class), mock(ComponentDefinitionService.class),
            mock(ClusterElementDefinitionService.class), mock(Evaluator.class));
    public final AssetFileToolCallbacksFactory assetFileToolCallbacksFactory = new AssetFileToolCallbacksFactory(
        mock(AssetFileFacade.class), null);
    public final DataTableToolCallbacksFactory dataTableToolCallbacksFactory = new DataTableToolCallbacksFactory(
        mock(WorkspaceDataTableFacade.class));
    public final KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory =
        new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentFacade.class));
    public final ContextStoreToolCallbacksFactory contextStoreToolCallbacksFactory =
        new ContextStoreToolCallbacksFactory(
            mock(WorkspaceContextStoreSourceService.class), mock(ContextStoreQueryService.class),
            mock(ContextStoreSourceFacade.class), mock(ContextStoreFacade.class),
            mock(ContextStoreSemanticSearchService.class), mock(ClusterElementDefinitionService.class));
    public final EnvironmentPromotionFacade environmentPromotionFacade = mock(EnvironmentPromotionFacade.class);
    public final EnvironmentService environmentService = mock(EnvironmentService.class);
    public final ChatModel chatModel = mock(ChatModel.class);
    public final ComponentTools componentTools = mock(ComponentTools.class);
    public final PersistedWorkflowChecker persistedWorkflowChecker = mock(PersistedWorkflowChecker.class);
    public final ProjectService projectService = mock(ProjectService.class);
    public final AiHubToolApprovalPolicy policy = mock(AiHubToolApprovalPolicy.class);
    public final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    public final ProjectFacade projectFacade = mock(ProjectFacade.class);
    public final ProjectWorkflowFacade projectWorkflowFacade = mock(ProjectWorkflowFacade.class);
    public final ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
    public final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    public final AiHubApprovalGate approvalGate = new AiHubApprovalGate(policy, approvalService, chatService);
    public final IntelligentToolCatalog intelligentToolCatalog = mock(IntelligentToolCatalog.class);
    public final InteractivePickerToolCallbacks interactivePickerToolCallbacks;
    public final ProjectTools projectTools = new ProjectTools(
        mock(ProjectCategoryFacade.class), projectDeploymentFacade, projectFacade, mock(ProjectTagFacade.class),
        mock(ScopedProjectGuard.class), workspaceFacade);
    public final ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools =
        new ProjectWorkflowLifecycleTools(projectWorkflowTools);
    public final InFlightAiHubRunRegistry inFlightRunRegistry = new InFlightAiHubRunRegistry();
    public final ReadProjectTools readProjectTools = new ReadProjectTools(projectTools);
    public final ReadProjectWorkflowTools readProjectWorkflowTools = new ReadProjectWorkflowTools(projectWorkflowTools);
    public final SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);
    public final EmbeddingProviderStatusProvider embeddingProviderStatusProvider =
        mock(EmbeddingProviderStatusProvider.class);
    public final VectorStore copilotPgVectorStore = mock(VectorStore.class);
    public final WebSearchToolCallbackProvider webSearchToolCallbackProvider =
        mock(WebSearchToolCallbackProvider.class);
    public final WorkflowExecutionTools workflowExecutionTools = new WorkflowExecutionTools(
        mock(ProjectWorkflowExecutionFacade.class), mock(ScopedProjectGuard.class));
    public final AiHubSessionMemory sessionMemory = new AiHubSessionMemory(
        InMemorySessionRepository.builder()
            .build(),
        null);

    public final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private @Nullable AiGuardrails aiGuardrails;

    private final AiHubConfiguration configuration = new AiHubConfiguration(
        new ClassPathResource("prompt_ai_hub_ask.txt"), new ClassPathResource("prompt_ai_hub_build.txt"));

    @SuppressWarnings("unchecked")
    public AiHubAgentFixtures(Set<String> catalogToolNames) {
        when(securityContextRehydrator.withUserSecurityContext(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1, Supplier.class)
                .get());

        interactivePickerToolCallbacks = new InteractivePickerToolCallbacks(
            mock(ConnectionDefinitionService.class), mock(WorkspaceConnectionFacade.class),
            mock(ComponentDefinitionService.class), mock(ActionDefinitionService.class),
            mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class),
            mock(TriggerDefinitionFacade.class), mock(ClusterElementDefinitionService.class),
            mock(ClusterElementDefinitionFacade.class), new PropertyOptionsResolver(securityContextRehydrator),
            mock(ObjectProvider.class), mock(ScopedConnectionGuard.class));

        when(intelligentToolCatalog.getByNames(anySet(), any(IntelligentToolVariant.class), any(), any()))
            .thenAnswer(invocation -> {
                Set<String> names = invocation.getArgument(0);
                BiFunction<ToolCallback, Object, ToolCallback> callbackDecorator = invocation.getArgument(3);

                List<ToolCallback> toolCallbacks = new ArrayList<>();

                for (String name : names) {
                    if (catalogToolNames.contains(name)) {
                        toolCallbacks.add(callbackDecorator.apply(stubToolCallback(name), null));
                    }
                }

                return toolCallbacks;
            });
    }

    public static AiHubAgentFixtures withBuildWorkflow() {
        return new AiHubAgentFixtures(
            Set.of(
                "buildWorkflow", "importWorkflow", "buildCustomComponent", "buildCodeWorkflow",
                "explainCustomComponent",
                "explainCodeWorkflow"));
    }

    public AiHubAgentFixtures withAiGuardrails(AiGuardrails aiGuardrails) {
        this.aiGuardrails = aiGuardrails;

        return this;
    }

    public AiHubSpringAIAgent askAgent() throws AGUIException {
        return askAgent(chatModel, emptyProvider());
    }

    public AiHubSpringAIAgent askAgent(
        ChatModel defaultChatModel, AiHubSpringAIAgent.OverrideChatClientResolver overrideChatClientResolver)
        throws AGUIException {

        return askAgent(defaultChatModel, providerOf(overrideChatClientResolver));
    }

    public AiHubSpringAIAgent buildAgent() throws AGUIException {
        return buildAgent(chatModel, emptyProvider());
    }

    public AiHubSpringAIAgent buildAgent(
        ChatModel defaultChatModel, AiHubSpringAIAgent.OverrideChatClientResolver overrideChatClientResolver)
        throws AGUIException {

        return buildAgent(defaultChatModel, providerOf(overrideChatClientResolver));
    }

    private AiHubSpringAIAgent askAgent(
        ChatModel defaultChatModel,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return configuration.aiHubAskSpringAIAgent(
            sessionMemory, defaultChatModel, askToolCallbacks(), securityContextRehydrator, approvalGate,
            inFlightRunRegistryProvider(), overrideChatClientResolverProvider, webSearchToolCallbackProviderProvider(),
            toolSearchAdvisorProvider(), availableProvider(aiGuardrails), availableProvider(meterRegistry));
    }

    private AiHubSpringAIAgent buildAgent(
        ChatModel defaultChatModel,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return configuration.aiHubBuildSpringAIAgent(
            sessionMemory, defaultChatModel, buildToolCallbacks(), securityContextRehydrator, approvalGate,
            inFlightRunRegistryProvider(), overrideChatClientResolverProvider, webSearchToolCallbackProviderProvider(),
            toolSearchAdvisorProvider(), availableProvider(aiGuardrails), availableProvider(meterRegistry));
    }

    public AiHubModeToolCallbacks askToolCallbacks() {
        return configuration.aiHubAskToolCallbacks(
            readProjectTools, readProjectWorkflowTools, workflowExecutionTools, componentToolsProvider(),
            persistedWorkflowCheckerProvider(), projectServiceProvider(), deploymentToolCallbacksFactoryProvider(),
            aiAgentToolCallbacksFactoryProvider(), availableProvider(assetFileToolCallbacksFactory),
            availableProvider(dataTableToolCallbacksFactory), availableProvider(knowledgeBaseToolCallbacksFactory),
            availableProvider(contextStoreToolCallbacksFactory), availableProvider(apiCollectionToolCallbacksFactory),
            availableProvider(componentRuleToolCallbacksFactory),
            availableProvider(aiAgentFacade), availableProvider(customComponentFacade),
            availableProvider(projectCodeWorkflowFacade), interactivePickerToolCallbacks, intelligentToolCatalog,
            workspaceFacade, projectWorkflowFacade, chatService, chatAccessPolicy, policy, sessionMemory,
            availableProvider(embeddingProviderStatusProvider), availableProvider(copilotPgVectorStore),
            availableProvider(aiGuardrails), availableProvider(meterRegistry));
    }

    public AiHubGlobalToolCatalog askGlobalToolCatalog() {
        return configuration.aiHubAskGlobalToolCatalog(
            askToolCallbacks(), componentToolsProvider(), availableProvider(null), availableProvider(null));
    }

    public AiHubGlobalToolCatalog buildGlobalToolCatalog() {
        return configuration.aiHubBuildGlobalToolCatalog(
            buildToolCallbacks(), componentToolsProvider(), availableProvider(null), availableProvider(null));
    }

    public AiHubModeToolCallbacks buildToolCallbacks() {
        return configuration.aiHubBuildToolCallbacks(
            projectTools, projectWorkflowLifecycleTools, workflowExecutionTools, componentToolsProvider(),
            persistedWorkflowCheckerProvider(), projectServiceProvider(), deploymentToolCallbacksFactoryProvider(),
            aiAgentToolCallbacksFactoryProvider(), availableProvider(assetFileToolCallbacksFactory),
            availableProvider(dataTableToolCallbacksFactory), availableProvider(knowledgeBaseToolCallbacksFactory),
            availableProvider(contextStoreToolCallbacksFactory), availableProvider(apiCollectionToolCallbacksFactory),
            availableProvider(componentRuleToolCallbacksFactory),
            availableProvider(aiAgentFacade), availableProvider(customComponentFacade),
            availableProvider(projectCodeWorkflowFacade), interactivePickerToolCallbacks, intelligentToolCatalog,
            workspaceFacade,
            projectWorkflowFacade,
            projectDeploymentFacade, availableProvider(environmentPromotionFacade),
            availableProvider(environmentService),
            chatService, chatAccessPolicy, artifactRecorder, policy, sessionMemory,
            availableProvider(embeddingProviderStatusProvider), availableProvider(copilotPgVectorStore),
            availableProvider(aiGuardrails), availableProvider(meterRegistry));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<InFlightAiHubRunRegistry> inFlightRunRegistryProvider() {
        ObjectProvider<InFlightAiHubRunRegistry> provider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            invocation.<Consumer<InFlightAiHubRunRegistry>>getArgument(0)
                .accept(inFlightRunRegistry);

            return null;
        }).when(provider)
            .ifAvailable(any());

        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<WebSearchToolCallbackProvider> webSearchToolCallbackProviderProvider() {
        ObjectProvider<WebSearchToolCallbackProvider> provider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            invocation.<Consumer<WebSearchToolCallbackProvider>>getArgument(0)
                .accept(webSearchToolCallbackProvider);

            return null;
        }).when(provider)
            .ifAvailable(any());

        return provider;
    }

    @SuppressWarnings("unchecked")
    public ObjectProvider<PersistedWorkflowChecker> persistedWorkflowCheckerProvider() {
        ObjectProvider<PersistedWorkflowChecker> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(persistedWorkflowChecker);

        return provider;
    }

    @SuppressWarnings("unchecked")
    public ObjectProvider<ProjectService> projectServiceProvider() {
        ObjectProvider<ProjectService> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(projectService);

        return provider;
    }

    @SuppressWarnings("unchecked")
    public ObjectProvider<ComponentTools> componentToolsProvider() {
        ObjectProvider<ComponentTools> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(componentTools);

        return provider;
    }

    @SuppressWarnings("unchecked")
    public ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider() {
        ObjectProvider<DeploymentToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(
            new DeploymentToolCallbacksFactory(projectDeploymentFacade, mock(ProjectWorkflowService.class)));

        return provider;
    }

    public ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider() {
        return availableProvider(
            new AiAgentToolCallbacksFactory(aiAgentFacade, mock(ClusterElementDefinitionService.class), null, null));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> availableProvider(@Nullable T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(bean);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchAdvisorProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> providerOf(
        AiHubSpringAIAgent.OverrideChatClientResolver overrideChatClientResolver) {

        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> provider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            invocation.<Consumer<AiHubSpringAIAgent.OverrideChatClientResolver>>getArgument(0)
                .accept(overrideChatClientResolver);

            return null;
        }).when(provider)
            .ifAvailable(any());

        return provider;
    }

    private static ToolCallback stubToolCallback(String name) {
        return FunctionToolCallback.builder(name, (String input) -> "{\"result\":\"" + name + "\"}")
            .description(name)
            .inputType(String.class)
            .build();
    }
}
