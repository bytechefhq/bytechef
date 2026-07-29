/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.tool.AskUserQuestionToolCallback;
import com.bytechef.ai.copilot.tool.ClusterElementAgentToolCallback;
import com.bytechef.ai.copilot.tool.CodeEditorAgentToolCallback;
import com.bytechef.ai.copilot.tool.CodeWorkflowAgentToolCallback;
import com.bytechef.ai.copilot.tool.ContextStoreAgentToolCallback;
import com.bytechef.ai.copilot.tool.ConverterAgentToolCallback;
import com.bytechef.ai.copilot.tool.CreateConnectionToolCallback;
import com.bytechef.ai.copilot.tool.CustomComponentAgentToolCallback;
import com.bytechef.ai.copilot.tool.DataTableAgentToolCallback;
import com.bytechef.ai.copilot.tool.KnowledgeBaseAgentToolCallback;
import com.bytechef.ai.copilot.tool.ListConnectionsForComponentToolCallback;
import com.bytechef.ai.copilot.tool.LookupActionPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.LookupTriggerPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.SelectConnectionToolCallback;
import com.bytechef.ai.copilot.tool.SelectPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.SelectTriggerPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.SkillsAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkflowEditorAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkflowExecutionAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkspaceCopilotConnectionLister;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.BuiltInSessionRepositoryFactory;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.BuiltInSessionRepositoryFactory.BuiltInSessionRepository;
import com.bytechef.ee.ai.hub.agent.AiHubRoutingAgent;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.agent.WebhookBridgeAgent;
import com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry;
import com.bytechef.ee.ai.hub.agent.WorkflowChatGuard;
import com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry;
import com.bytechef.ee.ai.hub.artifact.ArtifactGeneratorRegistry;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.progress.ProgressReportingToolCallback;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactService;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
import com.bytechef.ee.ai.hub.tool.AiHubTaskArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AttachTaskToolToolCallback;
import com.bytechef.ee.ai.hub.tool.CloneAiHubPersonalAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.CloneApiCollectionToolCallback;
import com.bytechef.ee.ai.hub.tool.CloneAssetFileToolCallback;
import com.bytechef.ee.ai.hub.tool.CloneMcpProjectToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateAiHubPersonalAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateApiCollectionToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateAssetFileToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateMcpProjectToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateProjectDeploymentToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateWorkflowChatToolCallback;
import com.bytechef.ee.ai.hub.tool.DeleteAiHubPersonalAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.DeleteProjectDeploymentToolCallback;
import com.bytechef.ee.ai.hub.tool.GetAssetFileContentToolCallback;
import com.bytechef.ee.ai.hub.tool.ListAiHubPersonalAgentsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListAiHubTasksToolCallback;
import com.bytechef.ee.ai.hub.tool.ListApiCollectionsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListAssetFilesToolCallback;
import com.bytechef.ee.ai.hub.tool.ListChatWorkflowsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListMcpServersToolCallback;
import com.bytechef.ee.ai.hub.tool.ListProjectDeploymentsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListTaskToolsToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenAiHubPersonalAgentTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenCodeWorkflowTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenCustomComponentTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenDataTableTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenFileTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenKnowledgeBaseTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenSkillTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowChatTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowTabToolCallback;
import com.bytechef.ee.ai.hub.tool.PromoteWorkflowToolCallback;
import com.bytechef.ee.ai.hub.tool.QueryDataTableToolCallback;
import com.bytechef.ee.ai.hub.tool.RemoveTaskToolToolCallback;
import com.bytechef.ee.ai.hub.tool.RollbackProjectDeploymentToolCallback;
import com.bytechef.ee.ai.hub.tool.RunChatWorkflowToolCallback;
import com.bytechef.ee.ai.hub.tool.ToggleProjectDeploymentToolCallback;
import com.bytechef.ee.ai.hub.tool.UpdateAiHubPersonalAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.UpdateProjectDeploymentToolCallback;
import com.bytechef.ee.ai.hub.tool.memory.DbAutoMemoryDirectoryOps;
import com.bytechef.ee.ai.hub.tool.memory.DbMemoryResourceResolver;
import com.bytechef.ee.ai.hub.toolsearch.AiHubGlobalToolCatalog;
import com.bytechef.ee.ai.hub.toolsearch.AiHubTaskBindingToolCallbackResolver;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.util.Source;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.platform.ai.agent.memory.AutoMemoryTools;
import com.bytechef.platform.ai.agent.memory.AutoMemoryToolsAdvisor;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI Hub LLM agent wiring — declares the {@code ai_hub_ask} / {@code ai_hub_build} routing agents (registered as AG-UI
 * {@link com.agui.server.LocalAgent} beans), the underlying Spring AI agents that the routing layer delegates to for
 * {@code kind = STANDARD} turns, and the {@link WebhookBridgeAgent} that handles {@code kind = WORKFLOW_CHAT} turns.
 *
 * <p>
 * The {@code @Bean} methods consume both CC domain types (task / memory / personal-agent services + Command
 * Center–owned tool callbacks) and shared LLM infrastructure (chat memory advisor, tool search advisor, project /
 * data-table / knowledge-base / mcp-project tool callbacks).
 * </p>
 *
 * <p>
 * Gated on {@code bytechef.ai.hub.enabled=true} so a deployment can toggle the AI Hub surface independently of any
 * other AI agents that may be enabled in the same process.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubConfiguration {

    private final Resource promptAiHubAskResource;
    private final Resource promptAiHubAutoMemoryToolsResource;
    private final Resource promptAiHubBuildResource;
    private final State state = new State();

    @SuppressFBWarnings("EI")
    public AiHubConfiguration(
        @Value("classpath:prompt_ai_hub_ask.txt") Resource promptAiHubAskResource,
        @Value("classpath:prompt/ai_hub_auto_memory_tools_system_prompt.md") Resource promptAiHubAutoMemoryToolsResource,
        @Value("classpath:prompt_ai_hub_build.txt") Resource promptAiHubBuildResource) {

        this.promptAiHubAskResource = promptAiHubAskResource;
        this.promptAiHubAutoMemoryToolsResource = promptAiHubAutoMemoryToolsResource;
        this.promptAiHubBuildResource = promptAiHubBuildResource;
    }

    // defaultCandidate = false: this is the LLM delegate the ai_hub_ask AiHubRoutingAgent wraps. It shares the
    // router's agentId (ai_hub_ask) on purpose — AgentTypeRegistry.fromKey(getAgentId()) needs it for LLM cost
    // attribution — so it
    // must stay out of the chat controllers' List<LocalAgent> autowiring (toMap would throw on the duplicate id). The
    // router still injects it through its explicit @Qualifier("aiHubAskSpringAIAgent").
    @Bean(defaultCandidate = false)
    AiHubSpringAIAgent aiHubAskSpringAIAgent(
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel, ObjectProvider<ToolCallback> toolCallbackProvider,
        @Qualifier("researchChatClient") ObjectProvider<ChatClient> researchChatClientProvider,
        @Qualifier("skillsAskSubAgentChatClient") ObjectProvider<ChatClient> skillsAskSubAgentChatClientProvider,
        @Qualifier("contextStoreAskSubAgentChatClient") //
        ObjectProvider<ChatClient> contextStoreAskSubAgentChatClientProvider,
        @Qualifier("knowledgeBaseAskSubAgentChatClient") //
        ObjectProvider<ChatClient> knowledgeBaseAskSubAgentChatClientProvider,
        @Qualifier("dataTableAskSubAgentChatClient") //
        ObjectProvider<ChatClient> dataTableAskSubAgentChatClientProvider,
        @Qualifier("clusterElementAskSubAgentChatClient") //
        ObjectProvider<ChatClient> clusterElementAskSubAgentChatClientProvider,
        @Qualifier("codeEditorAskSubAgentChatClient") //
        ObjectProvider<ChatClient> codeEditorAskSubAgentChatClientProvider,
        @Qualifier("workflowEditorAskSubAgentChatClient") //
        ObjectProvider<ChatClient> workflowEditorAskSubAgentChatClientProvider,
        @Qualifier("workflowExecutionAskSubAgentChatClient") //
        ObjectProvider<ChatClient> workflowExecutionAskSubAgentChatClientProvider,
        @Qualifier("customComponentAskSubAgentChatClient") //
        ObjectProvider<ChatClient> customComponentAskSubAgentChatClientProvider,
        @Qualifier("codeWorkflowAskSubAgentChatClient") //
        ObjectProvider<ChatClient> codeWorkflowAskSubAgentChatClientProvider,
        ArtifactGeneratorRegistry artifactGeneratorRegistry, AiHubTaskService taskService,
        AiAutoMemoryService aiHubMemoryService,
        DataTableService dataTableService,
        DataTableRowService dataTableRowService,
        AiHubTaskToolFacade taskToolFacade,
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService,
        WorkspaceConnectionFacade workspaceConnectionFacade,
        ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade,
        SecurityContextRehydrator securityContextRehydrator,
        PropertyOptionsResolver propertyOptionsResolver,
        ObjectProvider<ApiCollectionFacade> apiCollectionFacadeProvider,
        ObjectProvider<AiHubPersonalAgentService> aiHubPersonalAgentServiceProvider,
        @Qualifier("aiHubAskToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        ObjectProvider<AiHubTaskBindingToolCallbackResolver> taskBindingToolCallbackResolverProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        AiHubToolAttachMetrics aiHubToolAttachMetrics, JsonMapper jsonMapper)
        throws AGUIException {

        String name = Source.AI_HUB.name() + "_" + Mode.ASK.name();

        List<ToolCallback> toolCallbacks = new ArrayList<>(toolCallbackProvider.orderedStream()
            .toList());

        researchChatClientProvider.ifAvailable(
            researchChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    ResearchConfiguration.createResearchToolCallback(researchChatClient), "research")));

        toolCallbacks.add(new OpenFileTabToolCallback());
        // ASK mode is read-only (never builds workflows), so no server-side artifact recorder is wired —
        // the client still records the reference when the tab opens.
        toolCallbacks.add(new OpenWorkflowTabToolCallback(null));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback());
        toolCallbacks.add(new OpenDataTableTabToolCallback(null));
        toolCallbacks.add(new OpenKnowledgeBaseTabToolCallback(null));
        toolCallbacks.add(new OpenSkillTabToolCallback(null));
        toolCallbacks.add(new OpenCustomComponentTabToolCallback(null));
        toolCallbacks.add(new OpenCodeWorkflowTabToolCallback(null));
        toolCallbacks.add(
            new QueryDataTableToolCallback(
                artifactGeneratorRegistry, taskService, dataTableRowService, dataTableService));
        toolCallbacks.add(
            new AttachTaskToolToolCallback(taskService, taskToolFacade, connectionService, aiHubToolAttachMetrics));
        toolCallbacks.add(
            new RemoveTaskToolToolCallback(taskService, taskToolFacade));
        toolCallbacks.add(new AskUserQuestionToolCallback(aiHubToolAttachMetrics));

        // Task / connection state visibility — read-only. Lets the LLM avoid duplicate attaches and pick
        // existing connections before escalating to createConnection.
        registerToolAttachStateVisibilityToolCallbacks(
            toolCallbacks, taskService, taskToolFacade, componentDefinitionService, connectionDefinitionService,
            workspaceConnectionFacade, actionDefinitionService, actionDefinitionFacade, triggerDefinitionService,
            triggerDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics, jsonMapper);

        // Resource-discovery tools surface workspace state the LLM may want to reference but doesn't yet have a
        // way to enumerate. Read-only — both the ASK and BUILD agents register them so a casual ASK turn can
        // resolve "the staging customers API" / "my last research thread" to a concrete id without forcing the
        // user to switch into BUILD just to look something up. Workflow-execution lookups are now delegated to
        // the workflow_execution_agent specialist (registered via registerCopilotSubAgentToolCallbacks).
        toolCallbacks.add(new ListAiHubTasksToolCallback(taskService));

        apiCollectionFacadeProvider.ifAvailable(
            apiCollectionFacade -> toolCallbacks.add(new ListApiCollectionsToolCallback(apiCollectionFacade)));

        aiHubPersonalAgentServiceProvider.ifAvailable(aiHubPersonalAgentService -> {
            toolCallbacks.add(new ListAiHubPersonalAgentsToolCallback(aiHubPersonalAgentService));
            toolCallbacks.add(new OpenAiHubPersonalAgentTabToolCallback(aiHubPersonalAgentService,
                taskService));
        });

        // Copilot specialist sub-agent delegation. Each is registered only when its backing ChatClient
        // bean is present (the Copilot gate bytechef.ai.copilot.enabled is independent of AI Hub's
        // gate — if Copilot is disabled the beans are absent and the registrations are silently
        // skipped). Converter is BUILD-only and passed as null here.
        registerCopilotSubAgentToolCallbacks(
            toolCallbacks, skillsAskSubAgentChatClientProvider, contextStoreAskSubAgentChatClientProvider,
            knowledgeBaseAskSubAgentChatClientProvider, dataTableAskSubAgentChatClientProvider,
            clusterElementAskSubAgentChatClientProvider,
            codeEditorAskSubAgentChatClientProvider, workflowEditorAskSubAgentChatClientProvider, null,
            workflowExecutionAskSubAgentChatClientProvider, customComponentAskSubAgentChatClientProvider,
            codeWorkflowAskSubAgentChatClientProvider);

        AiHubSpringAIAgent.Builder builder = AiHubSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptAiHubAskResource))
            .toolCallbacks(toolCallbacks)
            .state(state)
            // Tenant + SecurityContext rehydration on Reactor scheduler threads so every tool runs under
            // the invoking tenant and user's SecurityContext. Without this, @PreAuthorize-protected facade
            // calls (ProjectFacadeImpl, etc.) throw AuthorizationDeniedException on bounded-elastic threads.
            .securityContextRehydrator(securityContextRehydrator);

        // Session-backed conversation memory, INSIDE the tool-calling loop (order > tool-search advisor's) so the
        // full tool request/response transcript is persisted and rehydrated per iteration. The tool-search advisor's
        // own in-loop history is disabled (PinnedToolSearchToolCallingAdvisor) to avoid double-writing.
        builder.advisor(aiHubSessionMemory.createSessionMemoryAdvisor());

        toolSearchToolCallAdvisorProvider.ifAvailable(builder::advisor);

        // ASK mode gets the same DB-backed auto-memory as BUILD so it can recall (and record) memories while
        // answering. The memory tools are added to the agent's static tool list, which
        // PinnedToolSearchToolCallingAdvisor
        // pins in full, so the tool-search narrowing doesn't strip them.
        builder.advisor(
            AutoMemoryToolsAdvisor.builder()
                .autoMemoryTools(
                    new AutoMemoryTools(
                        new DbMemoryResourceResolver(aiHubMemoryService),
                        new DbAutoMemoryDirectoryOps(aiHubMemoryService)))
                .memorySystemPrompt(promptAiHubAutoMemoryToolsResource)
                .build());

        taskBindingToolCallbackResolverProvider.ifAvailable(builder::taskToolBindingResolver);

        // Per-personal-agent LLM model override. Bean is only present when AI Gateway is enabled; absent → no
        // override capability, agents fall back to workspace default ChatClient (unchanged behaviour).
        overrideChatClientResolverProvider.ifAvailable(builder::overrideChatClientResolver);

        return builder.build();
    }

    // defaultCandidate = false: the ai_hub_build LLM delegate wrapped by the ai_hub_build AiHubRoutingAgent. Same
    // rationale as aiHubAskSpringAIAgent — shares the router's agentId for cost attribution, so it's kept out of the
    // controllers' List<LocalAgent> and injected only through @Qualifier("aiHubBuildSpringAIAgent").
    @Bean(defaultCandidate = false)
    AiHubSpringAIAgent aiHubBuildSpringAIAgent(
        AiHubSessionMemory aiHubSessionMemory, ChatModel chatModel, ObjectProvider<ToolCallback> toolCallbackProvider,
        @Qualifier("researchChatClient") ObjectProvider<ChatClient> researchChatClientProvider,
        @Qualifier("dataAnalystChatClient") ObjectProvider<ChatClient> dataAnalystChatClientProvider,
        @Qualifier("imageGeneratorChatClient") ObjectProvider<ChatClient> imageGeneratorChatClientProvider,
        @Qualifier("slideBuilderChatClient") ObjectProvider<ChatClient> slideBuilderChatClientProvider,
        @Qualifier("skillsBuildSubAgentChatClient") ObjectProvider<ChatClient> skillsBuildSubAgentChatClientProvider,
        @Qualifier("contextStoreBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> contextStoreBuildSubAgentChatClientProvider,
        @Qualifier("knowledgeBaseBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> knowledgeBaseBuildSubAgentChatClientProvider,
        @Qualifier("dataTableBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> dataTableBuildSubAgentChatClientProvider,
        @Qualifier("clusterElementBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> clusterElementBuildSubAgentChatClientProvider,
        @Qualifier("codeEditorBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> codeEditorBuildSubAgentChatClientProvider,
        @Qualifier("workflowEditorBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> workflowEditorBuildSubAgentChatClientProvider,
        @Qualifier("workflowExecutionBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> workflowExecutionBuildSubAgentChatClientProvider,
        @Qualifier("converterBuildSubAgentChatClientSupplier") //
        ObjectProvider<Supplier<ChatClient>> converterBuildSubAgentChatClientSupplierProvider,
        @Qualifier("customComponentBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> customComponentBuildSubAgentChatClientProvider,
        @Qualifier("codeWorkflowBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> codeWorkflowBuildSubAgentChatClientProvider,
        ArtifactGeneratorRegistry artifactGeneratorRegistry,
        AssetFileFacade assetFileFacade, AiHubTaskArtifactService taskArtifactService,
        AiHubTaskArtifactRecorder aiHubTaskArtifactRecorder,
        AiHubTaskService taskService, AiAutoMemoryService aiHubMemoryService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService,
        TriggerDefinitionService triggerDefinitionService,
        WorkflowFacade workflowFacade, WorkflowService workflowService, DataTableService dataTableService,
        DataTableRowService dataTableRowService,
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService,
        WorkspaceConnectionFacade workspaceConnectionFacade,
        ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionFacade triggerDefinitionFacade,
        SecurityContextRehydrator securityContextRehydrator,
        PropertyOptionsResolver propertyOptionsResolver,
        AiHubTaskToolFacade taskToolFacade,
        ObjectProvider<ApiCollectionFacade> apiCollectionFacadeProvider,
        ObjectProvider<McpProjectFacade> mcpProjectFacadeProvider,
        ObjectProvider<com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade> workspaceMcpServerFacadeProvider,
        ObjectProvider<AiHubPersonalAgentService> aiHubPersonalAgentServiceProvider,
        @Qualifier("aiHubBuildToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        ObjectProvider<AiHubTaskBindingToolCallbackResolver> taskBindingToolCallbackResolverProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        AiHubToolAttachMetrics aiHubToolAttachMetrics, JsonMapper jsonMapper)
        throws AGUIException {

        String name = Source.AI_HUB.name() + "_" + Mode.BUILD.name();

        List<ToolCallback> toolCallbacks = new ArrayList<>(toolCallbackProvider.orderedStream()
            .toList());

        registerSubAgentToolCallbacks(
            toolCallbacks, researchChatClientProvider, dataAnalystChatClientProvider,
            imageGeneratorChatClientProvider, slideBuilderChatClientProvider, assetFileFacade);

        toolCallbacks.add(new OpenFileTabToolCallback());
        toolCallbacks.add(new OpenWorkflowTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback());
        toolCallbacks.add(new OpenDataTableTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(new OpenKnowledgeBaseTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(new OpenSkillTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(new OpenCustomComponentTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(new OpenCodeWorkflowTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(
            new QueryDataTableToolCallback(
                artifactGeneratorRegistry, taskService, dataTableRowService, dataTableService));
        toolCallbacks.add(
            new ListChatWorkflowsToolCallback(
                projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService,
                triggerDefinitionService, workflowFacade, workflowService));
        toolCallbacks.add(
            new RunChatWorkflowToolCallback(
                projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService,
                workflowFacade, workflowService, taskArtifactService));
        toolCallbacks.add(new CreateWorkflowChatToolCallback(taskService));

        aiHubPersonalAgentServiceProvider.ifAvailable(aiHubPersonalAgentService -> {
            toolCallbacks.add(new ListAiHubPersonalAgentsToolCallback(aiHubPersonalAgentService));
            toolCallbacks.add(new OpenAiHubPersonalAgentTabToolCallback(aiHubPersonalAgentService,
                taskService));
            toolCallbacks.add(new CreateAiHubPersonalAgentToolCallback(aiHubPersonalAgentService));
            toolCallbacks.add(new UpdateAiHubPersonalAgentToolCallback(aiHubPersonalAgentService));
            toolCallbacks.add(new DeleteAiHubPersonalAgentToolCallback(aiHubPersonalAgentService));
            toolCallbacks.add(new CloneAiHubPersonalAgentToolCallback(aiHubPersonalAgentService));
        });

        // Copilot specialist sub-agent delegation. Write-capable variants for the BUILD agent, plus
        // the BUILD-only Converter sub-agent. Skips registrations when the corresponding ChatClient
        // bean is absent (Copilot disabled).
        registerCopilotSubAgentToolCallbacks(
            toolCallbacks, skillsBuildSubAgentChatClientProvider, contextStoreBuildSubAgentChatClientProvider,
            knowledgeBaseBuildSubAgentChatClientProvider, dataTableBuildSubAgentChatClientProvider,
            clusterElementBuildSubAgentChatClientProvider,
            codeEditorBuildSubAgentChatClientProvider, workflowEditorBuildSubAgentChatClientProvider,
            converterBuildSubAgentChatClientSupplierProvider, workflowExecutionBuildSubAgentChatClientProvider,
            customComponentBuildSubAgentChatClientProvider, codeWorkflowBuildSubAgentChatClientProvider);
        toolCallbacks.add(new CreateConnectionToolCallback(componentDefinitionService));
        toolCallbacks.add(new SelectConnectionToolCallback(componentDefinitionService));
        toolCallbacks.add(new ListProjectDeploymentsToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new CreateProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new UpdateProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new DeleteProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new RollbackProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new ToggleProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new PromoteWorkflowToolCallback(projectDeploymentFacade));

        toolCallbacks.add(
            new AttachTaskToolToolCallback(taskService, taskToolFacade, connectionService, aiHubToolAttachMetrics));
        toolCallbacks.add(
            new RemoveTaskToolToolCallback(taskService, taskToolFacade));
        toolCallbacks.add(new AskUserQuestionToolCallback(aiHubToolAttachMetrics));

        // Task / connection state visibility — mirrors the ASK agent. The two callbacks together let the LLM
        // resolve "is this already set up?" and "do I have a connection for this?" without escalating to the user.
        registerToolAttachStateVisibilityToolCallbacks(
            toolCallbacks, taskService, taskToolFacade, componentDefinitionService, connectionDefinitionService,
            workspaceConnectionFacade, actionDefinitionService, actionDefinitionFacade, triggerDefinitionService,
            triggerDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics, jsonMapper);

        apiCollectionFacadeProvider.ifAvailable(apiCollectionFacade -> {
            toolCallbacks.add(new CreateApiCollectionToolCallback(apiCollectionFacade));
            toolCallbacks.add(new CloneApiCollectionToolCallback(apiCollectionFacade));
            toolCallbacks.add(new ListApiCollectionsToolCallback(apiCollectionFacade));
        });

        // Resource discovery — read-only and always-on. Mirrors the same registrations on the ASK agent so
        // a "list my tasks" turn works identically regardless of which mode is active. Workflow-execution
        // lookups are delegated to the workflow_execution_agent specialist.
        toolCallbacks.add(new ListAiHubTasksToolCallback(taskService));
        mcpProjectFacadeProvider.ifAvailable(mcpProjectFacade -> {
            toolCallbacks.add(new CreateMcpProjectToolCallback(mcpProjectFacade));
            toolCallbacks.add(new CloneMcpProjectToolCallback(mcpProjectFacade));
        });

        workspaceMcpServerFacadeProvider.ifAvailable(
            workspaceMcpServerFacade -> toolCallbacks.add(new ListMcpServersToolCallback(workspaceMcpServerFacade)));

        // Auto-memory is now exposed via the forked AutoMemoryToolsAdvisor (DB-backed Resource seam),
        // registered as an advisor below rather than as standalone tool callbacks.

        toolCallbacks.add(new CloneAssetFileToolCallback(assetFileFacade));
        toolCallbacks.add(new CreateAssetFileToolCallback(assetFileFacade, aiHubTaskArtifactRecorder));
        toolCallbacks.add(new GetAssetFileContentToolCallback(assetFileFacade));
        toolCallbacks.add(new ListAssetFilesToolCallback(assetFileFacade));

        AiHubSpringAIAgent.Builder buildBuilder = AiHubSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptAiHubBuildResource))
            .toolCallbacks(toolCallbacks)
            .threadUserIdResolver(threadId -> taskService.findByThreadId(threadId)
                .map(AiHubTask::getUserId)
                .orElse(null))
            .state(state)
            // Mirrors aiHubAskSpringAIAgent — tenant + SecurityContext rehydration on tool execution so
            // @PreAuthorize-protected facade calls don't throw on Reactor scheduler threads.
            .securityContextRehydrator(securityContextRehydrator);

        // Session-backed conversation memory, INSIDE the tool-calling loop — mirrors aiHubAskSpringAIAgent.
        buildBuilder.advisor(aiHubSessionMemory.createSessionMemoryAdvisor());

        toolSearchToolCallAdvisorProvider.ifAvailable(buildBuilder::advisor);

        buildBuilder.advisor(
            AutoMemoryToolsAdvisor.builder()
                .autoMemoryTools(
                    new AutoMemoryTools(
                        new DbMemoryResourceResolver(aiHubMemoryService),
                        new DbAutoMemoryDirectoryOps(aiHubMemoryService)))
                .memorySystemPrompt(promptAiHubAutoMemoryToolsResource)
                .build());

        taskBindingToolCallbackResolverProvider.ifAvailable(buildBuilder::taskToolBindingResolver);
        overrideChatClientResolverProvider.ifAvailable(buildBuilder::overrideChatClientResolver);

        return buildBuilder.build();
    }

    /**
     * The AI Hub's session-based conversation memory over the application session backend
     * ({@code bytechef.ai.memory.provider}, jdbc by default). AutoCloseable — Spring disposes the owning Redis/S3
     * client on shutdown.
     */
    @Bean
    AiHubSessionMemory aiHubSessionMemory(
        Environment environment, @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate) {

        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(
            environment, jdbcTemplate);

        return new AiHubSessionMemory(
            builtInSessionRepository.sessionRepository(), builtInSessionRepository.closeable());
    }

    @Bean
    @ConditionalOnBean(WebhookWorkflowExecutor.class)
    WebhookBridgeAgent webhookBridgeAgent(
        WebhookWorkflowExecutor webhookFacade, AiHubTaskService taskService,
        WebhookResumeRegistry webhookResumeRegistry, JsonMapper jsonMapper, AssetFileFacade assetFileFacade,
        WorkflowChatMetrics workflowChatMetrics, WorkflowChatJobRegistry workflowChatJobRegistry,
        AiHubSessionMemory aiHubSessionMemory, WorkflowChatGuard workflowChatGuard,
        ObjectProvider<com.bytechef.atlas.execution.facade.JobFacade> jobFacadeProvider) throws AGUIException {

        return new WebhookBridgeAgent(
            webhookFacade, taskService, webhookResumeRegistry, jsonMapper, assetFileFacade,
            workflowChatMetrics, workflowChatJobRegistry, aiHubSessionMemory, workflowChatGuard,
            jobFacadeProvider.getIfAvailable());
    }

    @Bean
    AiHubRoutingAgent aiHubAskRoutingAgent(
        @Qualifier("aiHubAskSpringAIAgent") AiHubSpringAIAgent aiHubAskSpringAIAgent,
        ObjectProvider<WebhookBridgeAgent> webhookBridgeAgentProvider,
        AiHubTaskService taskService, AssetFileFacade assetFileFacade,
        ObjectProvider<AiHubPersonalAgentService> aiHubPersonalAgentServiceProvider)
        throws AGUIException {

        return new AiHubRoutingAgent(
            (Source.AI_HUB.name() + "_" + Mode.ASK.name()).toLowerCase(),
            aiHubAskSpringAIAgent,
            webhookBridgeAgentProvider.getIfAvailable(),
            taskService, assetFileFacade, aiHubPersonalAgentServiceProvider.getIfAvailable());
    }

    @Bean
    AiHubRoutingAgent aiHubBuildRoutingAgent(
        @Qualifier("aiHubBuildSpringAIAgent") AiHubSpringAIAgent aiHubBuildSpringAIAgent,
        ObjectProvider<WebhookBridgeAgent> webhookBridgeAgentProvider,
        AiHubTaskService taskService, AssetFileFacade assetFileFacade,
        ObjectProvider<AiHubPersonalAgentService> aiHubPersonalAgentServiceProvider)
        throws AGUIException {

        return new AiHubRoutingAgent(
            (Source.AI_HUB.name() + "_" + Mode.BUILD.name()).toLowerCase(),
            aiHubBuildSpringAIAgent,
            webhookBridgeAgentProvider.getIfAvailable(),
            taskService, assetFileFacade, aiHubPersonalAgentServiceProvider.getIfAvailable());
    }

    @Bean
    AiHubGlobalToolCatalog aiHubAskGlobalToolCatalog(
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, TaskDispatcherTools taskDispatcherTools) {

        return globalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, readProjectTools, readProjectWorkflowTools, componentTools,
            taskTools, taskDispatcherTools);
    }

    @Bean
    AiHubGlobalToolCatalog aiHubBuildGlobalToolCatalog(
        ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, ComponentTools componentTools,
        TaskTools taskTools, TaskDispatcherTools taskDispatcherTools, ScriptTools scriptTools,
        ClusterElementTools clusterElementTools) {

        return globalToolCatalog(
            ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID, projectTools, projectWorkflowTools, componentTools,
            taskTools, taskDispatcherTools, scriptTools, clusterElementTools);
    }

    private static AiHubGlobalToolCatalog globalToolCatalog(String sessionId, Object... toolObjects) {
        return new AiHubGlobalToolCatalog(sessionId, List.of(ToolCallbacks.from(toolObjects)));
    }

    /**
     * Registers the optional ChatClient-based sub-agent tool callbacks (research, data analyst, image generator, slide
     * builder) on the supplied tool list. Each is only added when its backing ChatClient bean is present. Extracted to
     * keep the BUILD-agent bean method within Checkstyle's per-method line limit.
     *
     * <p>
     * The older {@code workflow_builder} ChatClient sub-agent is intentionally absent — it has been superseded by the
     * Copilot {@code workflow_editor_agent} specialist registered through
     * {@link #registerCopilotSubAgentToolCallbacks}. The Copilot specialist persists workflows internally via
     * {@code ProjectWorkflowTools}, eliminating the JSON round-trip {@code workflow_builder} required.
     * </p>
     */
    private static void registerSubAgentToolCallbacks(
        List<ToolCallback> toolCallbacks, ObjectProvider<ChatClient> researchChatClientProvider,
        ObjectProvider<ChatClient> dataAnalystChatClientProvider,
        ObjectProvider<ChatClient> imageGeneratorChatClientProvider,
        ObjectProvider<ChatClient> slideBuilderChatClientProvider, AssetFileFacade assetFileFacade) {

        researchChatClientProvider.ifAvailable(
            researchChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    ResearchConfiguration.createResearchToolCallback(researchChatClient), "research")));

        dataAnalystChatClientProvider.ifAvailable(
            dataAnalystChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    DataAnalystConfiguration.createDataAnalystToolCallback(
                        dataAnalystChatClient, assetFileFacade),
                    "data_analyst")));

        imageGeneratorChatClientProvider.ifAvailable(
            imageGeneratorChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    ImageGeneratorConfiguration.createImageGeneratorToolCallback(imageGeneratorChatClient),
                    "image_generator")));

        slideBuilderChatClientProvider.ifAvailable(
            slideBuilderChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    SlideBuilderConfiguration.createSlideBuilderToolCallback(slideBuilderChatClient),
                    "slide_builder")));
    }

    /**
     * Registers the Copilot specialist sub-agent ToolCallbacks (skills, context store, knowledge base, data table,
     * cluster element, code editor, workflow editor, converter) on the supplied tool list. Each is only added when
     * its backing ChatClient bean is present — Copilot
     * disabled or a particular specialist missing skips silently. Mirrors {@link #registerSubAgentToolCallbacks} for
     * the older ChatClient sub-agents (research / data_analyst / image_generator / slide_builder).
     *
     * <p>
     * The {@code converterProvider} is nullable because the ASK agent has no Converter specialist (Copilot only ships a
     * BUILD-mode Converter agent); callers from the ASK bean pass {@code null} and the converter registration is
     * skipped.
     * </p>
     */
    private static void registerCopilotSubAgentToolCallbacks(
        List<ToolCallback> toolCallbacks,
        ObjectProvider<ChatClient> skillsSubAgentChatClientProvider,
        ObjectProvider<ChatClient> contextStoreSubAgentChatClientProvider,
        ObjectProvider<ChatClient> knowledgeBaseSubAgentChatClientProvider,
        ObjectProvider<ChatClient> dataTableSubAgentChatClientProvider,
        ObjectProvider<ChatClient> clusterElementSubAgentChatClientProvider,
        ObjectProvider<ChatClient> codeEditorSubAgentChatClientProvider,
        ObjectProvider<ChatClient> workflowEditorSubAgentChatClientProvider,
        @Nullable ObjectProvider<Supplier<ChatClient>> converterSubAgentChatClientSupplierProvider,
        ObjectProvider<ChatClient> workflowExecutionSubAgentChatClientProvider,
        ObjectProvider<ChatClient> customComponentSubAgentChatClientProvider,
        ObjectProvider<ChatClient> codeWorkflowSubAgentChatClientProvider) {

        skillsSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(new SkillsAgentToolCallback(chatClient), "skills_agent")));

        contextStoreSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new ContextStoreAgentToolCallback(chatClient), "context_store_agent")));

        knowledgeBaseSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new KnowledgeBaseAgentToolCallback(chatClient), "knowledge_base_agent")));

        dataTableSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new DataTableAgentToolCallback(chatClient), "data_table_agent")));

        clusterElementSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new ClusterElementAgentToolCallback(chatClient), "cluster_element_agent")));

        codeEditorSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new CodeEditorAgentToolCallback(chatClient), "code_editor_agent")));

        workflowEditorSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new WorkflowEditorAgentToolCallback(chatClient), "workflow_editor_agent")));

        workflowExecutionSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new WorkflowExecutionAgentToolCallback(chatClient), "workflow_execution_agent")));

        if (converterSubAgentChatClientSupplierProvider != null) {
            converterSubAgentChatClientSupplierProvider.ifAvailable(
                converterChatClientSupplier -> toolCallbacks.add(
                    new ProgressReportingToolCallback(
                        new ConverterAgentToolCallback(converterChatClientSupplier), "converter_agent")));
        }

        customComponentSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new CustomComponentAgentToolCallback(chatClient), "custom_component_agent")));

        codeWorkflowSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new CodeWorkflowAgentToolCallback(chatClient), "code_workflow_agent")));
    }

    /**
     * Registers state-visibility callbacks for the autonomous tool-attach flow: {@code listTaskTools} and
     * {@code listConnectionsForComponent}. Read-only on both agents — the LLM uses them to avoid duplicate attaches and
     * to surface an existing connection before falling back to {@code createConnection}.
     */
    private static void registerToolAttachStateVisibilityToolCallbacks(
        List<ToolCallback> toolCallbacks, AiHubTaskService taskService, AiHubTaskToolFacade taskToolFacade,
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService, WorkspaceConnectionFacade workspaceConnectionFacade,
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        PropertyOptionsResolver propertyOptionsResolver, AiHubToolAttachMetrics aiHubToolAttachMetrics,
        JsonMapper jsonMapper) {

        toolCallbacks.add(new ListTaskToolsToolCallback(taskService, taskToolFacade, aiHubToolAttachMetrics,
            jsonMapper));
        toolCallbacks.add(
            new ListConnectionsForComponentToolCallback(
                componentDefinitionService, connectionDefinitionService, aiHubToolAttachMetrics,
                List.of(new WorkspaceCopilotConnectionLister(workspaceConnectionFacade, propertyOptionsResolver))));
        toolCallbacks.add(
            new LookupActionPropertyOptionsToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics));
        toolCallbacks.add(
            new LookupTriggerPropertyOptionsToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics));
        toolCallbacks.add(
            new SelectPropertyOptionToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics));
        toolCallbacks.add(
            new SelectTriggerPropertyOptionToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics));
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
