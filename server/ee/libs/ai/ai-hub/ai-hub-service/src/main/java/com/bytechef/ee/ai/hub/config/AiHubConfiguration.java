/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.tool.AiAgentAgentToolCallback;
import com.bytechef.ai.copilot.tool.AskUserQuestionToolCallback;
import com.bytechef.ai.copilot.tool.AssetFileAgentToolCallback;
import com.bytechef.ai.copilot.tool.ClusterElementAgentToolCallback;
import com.bytechef.ai.copilot.tool.CodeEditorAgentToolCallback;
import com.bytechef.ai.copilot.tool.ConverterAgentToolCallback;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.CreateConnectionToolCallback;
import com.bytechef.ai.copilot.tool.DataTableAgentToolCallback;
import com.bytechef.ai.copilot.tool.KnowledgeBaseAgentToolCallback;
import com.bytechef.ai.copilot.tool.ListConnectionsForComponentToolCallback;
import com.bytechef.ai.copilot.tool.LookupComponentPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.SelectComponentPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.SelectConnectionToolCallback;
import com.bytechef.ai.copilot.tool.SkillsAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkflowEditorAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkflowExecutionAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkspaceCopilotConnectionLister;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.DeploymentManagerConfiguration;
import com.bytechef.automation.ai.tool.GetAssetFileContentToolCallback;
import com.bytechef.automation.ai.tool.ListAssetFilesToolCallback;
import com.bytechef.automation.ai.tool.ManagerAgentType;
import com.bytechef.automation.ai.tool.McpManagerConfiguration;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
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
import com.bytechef.ee.ai.hub.guardrails.SubAgentGuardrailedChatClient;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.progress.ProgressReportingToolCallback;
import com.bytechef.ee.ai.hub.subagent.SubAgentAdvisorContributor;
import com.bytechef.ee.ai.hub.subagent.SubAgentAskToolContributor;
import com.bytechef.ee.ai.hub.subagent.SubAgentSessionMemoryContributor;
import com.bytechef.ee.ai.hub.subagent.SubagentAskChannelRelay;
import com.bytechef.ee.ai.hub.subagent.WorkspaceAdvisorContributor;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactService;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import com.bytechef.ee.ai.hub.tool.AiHubTaskArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AttachTaskToolToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateWorkflowChatToolCallback;
import com.bytechef.ee.ai.hub.tool.ListAiHubPersonalAgentsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListAiHubTasksToolCallback;
import com.bytechef.ee.ai.hub.tool.ListChatWorkflowsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListTaskToolsToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenAiHubPersonalAgentTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenResourceTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowChatTabToolCallback;
import com.bytechef.ee.ai.hub.tool.RemoveTaskToolToolCallback;
import com.bytechef.ee.ai.hub.tool.RunChatWorkflowToolCallback;
import com.bytechef.ee.ai.hub.tool.memory.DbAutoMemoryDirectoryOps;
import com.bytechef.ee.ai.hub.tool.memory.DbMemoryResourceResolver;
import com.bytechef.ee.ai.hub.toolsearch.AiHubGlobalToolCatalog;
import com.bytechef.ee.ai.hub.toolsearch.AiHubTaskBindingToolCallbackResolver;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.util.Source;
import com.bytechef.ee.automation.ai.copilot.tool.CodeWorkflowAgentToolCallback;
import com.bytechef.ee.automation.ai.copilot.tool.ContextStoreAgentToolCallback;
import com.bytechef.ee.automation.ai.copilot.tool.CustomComponentAgentToolCallback;
import com.bytechef.ee.automation.ai.tool.ApiCollectionManagerConfiguration;
import com.bytechef.ee.automation.ai.tool.ListApiCollectionsToolCallback;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
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
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    static final String RESEARCH_SECTION_START_MARKER = "[[research:start]]";
    static final String RESEARCH_SECTION_END_MARKER = "[[research:end]]";

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
        @Qualifier("aiAgentAskSubAgentChatClient") //
        ObjectProvider<ChatClient> aiAgentAskSubAgentChatClientProvider,
        @Qualifier("assetFileAskSubAgentChatClient") //
        ObjectProvider<ChatClient> assetFileAskSubAgentChatClientProvider,
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
        AiHubTaskService taskService,
        AiAutoMemoryService aiHubMemoryService,
        AssetFileFacade assetFileFacade,
        AiHubTaskToolFacade taskToolFacade,
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService,
        WorkspaceConnectionFacade workspaceConnectionFacade,
        ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade,
        SecurityContextRehydrator securityContextRehydrator,
        PropertyOptionsResolver propertyOptionsResolver,
        ObjectProvider<AiHubPersonalAgentService> aiHubPersonalAgentServiceProvider,
        @Qualifier("aiHubAskToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        ObjectProvider<AiHubTaskBindingToolCallbackResolver> taskBindingToolCallbackResolverProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<LlmUsageRecorder> llmUsageRecorderProvider,
        ObjectProvider<AiGuardrails> aiGuardrailsProvider, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<WorkspaceSystemPrompts> workspaceSystemPromptsProvider,
        AiHubToolAttachMetrics aiHubToolAttachMetrics, JsonMapper jsonMapper)
        throws AGUIException {

        String name = Source.AI_HUB.name() + "_" + Mode.ASK.name();

        List<ToolCallback> toolCallbacks = new ArrayList<>(toolCallbackProvider.orderedStream()
            .toList());

        // Resolved once and reused for BOTH the top-level agent's own advisor (attached below via
        // builder.aiGuardrails(...)) and every subagent delegate's ChatClient (wrapped via
        // SubAgentGuardrailedChatClient.wrap in the registration helpers below) — see SubAgentGuardrailedChatClient's
        // javadoc for why delegate calls need their own, per-call workspace resolution instead of a builder-time
        // advisor. Absent AiGuardrails bean (EE guardrails module not on the classpath) → every wrap call below is a
        // no-op, unchanged behaviour.
        AiGuardrails aiGuardrails = aiGuardrailsProvider.getIfAvailable();
        AiGuardrailMetrics aiGuardrailMetrics = aiGuardrails == null
            ? null
            : new AiGuardrailMetrics(meterRegistryProvider.getIfAvailable(), "ai_hub");

        // Absent (EE workspace-prompt module not on the classpath) → attachWorkspaceSystemPromptAdvisor below is a
        // no-op, unchanged behaviour.
        WorkspaceSystemPrompts workspaceSystemPrompts = workspaceSystemPromptsProvider.getIfAvailable();

        // Resolved to a flag (not ifAvailable) because availability also decides whether the system prompt keeps
        // its research section — documenting an unregistered tool makes the model call it and fail the turn with
        // "No ToolCallback found for tool name: research".
        ChatClient researchChatClient = researchChatClientProvider.getIfAvailable();

        if (researchChatClient != null) {
            toolCallbacks.add(
                new ProgressReportingToolCallback(
                    ResearchConfiguration.createResearchToolCallback(
                        wrapDelegate(
                            researchChatClient, AiHubAgentType.RESEARCH.key(), aiGuardrails, aiGuardrailMetrics,
                            workspaceSystemPrompts, aiHubSessionMemory)),
                    "research"));
        }

        // Consolidated open-tab tool (type-keyed) replaces the seven per-resource variants on the pinned
        // list. ASK mode is read-only (never builds workflows), so no server-side artifact recorder is
        // wired — the client still records the reference when the tab opens.
        toolCallbacks.add(new OpenResourceTabToolCallback(null));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback());
        // Read-only asset-file access. The ASK prompt documents listAssetFiles/getAssetFileContent, and without
        // these registrations the model's direct calls fail with "No ToolCallback found". Creation stays
        // BUILD-only. Row-level data-table reads are delegated to the data_table_agent specialist instead of a
        // flat queryDataTable — the specialist already owns the read tool set.
        toolCallbacks.add(new GetAssetFileContentToolCallback(assetFileFacade));
        toolCallbacks.add(new ListAssetFilesToolCallback(assetFileFacade));
        // attachTaskTool/removeTaskTool are deliberately NOT registered here: the ASK prompt declares tool
        // attachment a BUILD-only mutation ("suggest switching to BUILD mode"), so the registrations were
        // dead weight the prompt forbade the model from using.
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

        // listApiCollections is demoted to the searchable catalog (aiHubAskGlobalToolCatalog) — rare enough
        // that it should not ride in every model call.

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
            aiAgentAskSubAgentChatClientProvider, assetFileAskSubAgentChatClientProvider, false,
            clusterElementAskSubAgentChatClientProvider,
            codeEditorAskSubAgentChatClientProvider, workflowEditorAskSubAgentChatClientProvider, null,
            workflowExecutionAskSubAgentChatClientProvider, customComponentAskSubAgentChatClientProvider,
            codeWorkflowAskSubAgentChatClientProvider, aiGuardrails, aiGuardrailMetrics, workspaceSystemPrompts,
            aiHubSessionMemory);

        AiHubSpringAIAgent.Builder builder = AiHubSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptAiHubAskResource, researchChatClient != null))
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

        // Per-turn token metering into ai_llm_usage (source = AI_HUB). Absent recorder → advisor logs only.
        llmUsageRecorderProvider.ifAvailable(builder::llmUsageRecorder);

        // Workspace content guardrails on every LLM turn this agent resolves a ChatClient for — see
        // AiHubSpringAIAgent#attachGuardrailsAdvisor. Absent AiGuardrails bean (EE guardrails module not on the
        // classpath) → no-op, unchanged behaviour. Reuses the same (aiGuardrails, aiGuardrailMetrics) pair the
        // subagent delegate registrations above were wrapped with, so the top-level agent and every delegate share
        // one "ai_hub"-tagged AiGuardrailMetrics instance for this bean.
        attachAiGuardrails(builder, aiGuardrails, aiGuardrailMetrics);

        // Appends the workspace admin's standing instructions to every LLM turn — see
        // AiHubSpringAIAgent#attachWorkspaceSystemPromptAdvisor. Absent WorkspaceSystemPrompts bean (EE
        // workspace-prompt module not on the classpath) → no-op, unchanged behaviour.
        builder.workspaceSystemPrompts(workspaceSystemPrompts);

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
        @Qualifier("aiAgentBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> aiAgentBuildSubAgentChatClientProvider,
        @Qualifier("assetFileBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> assetFileBuildSubAgentChatClientProvider,
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
        AssetFileFacade assetFileFacade, AiHubTaskArtifactService taskArtifactService,
        AiHubTaskArtifactRecorder aiHubTaskArtifactRecorder,
        AiHubTaskService taskService, AiAutoMemoryService aiHubMemoryService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService,
        TriggerDefinitionService triggerDefinitionService,
        WorkflowFacade workflowFacade, WorkflowService workflowService,
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
        @Qualifier("mcpManagerChatClient") ObjectProvider<ChatClient> mcpManagerChatClientProvider,
        @Qualifier("personalAgentManagerChatClient") //
        ObjectProvider<ChatClient> personalAgentManagerChatClientProvider,
        @Qualifier("deploymentManagerChatClient") ObjectProvider<ChatClient> deploymentManagerChatClientProvider,
        @Qualifier("apiCollectionManagerChatClient") //
        ObjectProvider<ChatClient> apiCollectionManagerChatClientProvider,
        @Qualifier("aiHubBuildToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        ObjectProvider<AiHubTaskBindingToolCallbackResolver> taskBindingToolCallbackResolverProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<LlmUsageRecorder> llmUsageRecorderProvider,
        ObjectProvider<AiGuardrails> aiGuardrailsProvider, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<WorkspaceSystemPrompts> workspaceSystemPromptsProvider,
        AiHubToolAttachMetrics aiHubToolAttachMetrics, JsonMapper jsonMapper)
        throws AGUIException {

        String name = Source.AI_HUB.name() + "_" + Mode.BUILD.name();

        List<ToolCallback> toolCallbacks = new ArrayList<>(toolCallbackProvider.orderedStream()
            .toList());

        // Resolved once and reused for BOTH the top-level agent's own advisor (attached below via
        // attachAiGuardrails) and every subagent delegate's ChatClient (wrapped via
        // SubAgentGuardrailedChatClient.wrap in the registration helpers below) — mirrors aiHubAskSpringAIAgent.
        AiGuardrails aiGuardrails = aiGuardrailsProvider.getIfAvailable();
        AiGuardrailMetrics aiGuardrailMetrics = aiGuardrails == null
            ? null
            : new AiGuardrailMetrics(meterRegistryProvider.getIfAvailable(), "ai_hub");

        // Absent (EE workspace-prompt module not on the classpath) → attachWorkspaceSystemPromptAdvisor below is a
        // no-op, unchanged behaviour.
        WorkspaceSystemPrompts workspaceSystemPrompts = workspaceSystemPromptsProvider.getIfAvailable();

        registerSubAgentToolCallbacks(
            toolCallbacks, researchChatClientProvider, dataAnalystChatClientProvider,
            imageGeneratorChatClientProvider, slideBuilderChatClientProvider, assetFileFacade, aiGuardrails,
            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory);

        // Mirrors aiHubAskSpringAIAgent: the research tool is conditionally registered (Firecrawl-gated), so the
        // system prompt's research section must be dropped when it is absent — otherwise the model calls the
        // documented-but-unregistered tool and the turn dies with "No ToolCallback found".
        boolean researchToolAvailable = researchChatClientProvider.getIfAvailable() != null;

        registerManagerSubAgentToolCallbacks(
            toolCallbacks, mcpManagerChatClientProvider, personalAgentManagerChatClientProvider,
            deploymentManagerChatClientProvider, apiCollectionManagerChatClientProvider, aiGuardrails,
            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory);

        // Consolidated open-tab tool (type-keyed) replaces the seven per-resource variants on the pinned list.
        toolCallbacks.add(new OpenResourceTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback());
        // Row-level data-table reads are delegated to the data_analyst / data_table_agent specialists instead of
        // a flat queryDataTable — both specialists already own the read tool set.
        toolCallbacks.add(
            new ListChatWorkflowsToolCallback(
                projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService,
                triggerDefinitionService, workflowFacade, workflowService));
        toolCallbacks.add(
            new RunChatWorkflowToolCallback(
                projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService,
                workflowFacade, workflowService, taskArtifactService));
        // createWorkflowChat is demoted to the searchable catalog (aiHubBuildGlobalToolCatalog) — rare enough
        // that it should not ride in every model call.

        // Personal-agent CRUD is delegated to the personal_agent_manager specialist (see
        // registerManagerSubAgentToolCallbacks); the ASK agent keeps its own read-only flat registrations.

        // Copilot specialist sub-agent delegation. Write-capable variants for the BUILD agent, plus
        // the BUILD-only Converter sub-agent. Skips registrations when the corresponding ChatClient
        // bean is absent (Copilot disabled).
        registerCopilotSubAgentToolCallbacks(
            toolCallbacks, skillsBuildSubAgentChatClientProvider, contextStoreBuildSubAgentChatClientProvider,
            knowledgeBaseBuildSubAgentChatClientProvider, dataTableBuildSubAgentChatClientProvider,
            aiAgentBuildSubAgentChatClientProvider, assetFileBuildSubAgentChatClientProvider, true,
            clusterElementBuildSubAgentChatClientProvider,
            codeEditorBuildSubAgentChatClientProvider, workflowEditorBuildSubAgentChatClientProvider,
            converterBuildSubAgentChatClientSupplierProvider, workflowExecutionBuildSubAgentChatClientProvider,
            customComponentBuildSubAgentChatClientProvider, codeWorkflowBuildSubAgentChatClientProvider, aiGuardrails,
            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory);
        toolCallbacks.add(new CreateConnectionToolCallback(componentDefinitionService));
        toolCallbacks.add(new SelectConnectionToolCallback(componentDefinitionService));

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

        // API-collection and MCP-server work is delegated to the api_collection_manager and mcp_manager
        // specialists (see registerManagerSubAgentToolCallbacks); the ASK agent keeps its read-only
        // listApiCollections flat registration.

        // Resource discovery — read-only and always-on. Mirrors the same registrations on the ASK agent so
        // a "list my tasks" turn works identically regardless of which mode is active. Workflow-execution
        // lookups are delegated to the workflow_execution_agent specialist.
        toolCallbacks.add(new ListAiHubTasksToolCallback(taskService));

        // Auto-memory is now exposed via the forked AutoMemoryToolsAdvisor (DB-backed Resource seam),
        // registered as an advisor below rather than as standalone tool callbacks.

        // createAssetFile, updateAssetFileContent, and cloneAssetFile are delegated to the asset_file_agent
        // specialist (see registerCopilotSubAgentToolCallbacks) instead of flat registrations here — the
        // specialist now owns file-writing end-to-end. The two reads stay pinned: the BUILD prompt documents
        // them directly, and demoting them would break those turns.
        toolCallbacks.add(new GetAssetFileContentToolCallback(assetFileFacade));
        toolCallbacks.add(new ListAssetFilesToolCallback(assetFileFacade));

        AiHubSpringAIAgent.Builder buildBuilder = AiHubSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptAiHubBuildResource, researchToolAvailable))
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

        // Per-turn token metering into ai_llm_usage (source = AI_HUB). Absent recorder → advisor logs only.
        llmUsageRecorderProvider.ifAvailable(buildBuilder::llmUsageRecorder);

        // Workspace content guardrails on every LLM turn this agent resolves a ChatClient for — mirrors
        // aiHubAskSpringAIAgent, including personal-agent model-override turns (see
        // AiHubSpringAIAgent#attachGuardrailsAdvisor). Reuses the same (aiGuardrails, aiGuardrailMetrics) pair the
        // subagent delegate registrations above were wrapped with.
        attachAiGuardrails(buildBuilder, aiGuardrails, aiGuardrailMetrics);

        // Appends the workspace admin's standing instructions to every LLM turn — see
        // AiHubSpringAIAgent#attachWorkspaceSystemPromptAdvisor. Absent WorkspaceSystemPrompts bean (EE
        // workspace-prompt module not on the classpath) → no-op, unchanged behaviour.
        buildBuilder.workspaceSystemPrompts(workspaceSystemPrompts);

        return buildBuilder.build();
    }

    /**
     * Shared wiring for both the ASK and BUILD agents: when {@code aiGuardrails} is non-null, attaches it (together
     * with {@code aiGuardrailMetrics}, fixed to the {@code ai_hub} surface — deliberately not the shared
     * engine-internal metrics bean, which is conditional on the AI Gateway being enabled and tagged with a single
     * deployment-wide surface property, see {@link AiGuardrailMetrics}'s own javadoc) to {@code builder}. Absent
     * {@code aiGuardrails} (EE guardrails module not on the classpath) is a no-op — both agents fall back to their
     * pre-existing, unguarded behaviour. Callers resolve the pair once per bean method and pass the SAME instances here
     * and into the subagent registration helpers ({@link #registerCopilotSubAgentToolCallbacks},
     * {@link #registerSubAgentToolCallbacks}, {@link #registerManagerSubAgentToolCallbacks}) so the top-level agent and
     * every delegate share one {@code ai_hub}-tagged {@link AiGuardrailMetrics} instance.
     */
    private static void attachAiGuardrails(
        AiHubSpringAIAgent.Builder builder, @Nullable AiGuardrails aiGuardrails,
        @Nullable AiGuardrailMetrics aiGuardrailMetrics) {

        if (aiGuardrails != null) {
            builder.aiGuardrails(aiGuardrails, aiGuardrailMetrics);
        }
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
        ComponentTools componentTools, TaskTools taskTools, TaskDispatcherTools taskDispatcherTools,
        ObjectProvider<ApiCollectionFacade> apiCollectionFacadeProvider) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        Collections.addAll(
            toolCallbacks,
            ToolCallbacks.from(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, taskDispatcherTools));

        // Demoted from the pinned list: rarely-used reads stay reachable through searchTool without paying
        // per-turn schema cost on every model call.
        apiCollectionFacadeProvider.ifAvailable(
            apiCollectionFacade -> toolCallbacks.add(new ListApiCollectionsToolCallback(apiCollectionFacade)));

        return new AiHubGlobalToolCatalog(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, toolCallbacks);
    }

    @Bean
    AiHubGlobalToolCatalog aiHubBuildGlobalToolCatalog(
        ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, ComponentTools componentTools,
        TaskTools taskTools, TaskDispatcherTools taskDispatcherTools, ScriptTools scriptTools,
        ClusterElementTools clusterElementTools, AiHubTaskService taskService) {

        // cloneAssetFile is reachable through the asset_file_agent delegate (see
        // registerCopilotSubAgentToolCallbacks) rather than the searchable catalog — file-writing work is now
        // owned end-to-end by the specialist subagent.
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        Collections.addAll(
            toolCallbacks,
            ToolCallbacks.from(
                projectTools, projectWorkflowTools, componentTools, taskTools, taskDispatcherTools, scriptTools,
                clusterElementTools));

        toolCallbacks.add(new CreateWorkflowChatToolCallback(taskService));

        return new AiHubGlobalToolCatalog(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID, toolCallbacks);
    }

    /**
     * The specialists allowed to pose a question to the user. Restricted to the manager specialists: each owns a prompt
     * that is not shared with a Copilot panel agent, so the tool can be documented where it is registered.
     *
     * <p>
     * The Copilot domain specialists are deliberately absent. Their prompt file is shared by the subagent client and
     * the panel agent (see the domain copilot slice pattern), and the panel agent has no ask tool registered — so
     * documenting the tool in that shared prompt would make the panel agent call a tool that does not exist there and
     * kill the turn with "No ToolCallback found". Wiring them needs a prompt split first, not more wiring.
     * </p>
     *
     * <p>
     * The generative one-shots (research, data_analyst, image_generator, slide_builder, converter) are absent by intent
     * rather than by blocker: they are asked to produce something, and a clarifying round trip costs more than it
     * saves.
     * </p>
     */
    private static final Set<String> ASK_CAPABLE_AGENT_TYPE_KEYS = Set.of(
        AiHubAgentType.PERSONAL_AGENT_MANAGER.key(), ManagerAgentType.MCP_MANAGER.key(),
        ManagerAgentType.DEPLOYMENT_MANAGER.key(), ManagerAgentType.API_COLLECTION_MANAGER.key());

    /**
     * Wraps one delegate's {@code ChatClient} in everything a specialist call needs per request: the calling
     * workspace's guardrails and system prompt, that specialist's own per-conversation session memory, and — for the
     * specialists in {@link #ASK_CAPABLE_AGENT_TYPE_KEYS} — the tool that lets it pose a question to the user.
     *
     * <p>
     * Every delegate registration goes through here rather than calling
     * {@link SubAgentGuardrailedChatClient#wrap(ChatClient, List)} directly, so no site can quietly forget the memory
     * contributor and leave one specialist amnesiac while the rest remember.
     * </p>
     *
     * <p>
     * {@code agentTypeKey} MUST be a key registered with {@code AgentTypeRegistry}: it becomes the session-id suffix,
     * and the purge that runs when an AI Hub task is deleted reconstructs the keys to delete from that registry. A key
     * that is not registered would produce a session nothing ever deletes.
     * </p>
     */
    private static ChatClient wrapDelegate(
        ChatClient chatClient, String agentTypeKey, @Nullable AiGuardrails aiGuardrails,
        @Nullable AiGuardrailMetrics aiGuardrailMetrics, @Nullable WorkspaceSystemPrompts workspaceSystemPrompts,
        @Nullable AiHubSessionMemory aiHubSessionMemory) {

        List<SubAgentAdvisorContributor> contributors = new ArrayList<>();

        boolean guardrailsPresent = aiGuardrails != null && aiGuardrailMetrics != null;

        if (guardrailsPresent || workspaceSystemPrompts != null) {
            contributors.add(
                new WorkspaceAdvisorContributor(
                    guardrailsPresent ? aiGuardrails : null, guardrailsPresent ? aiGuardrailMetrics : null,
                    workspaceSystemPrompts));
        }

        if (aiHubSessionMemory != null) {
            contributors.add(new SubAgentSessionMemoryContributor(aiHubSessionMemory, agentTypeKey));
        }

        if (ASK_CAPABLE_AGENT_TYPE_KEYS.contains(agentTypeKey)) {
            contributors.add(new SubAgentAskToolContributor());
        }

        return SubAgentGuardrailedChatClient.wrap(chatClient, contributors);
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
     *
     * <p>
     * Each delegate's {@code ChatClient} is wrapped with {@link #wrapDelegate} before being handed to its
     * {@code createXToolCallback} factory, so the delegate's own one-shot LLM call runs under the workspace's
     * {@code AiGuardrailsAdvisor} and {@code WorkspaceSystemPromptAdvisor} and its own session memory — see
     * {@code SubAgentGuardrailedChatClient}'s javadoc for why this seam covers every delegate without touching the
     * individual {@code *ToolCallback}/{@code *Configuration} classes. {@code aiGuardrails}/{@code aiGuardrailMetrics}
     * are {@code null} when the EE guardrails module isn't wired, and {@code workspaceSystemPrompts} is {@code null}
     * when the EE workspace-prompt module isn't wired, in which case the corresponding wrapping is a no-op.
     * </p>
     */
    private static void registerSubAgentToolCallbacks(
        List<ToolCallback> toolCallbacks, ObjectProvider<ChatClient> researchChatClientProvider,
        ObjectProvider<ChatClient> dataAnalystChatClientProvider,
        ObjectProvider<ChatClient> imageGeneratorChatClientProvider,
        ObjectProvider<ChatClient> slideBuilderChatClientProvider, AssetFileFacade assetFileFacade,
        @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics,
        @Nullable WorkspaceSystemPrompts workspaceSystemPrompts, @Nullable AiHubSessionMemory aiHubSessionMemory) {

        researchChatClientProvider.ifAvailable(
            researchChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    ResearchConfiguration.createResearchToolCallback(
                        wrapDelegate(
                            researchChatClient, AiHubAgentType.RESEARCH.key(), aiGuardrails, aiGuardrailMetrics,
                            workspaceSystemPrompts, aiHubSessionMemory)),
                    "research")));

        dataAnalystChatClientProvider.ifAvailable(
            dataAnalystChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    DataAnalystConfiguration.createDataAnalystToolCallback(
                        wrapDelegate(
                            dataAnalystChatClient, AiHubAgentType.DATA_ANALYST.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory),
                        assetFileFacade),
                    "data_analyst")));

        imageGeneratorChatClientProvider.ifAvailable(
            imageGeneratorChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    ImageGeneratorConfiguration.createImageGeneratorToolCallback(
                        wrapDelegate(
                            imageGeneratorChatClient, AiHubAgentType.IMAGE_GENERATOR.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "image_generator")));

        slideBuilderChatClientProvider.ifAvailable(
            slideBuilderChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    SlideBuilderConfiguration.createSlideBuilderToolCallback(
                        wrapDelegate(
                            slideBuilderChatClient, AiHubAgentType.SLIDE_BUILDER.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "slide_builder")));
    }

    /**
     * Registers the AI-hub-owned "manager" specialist sub-agent ToolCallbacks (MCP servers, personal agents, project
     * deployments, API collections) on the supplied tool list. Each is only added when its backing ChatClient bean is
     * present — a missing facade (feature module not on the classpath) means the specialist's ChatClient bean was not
     * created and the registration is silently skipped. Mirrors {@link #registerSubAgentToolCallbacks}, including the
     * {@link SubAgentGuardrailedChatClient#wrap} guardrail/workspace-prompt wrapping. This wiring covers only the AI
     * Hub chat surface — the separate MCP-surface manager contributions
     * ({@code AiHubManagerMcpContributorConfiguration}, {@code ManagerMcpContributorConfiguration},
     * {@code ApiCollectionManagerMcpContributorConfiguration}) construct their own {@code ManagerSubAgentToolCallback}
     * instances directly from the same underlying {@code ChatClient} beans and are NOT wrapped here — left out of
     * scope, see the AI Guardrails spec's decisions log.
     */
    private static void registerManagerSubAgentToolCallbacks(
        List<ToolCallback> toolCallbacks, ObjectProvider<ChatClient> mcpManagerChatClientProvider,
        ObjectProvider<ChatClient> personalAgentManagerChatClientProvider,
        ObjectProvider<ChatClient> deploymentManagerChatClientProvider,
        ObjectProvider<ChatClient> apiCollectionManagerChatClientProvider, @Nullable AiGuardrails aiGuardrails,
        @Nullable AiGuardrailMetrics aiGuardrailMetrics, @Nullable WorkspaceSystemPrompts workspaceSystemPrompts,
        @Nullable AiHubSessionMemory aiHubSessionMemory) {

        mcpManagerChatClientProvider.ifAvailable(
            mcpManagerChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    McpManagerConfiguration.createMcpManagerToolCallback(
                        wrapDelegate(
                            mcpManagerChatClient, ManagerAgentType.MCP_MANAGER.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory),
                        new SubagentAskChannelRelay()),
                    "mcp_manager")));

        personalAgentManagerChatClientProvider.ifAvailable(
            personalAgentManagerChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    PersonalAgentManagerConfiguration.createPersonalAgentManagerToolCallback(
                        wrapDelegate(
                            personalAgentManagerChatClient, AiHubAgentType.PERSONAL_AGENT_MANAGER.key(),
                            aiGuardrails, aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory),
                        new SubagentAskChannelRelay()),
                    "personal_agent_manager")));

        deploymentManagerChatClientProvider.ifAvailable(
            deploymentManagerChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    DeploymentManagerConfiguration.createDeploymentManagerToolCallback(
                        wrapDelegate(
                            deploymentManagerChatClient, ManagerAgentType.DEPLOYMENT_MANAGER.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory),
                        new SubagentAskChannelRelay()),
                    "deployment_manager")));

        apiCollectionManagerChatClientProvider.ifAvailable(
            apiCollectionManagerChatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    ApiCollectionManagerConfiguration.createApiCollectionManagerToolCallback(
                        wrapDelegate(
                            apiCollectionManagerChatClient, ManagerAgentType.API_COLLECTION_MANAGER.key(),
                            aiGuardrails, aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory),
                        new SubagentAskChannelRelay()),
                    "api_collection_manager")));
    }

    /**
     * Registers the Copilot specialist sub-agent ToolCallbacks (skills, context store, knowledge base, data table, ai
     * agent builder, asset file, cluster element, code editor, workflow editor, converter) on the supplied tool list.
     * Each is only added when its backing ChatClient bean is present — Copilot disabled or a particular specialist
     * missing skips silently. Mirrors {@link #registerSubAgentToolCallbacks} for the older ChatClient sub-agents
     * (research / data_analyst / image_generator / slide_builder).
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
        ObjectProvider<ChatClient> aiAgentSubAgentChatClientProvider,
        ObjectProvider<ChatClient> assetFileSubAgentChatClientProvider, boolean assetFileWriteCapable,
        ObjectProvider<ChatClient> clusterElementSubAgentChatClientProvider,
        ObjectProvider<ChatClient> codeEditorSubAgentChatClientProvider,
        ObjectProvider<ChatClient> workflowEditorSubAgentChatClientProvider,
        @Nullable ObjectProvider<Supplier<ChatClient>> converterSubAgentChatClientSupplierProvider,
        ObjectProvider<ChatClient> workflowExecutionSubAgentChatClientProvider,
        ObjectProvider<ChatClient> customComponentSubAgentChatClientProvider,
        ObjectProvider<ChatClient> codeWorkflowSubAgentChatClientProvider, @Nullable AiGuardrails aiGuardrails,
        @Nullable AiGuardrailMetrics aiGuardrailMetrics, @Nullable WorkspaceSystemPrompts workspaceSystemPrompts,
        @Nullable AiHubSessionMemory aiHubSessionMemory) {

        // The memory key is CopilotAgentType.SKILLS ("skills"), not this callback's "skills_agent" progress label —
        // there is no skills_agent agent type, and an unregistered key would leave a session the task-delete purge
        // could never reconstruct.
        skillsSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new SkillsAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.SKILLS.key(), aiGuardrails, aiGuardrailMetrics,
                            workspaceSystemPrompts, aiHubSessionMemory)),
                    "skills_agent")));

        contextStoreSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new ContextStoreAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.CONTEXT_STORE_AGENT.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "context_store_agent")));

        knowledgeBaseSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new KnowledgeBaseAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.KNOWLEDGE_BASE_AGENT.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "knowledge_base_agent")));

        dataTableSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new DataTableAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.DATA_TABLE_AGENT.key(), aiGuardrails, aiGuardrailMetrics,
                            workspaceSystemPrompts, aiHubSessionMemory)),
                    "data_table_agent")));

        aiAgentSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new AiAgentAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.AI_AGENT_AGENT.key(), aiGuardrails, aiGuardrailMetrics,
                            workspaceSystemPrompts, aiHubSessionMemory)),
                    "ai_agent_agent")));

        assetFileSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new AssetFileAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.ASSET_FILE_AGENT.key(), aiGuardrails, aiGuardrailMetrics,
                            workspaceSystemPrompts, aiHubSessionMemory),
                        assetFileWriteCapable),
                    "asset_file_agent")));

        clusterElementSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new ClusterElementAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.CLUSTER_ELEMENT_AGENT.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "cluster_element_agent")));

        codeEditorSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new CodeEditorAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.CODE_EDITOR_AGENT.key(), aiGuardrails, aiGuardrailMetrics,
                            workspaceSystemPrompts, aiHubSessionMemory)),
                    "code_editor_agent")));

        workflowEditorSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new WorkflowEditorAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.WORKFLOW_EDITOR_AGENT.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "workflow_editor_agent")));

        workflowExecutionSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new WorkflowExecutionAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.WORKFLOW_EXECUTION_AGENT.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "workflow_execution_agent")));

        if (converterSubAgentChatClientSupplierProvider != null) {
            converterSubAgentChatClientSupplierProvider.ifAvailable(
                converterChatClientSupplier -> toolCallbacks.add(
                    new ProgressReportingToolCallback(
                        new ConverterAgentToolCallback(
                            () -> wrapDelegate(
                                converterChatClientSupplier.get(), CopilotAgentType.CONVERTER_AGENT.key(),
                                aiGuardrails, aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                        "converter_agent")));
        }

        customComponentSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new CustomComponentAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.CUSTOM_COMPONENT_AGENT.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "custom_component_agent")));

        codeWorkflowSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new CodeWorkflowAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.CODE_WORKFLOW_AGENT.key(), aiGuardrails,
                            aiGuardrailMetrics, workspaceSystemPrompts, aiHubSessionMemory)),
                    "code_workflow_agent")));
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
        // Unified kind-keyed lookup/select pair replaces the four action/trigger twins on the pinned list. The
        // emitted select-property-option marker is unchanged, so the chat client's picker rendering still works.
        toolCallbacks.add(
            new LookupComponentPropertyOptionsToolCallback(
                actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                propertyOptionsResolver, aiHubToolAttachMetrics));
        toolCallbacks.add(
            new SelectComponentPropertyOptionToolCallback(
                actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                propertyOptionsResolver, aiHubToolAttachMetrics));
    }

    private String getSystemPrompt(Resource systemPromptResource, boolean researchToolAvailable) {
        try {
            InputStream inputStream = systemPromptResource.getInputStream();

            return filterResearchSection(
                new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), researchToolAvailable);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }

    /**
     * Keeps the prompt's research documentation in sync with the conditionally registered research tool: every section
     * between {@code [[research:start]]} / {@code [[research:end]]} marker lines is kept (markers stripped) when the
     * tool is registered and removed entirely when it is not, so the model is never told about a tool it cannot call. A
     * prompt without markers passes through unchanged. Package-private for tests.
     */
    static String filterResearchSection(String systemPrompt, boolean researchToolAvailable) {
        if (researchToolAvailable) {
            return systemPrompt
                .replace(RESEARCH_SECTION_START_MARKER + "\n", "")
                .replace(RESEARCH_SECTION_END_MARKER + "\n", "")
                .replace(RESEARCH_SECTION_START_MARKER, "")
                .replace(RESEARCH_SECTION_END_MARKER, "");
        }

        String filteredSystemPrompt = systemPrompt;

        while (true) {
            int startIndex = filteredSystemPrompt.indexOf(RESEARCH_SECTION_START_MARKER);

            if (startIndex < 0) {
                return filteredSystemPrompt;
            }

            int endIndex = filteredSystemPrompt.indexOf(RESEARCH_SECTION_END_MARKER, startIndex);

            if (endIndex < 0) {
                return filteredSystemPrompt;
            }

            int afterEndIndex = endIndex + RESEARCH_SECTION_END_MARKER.length();

            if (afterEndIndex < filteredSystemPrompt.length() && filteredSystemPrompt.charAt(afterEndIndex) == '\n') {
                afterEndIndex++;

                // The block is surrounded by blank lines; dropping one avoids leaving a double blank line behind.
                if (afterEndIndex < filteredSystemPrompt.length()
                    && filteredSystemPrompt.charAt(afterEndIndex) == '\n') {

                    afterEndIndex++;
                }
            }

            filteredSystemPrompt =
                filteredSystemPrompt.substring(0, startIndex) + filteredSystemPrompt.substring(afterEndIndex);
        }
    }
}
