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
import com.bytechef.ai.copilot.tool.CreateConnectionToolCallback;
import com.bytechef.ai.copilot.tool.ListConnectionsForComponentToolCallback;
import com.bytechef.ai.copilot.tool.LookupComponentPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.SelectComponentPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.SelectConnectionToolCallback;
import com.bytechef.ai.copilot.tool.WorkspaceCopilotConnectionLister;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.datatable.DataTableToolCallbacksFactory;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
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
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.guardrails.SubAgentGuardrailedChatClient;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.ee.ai.hub.progress.ProgressReportingToolCallback;
import com.bytechef.ee.ai.hub.subagent.SubAgentAdvisorContributor;
import com.bytechef.ee.ai.hub.subagent.SubAgentSessionMemoryContributor;
import com.bytechef.ee.ai.hub.subagent.WorkspaceAdvisorContributor;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AttachChatToolToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateWorkflowChatToolCallback;
import com.bytechef.ee.ai.hub.tool.ListAiHubChatsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListChatToolsToolCallback;
import com.bytechef.ee.ai.hub.tool.ListChatWorkflowsToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenResourceTabToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenWorkflowChatTabToolCallback;
import com.bytechef.ee.ai.hub.tool.RemoveChatToolToolCallback;
import com.bytechef.ee.ai.hub.tool.RunChatWorkflowToolCallback;
import com.bytechef.ee.ai.hub.tool.memory.DbAutoMemoryDirectoryOps;
import com.bytechef.ee.ai.hub.tool.memory.DbMemoryResourceResolver;
import com.bytechef.ee.ai.hub.toolsearch.AiHubChatBindingToolCallbackResolver;
import com.bytechef.ee.ai.hub.toolsearch.AiHubGlobalToolCatalog;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.util.Source;
import com.bytechef.ee.automation.ai.tool.ApiCollectionToolCallbacksFactory;
import com.bytechef.ee.automation.ai.tool.PromoteToEnvironmentToolCallback;
import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
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
import com.bytechef.platform.configuration.service.EnvironmentService;
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
import java.util.stream.Collectors;
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
 * The {@code @Bean} methods consume both CC domain types (chat / memory / task services + Command Center–owned tool
 * callbacks) and shared LLM infrastructure (chat memory advisor, tool search advisor, project / data-table /
 * knowledge-base / mcp-project tool callbacks).
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

    /**
     * Names of the {@code com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition}s the AI Hub registers on both
     * its ASK and BUILD agents, filtered with {@link IntelligentToolCatalog#getByNames} over its own name partition.
     *
     * <p>
     * This is deliberately NINE of the catalog's ten names, not all ten: {@code buildIntegrationWorkflow} is an
     * embedded-product delegate (the embedded counterpart of {@code buildWorkflow}) that reaches only the embedded
     * management-MCP surface — the AI Hub has never registered it and its prompts do not mention it. This is the one
     * deliberate hub/MCP parity gap; {@code IntelligentToolSurfaceParityTest} asserts it explicitly. Every name here
     * has a {@code null} ASK {@code chatClientFactory} except the eight pre-existing ones — {@code configureMcpServer}
     * (the promoted {@code mcp_agent} delegate) is BUILD-only, matching {@code importWorkflow}'s shape, so the catalog
     * silently skips it on the ASK agent's own {@link #registerIntelligentToolCallbacks} call.
     * </p>
     */
    static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of(
        "authorSkill", "configureClusterElement", "writeScript", "buildWorkflow",
        "debugWorkflowExecution", "importWorkflow", "buildCustomComponent", "buildCodeWorkflow",
        "configureMcpServer");

    /**
     * The one tool in {@link McpServerToolCallbacksFactory#writeToolCallbacks()}'s seven-tool write leg that stays OFF
     * both flat surfaces (ticket 732, Task 3): the tool-mapping mutation lives exclusively inside the
     * {@code configureMcpServer} intelligent tool's inner two-tool ChatClient (see
     * {@code McpServerSubAgentConfiguration}), so flattening it here too would duplicate the mapping capability on two
     * paths with different judgment behind them.
     */
    private static final String MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME = "updateMcpProjectWorkflowParameters";

    private final Resource promptAiHubAskResource;
    private final Resource promptAiHubAutoMemoryToolsResource;
    private final Resource promptAiHubBuildResource;
    private final State state = new State();

    // RUBY-DISABLED: the prompt resources loaded below had every Ruby reference DELETED, not commented out.
    // getSystemPrompt() reads them with a verbatim readAllBytes and filters nothing but the RESEARCH_SECTION
    // markers, so the rest of the file becomes the system message as-is — a commented-out Ruby line would still
    // be read by the model as content and it would keep offering a language that org.graalvm.polyglot:ruby
    // (stuck at 25.0.0, crashes on the pinned Truffle 25.2.4) can no longer run. The marker therefore lives
    // here, in code the model never sees.
    // Affected: prompt_ai_hub_build.txt and prompt_ai_hub_ask.txt — the buildCodeWorkflow and writeScript router
    // entries, which named Ruby as an authoring language. These mirror the subagent DESCRIPTION constants in
    // CodeWorkflowAgentToolCallback and CodeEditorAgentToolCallback; restore both together once a polyglot ruby jar
    // built on Truffle 25.2+ is published (or GraalVM is downgraded). Grep RUBY-DISABLED.
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
        ObjectProvider<McpServerToolCallbacksFactory> mcpServerToolCallbacksFactoryProvider,
        ObjectProvider<ApiCollectionToolCallbacksFactory> apiCollectionToolCallbacksFactoryProvider,
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        ObjectProvider<AssetFileToolCallbacksFactory> assetFileToolCallbacksFactoryProvider,
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider,
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider,
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider,
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider,
        AiHubChatService chatService,
        AiAutoMemoryService aiHubMemoryService,
        AiHubChatToolFacade chatToolFacade,
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService,
        WorkspaceConnectionFacade workspaceConnectionFacade,
        ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade,
        SecurityContextRehydrator securityContextRehydrator,
        PropertyOptionsResolver propertyOptionsResolver,
        @Qualifier("aiHubAskToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        ObjectProvider<AiHubChatBindingToolCallbackResolver> chatBindingToolCallbackResolverProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<LlmUsageRecorder> llmUsageRecorderProvider,
        ObjectProvider<AiGuardrails> aiGuardrailsProvider, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<WorkspaceSystemPrompts> workspaceSystemPromptsProvider,
        AiHubToolAttachMetrics aiHubToolAttachMetrics, JsonMapper jsonMapper,
        IntelligentToolCatalog intelligentToolCatalog)
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
        // Read-only asset-file access (ticket 732, Task 4) — see assetFileFlatCrudToolCallbacks' javadoc. The
        // ASK prompt documents listAssetFiles/getAssetFileContent, and without these registrations the model's
        // direct calls fail with "No ToolCallback found". Creation stays BUILD-only.
        toolCallbacks.addAll(assetFileFlatCrudToolCallbacks(assetFileToolCallbacksFactoryProvider, false));
        // Data-table, knowledge-base, context-store, and AI-Agent-builder reads are flat (Tasks 5-8); mutations
        // are catalog-demoted.
        toolCallbacks.addAll(dataTableFlatCrudToolCallbacks(dataTableToolCallbacksFactoryProvider));
        toolCallbacks.addAll(knowledgeBaseFlatCrudToolCallbacks(knowledgeBaseToolCallbacksFactoryProvider));
        toolCallbacks.addAll(contextStoreFlatCrudToolCallbacks(contextStoreToolCallbacksFactoryProvider));
        toolCallbacks.addAll(aiAgentFlatCrudToolCallbacks(aiAgentToolCallbacksFactoryProvider));
        // attachChatTool/removeChatTool are deliberately NOT registered here: the ASK prompt declares tool
        // attachment a BUILD-only mutation ("suggest switching to BUILD mode"), so the registrations were
        // dead weight the prompt forbade the model from using.
        toolCallbacks.add(new AskUserQuestionToolCallback(aiHubToolAttachMetrics));

        // Chat / connection state visibility — read-only. Lets the LLM avoid duplicate attaches and pick
        // existing connections before escalating to createConnection.
        registerToolAttachStateVisibilityToolCallbacks(
            toolCallbacks, chatService, chatToolFacade, componentDefinitionService, connectionDefinitionService,
            workspaceConnectionFacade, actionDefinitionService, actionDefinitionFacade, triggerDefinitionService,
            triggerDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics, jsonMapper);

        // Resource-discovery tools surface workspace state the LLM may want to reference but doesn't yet have a
        // way to enumerate. Read-only — both the ASK and BUILD agents register them so a casual ASK turn can
        // resolve "the staging customers API" / "my last research thread" to a concrete id without forcing the
        // user to switch into BUILD just to look something up. Workflow-execution lookups are now delegated to
        // the debugWorkflowExecution specialist (registered via registerIntelligentToolCallbacks).
        toolCallbacks.add(new ListAiHubChatsToolCallback(chatService));
        // MCP server / API-collection / project-deployment read legs (ticket 732, Tasks 2-3) — see each
        // *FlatCrudToolCallbacks helper's own javadoc.
        toolCallbacks.addAll(mcpServerFlatCrudToolCallbacks(mcpServerToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(apiCollectionFlatCrudToolCallbacks(apiCollectionToolCallbacksFactoryProvider, false));
        toolCallbacks.addAll(deploymentFlatCrudToolCallbacks(deploymentToolCallbacksFactoryProvider, false));

        // Intelligent delegate tools (skills, cluster element, code editor, project workflow, workflow
        // execution, converter, custom component, code workflow) come from the shared catalog. Converter has
        // no ASK subagent ChatClient (Copilot ships only a BUILD-mode Converter agent), so the catalog skips
        // it for this variant — matching the pre-catalog behaviour of passing null for the converter here.
        registerIntelligentToolCallbacks(
            toolCallbacks, intelligentToolCatalog, IntelligentToolVariant.ASK, aiGuardrails, aiGuardrailMetrics,
            workspaceSystemPrompts, aiHubSessionMemory);

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

        chatBindingToolCallbackResolverProvider.ifAvailable(builder::chatToolBindingResolver);

        // Per-task LLM model override. Bean is only present when AI Gateway is enabled; absent → no
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
        ObjectProvider<McpServerToolCallbacksFactory> mcpServerToolCallbacksFactoryProvider,
        ObjectProvider<AssetFileToolCallbacksFactory> assetFileToolCallbacksFactoryProvider,
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider,
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider,
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider,
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider,
        AssetFileFacade assetFileFacade, AiHubChatArtifactService chatArtifactService,
        AiHubChatArtifactRecorder aiHubChatArtifactRecorder,
        AiHubChatService chatService, AiAutoMemoryService aiHubMemoryService,
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
        AiHubChatToolFacade chatToolFacade,
        ObjectProvider<ApiCollectionToolCallbacksFactory> apiCollectionToolCallbacksFactoryProvider,
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        @Qualifier("aiHubBuildToolSearchToolCallAdvisor") //
        ObjectProvider<ToolSearchToolCallingAdvisor> toolSearchToolCallAdvisorProvider,
        ObjectProvider<AiHubChatBindingToolCallbackResolver> chatBindingToolCallbackResolverProvider,
        ObjectProvider<AiHubSpringAIAgent.OverrideChatClientResolver> overrideChatClientResolverProvider,
        ObjectProvider<LlmUsageRecorder> llmUsageRecorderProvider,
        ObjectProvider<AiGuardrails> aiGuardrailsProvider, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<WorkspaceSystemPrompts> workspaceSystemPromptsProvider,
        AiHubToolAttachMetrics aiHubToolAttachMetrics, JsonMapper jsonMapper,
        IntelligentToolCatalog intelligentToolCatalog)
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

        // Consolidated open-tab tool (type-keyed) replaces the seven per-resource variants on the pinned list.
        toolCallbacks.add(new OpenResourceTabToolCallback(aiHubChatArtifactRecorder));
        toolCallbacks.add(new OpenWorkflowChatTabToolCallback());
        toolCallbacks.add(
            new ListChatWorkflowsToolCallback(
                projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService,
                triggerDefinitionService, workflowFacade, workflowService));
        toolCallbacks.add(
            new RunChatWorkflowToolCallback(
                projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService,
                workflowFacade, workflowService, chatArtifactService));
        // createWorkflowChat is demoted to the searchable catalog (aiHubBuildGlobalToolCatalog) — rare enough
        // that it should not ride in every model call.

        // API-collection CRUD is flat (ticket 732, Task 2); see apiCollectionFlatCrudToolCallbacks' javadoc.
        toolCallbacks.addAll(apiCollectionFlatCrudToolCallbacks(apiCollectionToolCallbacksFactoryProvider, true));
        // Project-deployment CRUD is flat (ticket 732, Task 3); see deploymentFlatCrudToolCallbacks' javadoc.
        toolCallbacks.addAll(deploymentFlatCrudToolCallbacks(deploymentToolCallbacksFactoryProvider, true));
        // Data-table reads are flat (ticket 732, Task 5); see dataTableFlatCrudToolCallbacks' javadoc. The eight
        // mutations are catalog-demoted instead of pinned here — see aiHubBuildGlobalToolCatalog's javadoc.
        toolCallbacks.addAll(dataTableFlatCrudToolCallbacks(dataTableToolCallbacksFactoryProvider));
        // Knowledge-base reads are flat (ticket 732, Task 6); see knowledgeBaseFlatCrudToolCallbacks' javadoc.
        // The five mutations are catalog-demoted instead of pinned here — see aiHubBuildGlobalToolCatalog's javadoc.
        toolCallbacks.addAll(knowledgeBaseFlatCrudToolCallbacks(knowledgeBaseToolCallbacksFactoryProvider));
        // Context-store reads are flat (ticket 732, Task 7); see contextStoreFlatCrudToolCallbacks' javadoc. The
        // six mutations are catalog-demoted instead of pinned here — see aiHubBuildGlobalToolCatalog's javadoc.
        toolCallbacks.addAll(contextStoreFlatCrudToolCallbacks(contextStoreToolCallbacksFactoryProvider));
        // AI-Agent-builder reads are flat (ticket 732, Task 8 — the LAST CRUD-delegate-unwind task); see
        // aiAgentFlatCrudToolCallbacks' javadoc. The nine mutations are catalog-demoted instead of pinned here —
        // see aiHubBuildGlobalToolCatalog's javadoc.
        toolCallbacks.addAll(aiAgentFlatCrudToolCallbacks(aiAgentToolCallbacksFactoryProvider));

        // Intelligent delegate tools (skills, cluster element, code editor, project workflow, workflow
        // execution, converter, custom component, code workflow) come from the shared catalog, including the
        // BUILD-only Converter sub-agent.
        registerIntelligentToolCallbacks(
            toolCallbacks, intelligentToolCatalog, IntelligentToolVariant.BUILD, aiGuardrails, aiGuardrailMetrics,
            workspaceSystemPrompts, aiHubSessionMemory);

        toolCallbacks.add(new CreateConnectionToolCallback(componentDefinitionService));
        toolCallbacks.add(new SelectConnectionToolCallback(componentDefinitionService));

        toolCallbacks.add(
            new AttachChatToolToolCallback(chatService, chatToolFacade, connectionService, aiHubToolAttachMetrics));
        toolCallbacks.add(
            new RemoveChatToolToolCallback(chatService, chatToolFacade));
        toolCallbacks.add(new AskUserQuestionToolCallback(aiHubToolAttachMetrics));

        // Chat / connection state visibility — mirrors the ASK agent. The two callbacks together let the LLM
        // resolve "is this already set up?" and "do I have a connection for this?" without escalating to the user.
        registerToolAttachStateVisibilityToolCallbacks(
            toolCallbacks, chatService, chatToolFacade, componentDefinitionService, connectionDefinitionService,
            workspaceConnectionFacade, actionDefinitionService, actionDefinitionFacade, triggerDefinitionService,
            triggerDefinitionFacade, propertyOptionsResolver, aiHubToolAttachMetrics, jsonMapper);

        // MCP-server flat CRUD (Task 3) — see mcpServerFlatCrudToolCallbacks' javadoc.
        toolCallbacks.addAll(mcpServerFlatCrudToolCallbacks(mcpServerToolCallbacksFactoryProvider, true));

        // Resource discovery — read-only and always-on. Mirrors the same registrations on the ASK agent so
        // a "list my chats" turn works identically regardless of which mode is active. Workflow-execution
        // lookups are delegated to the debugWorkflowExecution specialist.
        toolCallbacks.add(new ListAiHubChatsToolCallback(chatService));

        // Auto-memory is now exposed via the forked AutoMemoryToolsAdvisor (DB-backed Resource seam),
        // registered as an advisor below rather than as standalone tool callbacks.

        // Asset-file CRUD is flat (ticket 732, Task 4 of the CRUD-delegate unwind); see
        // assetFileFlatCrudToolCallbacks' javadoc.
        toolCallbacks.addAll(assetFileFlatCrudToolCallbacks(assetFileToolCallbacksFactoryProvider, true));

        AiHubSpringAIAgent.Builder buildBuilder = AiHubSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptAiHubBuildResource, researchToolAvailable))
            .toolCallbacks(toolCallbacks)
            .threadUserIdResolver(threadId -> chatService.findByThreadId(threadId)
                .map(AiHubChat::getUserId)
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

        chatBindingToolCallbackResolverProvider.ifAvailable(buildBuilder::chatToolBindingResolver);
        overrideChatClientResolverProvider.ifAvailable(buildBuilder::overrideChatClientResolver);

        // Per-turn token metering into ai_llm_usage (source = AI_HUB). Absent recorder → advisor logs only.
        llmUsageRecorderProvider.ifAvailable(buildBuilder::llmUsageRecorder);

        // Workspace content guardrails on every LLM turn this agent resolves a ChatClient for — mirrors
        // aiHubAskSpringAIAgent, including task model-override turns (see
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
     * and into the subagent registration helper ({@link #registerSubAgentToolCallbacks}) so the top-level agent and
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
        WebhookWorkflowExecutor webhookFacade, AiHubChatService chatService,
        WebhookResumeRegistry webhookResumeRegistry, JsonMapper jsonMapper, AssetFileFacade assetFileFacade,
        WorkflowChatMetrics workflowChatMetrics, WorkflowChatJobRegistry workflowChatJobRegistry,
        AiHubSessionMemory aiHubSessionMemory, WorkflowChatGuard workflowChatGuard,
        ObjectProvider<com.bytechef.atlas.execution.facade.JobFacade> jobFacadeProvider) throws AGUIException {

        return new WebhookBridgeAgent(
            webhookFacade, chatService, webhookResumeRegistry, jsonMapper, assetFileFacade,
            workflowChatMetrics, workflowChatJobRegistry, aiHubSessionMemory, workflowChatGuard,
            jobFacadeProvider.getIfAvailable());
    }

    @Bean
    AiHubRoutingAgent aiHubAskRoutingAgent(
        @Qualifier("aiHubAskSpringAIAgent") AiHubSpringAIAgent aiHubAskSpringAIAgent,
        ObjectProvider<WebhookBridgeAgent> webhookBridgeAgentProvider,
        AiHubChatService chatService, AssetFileFacade assetFileFacade)
        throws AGUIException {

        return new AiHubRoutingAgent(
            (Source.AI_HUB.name() + "_" + Mode.ASK.name()).toLowerCase(),
            aiHubAskSpringAIAgent,
            webhookBridgeAgentProvider.getIfAvailable(),
            chatService, assetFileFacade);
    }

    @Bean
    AiHubRoutingAgent aiHubBuildRoutingAgent(
        @Qualifier("aiHubBuildSpringAIAgent") AiHubSpringAIAgent aiHubBuildSpringAIAgent,
        ObjectProvider<WebhookBridgeAgent> webhookBridgeAgentProvider,
        AiHubChatService chatService, AssetFileFacade assetFileFacade)
        throws AGUIException {

        return new AiHubRoutingAgent(
            (Source.AI_HUB.name() + "_" + Mode.BUILD.name()).toLowerCase(),
            aiHubBuildSpringAIAgent,
            webhookBridgeAgentProvider.getIfAvailable(),
            chatService, assetFileFacade);
    }

    @Bean
    AiHubGlobalToolCatalog aiHubAskGlobalToolCatalog(
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, TaskDispatcherTools taskDispatcherTools,
        WorkflowExecutionTools workflowExecutionTools) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        Collections.addAll(
            toolCallbacks,
            ToolCallbacks.from(
                readProjectTools, readProjectWorkflowTools, componentTools, taskTools, taskDispatcherTools,
                workflowExecutionTools));

        // listApiCollections used to be demoted here (rarely-used read, kept out of the pinned list). Ticket 732,
        // Task 2 of the CRUD-delegate unwind pins it instead alongside its two write siblings — see
        // apiCollectionFlatCrudToolCallbacks' javadoc.

        return new AiHubGlobalToolCatalog(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, toolCallbacks);
    }

    @Bean
    AiHubGlobalToolCatalog aiHubBuildGlobalToolCatalog(
        ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, ComponentTools componentTools,
        TaskTools taskTools, TaskDispatcherTools taskDispatcherTools, ScriptTools scriptTools,
        ClusterElementTools clusterElementTools, WorkflowExecutionTools workflowExecutionTools,
        AiHubChatService chatService,
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider,
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider,
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider,
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider,
        ObjectProvider<EnvironmentPromotionFacade> environmentPromotionFacadeProvider,
        EnvironmentService environmentService) {

        // cloneAssetFile is pinned flat on the BUILD agent (see assetFileFlatCrudToolCallbacks), not catalog-
        // demoted — it rides with the other six asset-file tools as one coherent CRUD surface.
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        Collections.addAll(
            toolCallbacks,
            ToolCallbacks.from(
                projectTools, projectWorkflowTools, componentTools, taskTools, taskDispatcherTools, scriptTools,
                clusterElementTools, workflowExecutionTools));

        toolCallbacks.add(new CreateWorkflowChatToolCallback(chatService));

        // Data-table mutations are catalog-demoted rather than pinned (ticket 732, Task 5 of the CRUD-delegate
        // unwind) — see dataTableCatalogToolCallbacks' javadoc for why this domain departs from the smaller
        // domains' precedent of pinning everything.
        toolCallbacks.addAll(dataTableCatalogToolCallbacks(dataTableToolCallbacksFactoryProvider));

        // Knowledge-base mutations are catalog-demoted rather than pinned (ticket 732, Task 6 of the CRUD-delegate
        // unwind) — see knowledgeBaseCatalogToolCallbacks' javadoc.
        toolCallbacks.addAll(knowledgeBaseCatalogToolCallbacks(knowledgeBaseToolCallbacksFactoryProvider));

        // Context-store mutations are catalog-demoted rather than pinned (ticket 732, Task 7 of the CRUD-delegate
        // unwind) — see contextStoreCatalogToolCallbacks' javadoc.
        toolCallbacks.addAll(contextStoreCatalogToolCallbacks(contextStoreToolCallbacksFactoryProvider));

        // AI-Agent-builder mutations are catalog-demoted rather than pinned (ticket 732, Task 8 of the
        // CRUD-delegate unwind, the LAST delegate) — see aiAgentCatalogToolCallbacks' javadoc.
        toolCallbacks.addAll(aiAgentCatalogToolCallbacks(aiAgentToolCallbacksFactoryProvider));

        // Environment promotion: catalog rather than pinned, because promoting is rare and every pinned tool pays
        // schema tokens on every model iteration. Catalog tools are security-context-rehydration-wrapped by
        // ToolSearchAdvisorConfiguration, which is also what lets them reach the facade's @PreAuthorize guards from
        // a worker thread. ObjectProvider because promotion is wired into server-app only: in a distributed EE
        // deployment the facade bean is absent and the tools are simply not registered, rather than failing startup.
        environmentPromotionFacadeProvider.ifAvailable(environmentPromotionFacade -> {
            for (PromotionResourceType promotionResourceType : PromotionResourceType.values()) {
                toolCallbacks.add(
                    new PromoteToEnvironmentToolCallback(
                        promotionResourceType, environmentPromotionFacade, environmentService));
            }
        });

        return new AiHubGlobalToolCatalog(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID, toolCallbacks);
    }

    /**
     * Wraps one delegate's {@code ChatClient} in everything a specialist call needs per request: the calling
     * workspace's guardrails and system prompt, and that specialist's own per-conversation session memory.
     *
     * <p>
     * Every AI Hub delegate registration goes through here rather than calling
     * {@link SubAgentGuardrailedChatClient#wrap(ChatClient, List)} directly, so no hub site can quietly forget the
     * memory contributor and leave one specialist amnesiac while the rest remember.
     * </p>
     *
     * <p>
     * {@code agentTypeKey} MUST be a key registered with {@code AgentTypeRegistry}: it becomes the session-id suffix,
     * and the purge that runs when an AI Hub chat is deleted reconstructs the keys to delete from that registry. A key
     * that is not registered would produce a session nothing ever deletes.
     * </p>
     *
     * <p>
     * <b>The ask capability is deliberately NOT attached here.</b> A third contributor used to be —
     * {@code SubAgentAskToolContributor}, which let an allow-listed specialist pose a question to the user, gated on an
     * {@code ASK_CAPABLE_AGENT_TYPE_KEYS} set. That set decayed to an empty compile-time {@code Set.of()} once its last
     * member was dissolved, and the whole interactive-question stack was removed as unreachable. It is back (ticket
     * 732), rebuilt one layer out: {@code IntelligentToolCatalog#buildToolCallback} attaches the specialist-facing
     * {@code askUserQuestion} tool to every intelligent delegate's own {@code ChatClient} and wraps the delegate in
     * {@code SubAgentAskRelayToolCallback}, which carries a raised question out as the delegate's own tool result. One
     * seam serves the AI Hub, the Copilot panels and the management MCP server alike, a delegate added later inherits
     * the capability instead of having to remember, and there is no allowlist left to decay. Exactly two classes from
     * the old stack did not come back and have no replacement: {@code SubAgentToolCallback} (the delegate callback the
     * relay was built INTO — today's intelligent delegates are their own classes) and
     * {@code SubAgentAskToolContributor} (this method's gate). Surviving javadoc mentions of either are historical.
     * </p>
     *
     * <p>
     * <b>Session memory reaches a specialist ONLY through this method, and that asymmetry is load-bearing.</b>
     * {@link SubAgentSessionMemoryContributor} is attached here and nowhere else. Every other surface that builds the
     * same delegates — {@code ProjectAgentConfiguration} and {@code McpServerAgentConfiguration} through
     * {@code IntelligentToolCatalog#getForPanel}, and the three management-MCP contributors through {@code #getByNames}
     * — passes an identity {@code chatClientDecorator}; the MCP surface additionally carries no conversation id for
     * {@link SubAgentSessionMemoryContributor} to key a session on, so memory could not be attached there even if a
     * decorator wanted to. A specialist that asks a question on a panel or over MCP therefore starts from nothing on
     * the call that brings the answer back. Nothing user-facing may say otherwise: the ask tool's description, its stop
     * instruction, {@code SubAgentQuestionFormatter}'s re-invocation sentence and the management MCP server's
     * {@code instructions} each claimed a surviving context at some point and were each corrected to tell the caller to
     * restate what the specialist needs.
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

        return SubAgentGuardrailedChatClient.wrap(chatClient, contributors);
    }

    /**
     * Registers the {@link #INTELLIGENT_TOOL_NAMES} delegates from the shared {@link IntelligentToolCatalog} for the
     * given {@code variant}, decorated exactly like the pre-catalog registrations: each delegate's {@code ChatClient}
     * is wrapped with {@link #wrapDelegate} and each resulting {@link ToolCallback} is wrapped in a
     * {@link ProgressReportingToolCallback} labelled with the definition's own name. Extracted to keep both agent bean
     * methods within Checkstyle's per-method line limit.
     */
    private static void registerIntelligentToolCallbacks(
        List<ToolCallback> toolCallbacks, IntelligentToolCatalog intelligentToolCatalog,
        IntelligentToolVariant variant, @Nullable AiGuardrails aiGuardrails,
        @Nullable AiGuardrailMetrics aiGuardrailMetrics, @Nullable WorkspaceSystemPrompts workspaceSystemPrompts,
        @Nullable AiHubSessionMemory aiHubSessionMemory) {

        toolCallbacks.addAll(
            intelligentToolCatalog.getByNames(
                INTELLIGENT_TOOL_NAMES, variant,
                (chatClient, definition) -> wrapDelegate(
                    chatClient, definition.agentTypeKey(), aiGuardrails, aiGuardrailMetrics, workspaceSystemPrompts,
                    aiHubSessionMemory),
                (toolCallback, definition) -> new ProgressReportingToolCallback(toolCallback, definition.name())));
    }

    /**
     * Registers the optional ChatClient-based sub-agent tool callbacks (research, data analyst, image generator, slide
     * builder) on the supplied tool list. Each is only added when its backing ChatClient bean is present. Extracted to
     * keep the BUILD-agent bean method within Checkstyle's per-method line limit.
     *
     * <p>
     * The older {@code workflow_builder} ChatClient sub-agent is intentionally absent — it has been superseded by the
     * Copilot {@code buildWorkflow} specialist registered through {@link #registerIntelligentToolCallbacks}. The
     * Copilot specialist persists workflows internally via {@code ProjectWorkflowTools}, eliminating the JSON
     * round-trip {@code workflow_builder} required.
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

    // There is no longer a registerCopilotSubAgentToolCallbacks method: it used to register the Copilot CRUD
    // specialist sub-agent ToolCallbacks (asset_file_agent, data_table_agent, knowledge_base_agent,
    // context_store_agent, ai_agent_agent) on the supplied tool list, one ChatClient-backed delegate at a time. All
    // five are gone now (ticket 732, CRUD-delegate-unwind Tasks 4-8 — ai_agent_agent was the LAST one, Task 8): their
    // reads are registered flat instead (see assetFileFlatCrudToolCallbacks, dataTableFlatCrudToolCallbacks,
    // knowledgeBaseFlatCrudToolCallbacks, contextStoreFlatCrudToolCallbacks, aiAgentFlatCrudToolCallbacks below) and
    // every mutation set except asset-file's is catalog-demoted (see dataTableCatalogToolCallbacks,
    // knowledgeBaseCatalogToolCallbacks, contextStoreCatalogToolCallbacks, aiAgentCatalogToolCallbacks below). The
    // intelligent delegates that used to live in the deleted method too (skills, cluster element, code editor,
    // project workflow, workflow execution, converter, custom component, code workflow) are registered from the
    // shared IntelligentToolCatalog at the call sites instead — see INTELLIGENT_TOOL_NAMES.

    /**
     * Registers state-visibility callbacks for the autonomous tool-attach flow: {@code listChatTools} and
     * {@code listConnectionsForComponent}. Read-only on both agents — the LLM uses them to avoid duplicate attaches and
     * to surface an existing connection before falling back to {@code createConnection}.
     */
    private static void registerToolAttachStateVisibilityToolCallbacks(
        List<ToolCallback> toolCallbacks, AiHubChatService chatService, AiHubChatToolFacade chatToolFacade,
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService, WorkspaceConnectionFacade workspaceConnectionFacade,
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        PropertyOptionsResolver propertyOptionsResolver, AiHubToolAttachMetrics aiHubToolAttachMetrics,
        JsonMapper jsonMapper) {

        toolCallbacks.add(new ListChatToolsToolCallback(chatService, chatToolFacade, aiHubToolAttachMetrics,
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

    /**
     * The MCP server CRUD tools flattened onto this surface (ticket 732, Task 3): {@code listMcpServers} +
     * {@code listMcpProjectWorkflows} on both agents, plus (write-only) {@code createMcpServer},
     * {@code updateMcpServer}, {@code createMcpProject}, {@code cloneMcpProject} on BUILD — mirroring how every other
     * flat domain in this class splits its read/write leg between ASK and BUILD. Filters
     * {@link McpServerToolCallbacksFactory#writeToolCallbacks()} down to exclude
     * {@value #MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME}, see that constant's javadoc. An absent factory bean (Copilot
     * disabled, or the MCP facades not on the classpath) resolves to an empty list — the same silent-skip degrade every
     * other Copilot-domain registration in this class already follows.
     *
     * <p>
     * No context wrapping is needed here (unlike the management MCP surface's {@code WorkspaceScopedFlatToolCallback}):
     * every tool call routed through {@link AiHubSpringAIAgent#toolContext} already carries
     * {@link com.bytechef.automation.ai.tool.AutomationToolInvocationContext}-compatible keys — the exact family
     * {@code listMcpServers}/{@code createMcpServer} read — for every registered pinned tool, not just MCP ones.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationMcpServerFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> mcpServerFlatCrudToolCallbacks(
        ObjectProvider<McpServerToolCallbacksFactory> mcpServerToolCallbacksFactoryProvider, boolean writable) {

        McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = mcpServerToolCallbacksFactoryProvider
            .getIfAvailable();

        if (mcpServerToolCallbacksFactory == null) {
            return List.of();
        }

        if (!writable) {
            return mcpServerToolCallbacksFactory.readToolCallbacks();
        }

        return mcpServerToolCallbacksFactory.writeToolCallbacks()
            .stream()
            .filter(toolCallback -> !MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME.equals(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();
    }

    /**
     * The three API-collection CRUD tools flattened onto this surface (ticket 732, Task 2 of the CRUD-delegate unwind),
     * replacing the dissolved {@code api_collection_agent} delegate: {@code listApiCollections} on both agents, plus
     * (write-only) {@code createApiCollection} on BUILD — mirroring how every other flat domain in this class splits
     * its read/write leg between ASK and BUILD. An absent factory bean (Copilot disabled, or the api-platform facade
     * not on the classpath) resolves to an empty list — the same silent-skip degrade every other Copilot-domain
     * registration in this class already follows.
     *
     * <p>
     * All three are pinned rather than catalog-demoted — a departure from {@code listApiCollections}'s PRE-dissolution
     * placement, which lived in {@code aiHubAskGlobalToolCatalog} as a rarely-used read. Matching the
     * {@code task_agent} (Task 1) and MCP-server-CRUD (Task 3) precedents: the domain is tiny (three tools total, the
     * smallest of the eight delegates this plan dissolves), so splitting the one read tool into the catalog while its
     * two write siblings stay pinned on BUILD would fragment one coherent CRUD surface across two lookup mechanisms for
     * a marginal schema saving, and would force a {@code searchTool} round trip into a request the ASK agent should be
     * able to answer immediately ("what API collections do I have").
     * </p>
     *
     * <p>
     * No context wrapping is needed here (unlike the management MCP surface's {@code WorkspaceScopedFlatToolCallback}):
     * every tool call routed through {@link AiHubSpringAIAgent#toolContext} already carries
     * {@link com.bytechef.automation.ai.tool.AutomationToolInvocationContext}-compatible keys — the exact family
     * {@code listApiCollections} reads — for every registered pinned tool, not just API-collection ones.
     * {@code createApiCollection} never read that context at all (they resolve everything from an id already in their
     * own input), a pre-existing property of these tool classes unaffected by dissolving the delegate that used to wrap
     * them.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationApiCollectionFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> apiCollectionFlatCrudToolCallbacks(
        ObjectProvider<ApiCollectionToolCallbacksFactory> apiCollectionToolCallbacksFactoryProvider,
        boolean writable) {

        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory = apiCollectionToolCallbacksFactoryProvider
            .getIfAvailable();

        if (apiCollectionToolCallbacksFactory == null) {
            return List.of();
        }

        return writable
            ? apiCollectionToolCallbacksFactory.writeToolCallbacks()
            : apiCollectionToolCallbacksFactory.readToolCallbacks();
    }

    /**
     * The seven project-deployment CRUD tools flattened onto this surface (ticket 732, Task 3 of the CRUD-delegate
     * unwind), replacing the dissolved {@code project_deployment_agent} delegate: {@code listProjectDeployments} on
     * both agents, plus (write-only) {@code createProjectDeployment}, {@code updateProjectDeployment},
     * {@code deleteProjectDeployment}, {@code rollbackProjectDeployment}, {@code toggleProjectDeployment},
     * {@code promoteWorkflow} on BUILD — mirroring how every other flat domain in this class splits its read/write leg
     * between ASK and BUILD. An absent factory bean resolves to an empty list — the same silent-skip degrade every
     * other Copilot-domain registration in this class already follows; unlike
     * {@link #apiCollectionFlatCrudToolCallbacks}, {@link DeploymentToolCallbacksFactory} is registered whenever EITHER
     * {@code bytechef.ai.copilot.enabled} OR {@code bytechef.ai.hub.enabled} is true (see
     * {@code DeploymentAgentConfiguration}), so this domain does not carry that trade-off.
     *
     * <p>
     * All seven are pinned rather than catalog-demoted — the same departure from the plan's general "mutations go to
     * the searchable catalog" guidance that {@code task_agent} (Task 1) and MCP-server-CRUD (Task 3's earlier,
     * differently-scoped sibling plan) took: the domain is comparable in size to the seven-tool task precedent, ASK
     * gains its one read tool for the first time here (the dissolved delegate was BUILD-only, so there is no
     * pre-existing catalog placement to preserve either way), and splitting the six mutations into the catalog while
     * {@code listProjectDeployments} stays pinned would fragment one coherent CRUD surface across two lookup mechanisms
     * for a marginal schema saving — plus force a {@code searchTool} round trip into a request ASK should answer
     * immediately ("what deployments do I have").
     * </p>
     *
     * <p>
     * No context wrapping is needed here (unlike the management MCP surface's {@code WorkspaceScopedFlatToolCallback}):
     * every tool call routed through {@link AiHubSpringAIAgent#toolContext} already carries
     * {@link com.bytechef.automation.ai.tool.AutomationToolInvocationContext}-compatible keys — the exact family
     * {@code listProjectDeployments} reads — for every registered pinned tool, not just deployment ones. The six
     * mutations never read that context at all (they resolve everything from an id already in their own input), a
     * pre-existing property of these tool classes unaffected by dissolving the delegate that used to wrap them —
     * including a pre-existing lack of any workspace-ownership check at the tool or facade level, carried forward
     * unchanged rather than introduced by this flattening.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationDeploymentFlatCrudToolCallbacksTest}.
     * </p>
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

    /**
     * The seven asset-file CRUD tools flattened onto this surface (ticket 732, Task 4 of the CRUD-delegate unwind),
     * replacing the dissolved {@code asset_file_agent} delegate: {@code listAssetFiles} + {@code getAssetFileContent}
     * on both agents, plus (write-only) {@code createAssetFile}, {@code createBinaryAssetFile},
     * {@code updateAssetFileContent}, {@code cloneAssetFile}, {@code createAssetFileFromUrl} on BUILD — mirroring how
     * every other flat domain in this class splits its read/write leg between ASK and BUILD. An absent factory bean
     * (both Copilot AND AI Hub disabled — {@link AssetFileToolCallbacksFactory} is registered whenever either is, see
     * {@code AssetFileAgentConfiguration}) resolves to an empty list — the same silent-skip degrade every other
     * Copilot-domain registration in this class already follows.
     *
     * <p>
     * All seven are pinned rather than catalog-demoted — the same departure from the plan's general "mutations go to
     * the searchable catalog" guidance that {@code task_agent} (Task 1), {@code api_collection_agent} (Task 2), and
     * {@code project_deployment_agent} (Task 3) took: the domain is comparable in size to those precedents, the two
     * reads were ALREADY pinned before this task (the dissolved delegate only ever owned the five writes), and
     * splitting the five mutations into the catalog while the two reads stay pinned would fragment one coherent CRUD
     * surface across two lookup mechanisms for a marginal schema saving.
     * </p>
     *
     * <p>
     * No context wrapping is needed here (unlike the management MCP surface's {@code WorkspaceScopedFlatToolCallback}):
     * every tool call routed through {@link AiHubSpringAIAgent#toolContext} already carries
     * {@link com.bytechef.automation.ai.tool.AutomationToolInvocationContext}-compatible keys — the exact family every
     * one of these seven tools reads, including {@code sourceOrdinal} for the three create tools — for every registered
     * pinned tool, not just asset-file ones.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationAssetFileFlatCrudToolCallbacksTest}.
     * </p>
     */
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

    /**
     * The three read-only data-table tools ({@code listDataTables}, {@code queryDataTable}, {@code aggregateDataTable})
     * flattened onto BOTH ai_hub agents (ticket 732, Task 5 of the CRUD-delegate unwind), replacing the read leg of the
     * dissolved {@code data_table_agent} delegate. Unlike every sibling {@code *FlatCrudToolCallbacks} helper in this
     * class, there is no {@code writable} parameter — ASK and BUILD pin the exact same three reads; the eight mutations
     * are catalog-demoted instead of joining BUILD's pinned list (see {@link #dataTableCatalogToolCallbacks}), so there
     * is no larger "write" set to return here. An absent factory bean (both Copilot AND AI Hub disabled —
     * {@link DataTableToolCallbacksFactory} is registered whenever either is, see {@code DataTableAgentConfiguration})
     * resolves to an empty list.
     *
     * <p>
     * No context wrapping is needed here (unlike the management MCP surface's
     * {@code com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback}): every tool call routed through
     * {@link AiHubSpringAIAgent#toolContext} already carries {@code AgentToolInvocationContext}-compatible keys — the
     * exact family every one of these three tools reads, which is the OTHER family from every domain flattened onto
     * this class before this task (all of which read {@code AutomationToolInvocationContext}'s).
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationDataTableFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> dataTableFlatCrudToolCallbacks(
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider) {

        DataTableToolCallbacksFactory dataTableToolCallbacksFactory = dataTableToolCallbacksFactoryProvider
            .getIfAvailable();

        if (dataTableToolCallbacksFactory == null) {
            return List.of();
        }

        return dataTableToolCallbacksFactory.readToolCallbacks();
    }

    /**
     * The eight mutation data-table tools ({@code addDataTableRow}, {@code updateDataTableRow},
     * {@code deleteDataTableRow}, {@code addDataTableColumn}, {@code createDataTable}, {@code createDataTableFromCsv},
     * {@code cloneDataTable}, {@code dropDataTable}) registered on the BUILD agent's searchable tool catalog rather
     * than pinned (ticket 732, Task 5 of the CRUD-delegate unwind) — a departure from how the SMALLER flattened domains
     * in this class (task_agent's 7, api_collection_agent's 3, project_deployment_agent's 7, asset_file_agent's 7) were
     * all pinned in full. Data-table is the first of the larger domains (eleven tools total) and the plan's general
     * "reads pinned, mutations to the searchable catalog" guidance — the same treatment {@code createWorkflowChat}
     * already gets — applies here rather than being departed from: pinning all eleven would add real schema-token
     * weight to EVERY BUILD-mode model call for a domain most turns never touch. The three reads still pin (see
     * {@link #dataTableFlatCrudToolCallbacks}) since grounding queries ("what tables do I have", "what's in this
     * table") are far higher frequency than any single mutation. The prompt documents "find with searchTool first" for
     * each demoted name — see {@code prompt_ai_hub_build.txt}'s Data-table mutations section.
     *
     * <p>
     * Derives the mutation set as {@code writeToolCallbacks() minus readToolCallbacks()} by name rather than hand-
     * listing eight classes, so this list can never silently drift out of step with the factory's own read/write split.
     * </p>
     *
     * <p>
     * Catalog tools are security-context-rehydration-wrapped by {@code ToolSearchAdvisorConfiguration} and receive the
     * same {@link AiHubSpringAIAgent#toolContext} as every pinned tool once a {@code searchTool} hit surfaces them —
     * catalog membership does not change which context family a tool reads, so no extra wrapping is needed here either.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationDataTableFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> dataTableCatalogToolCallbacks(
        ObjectProvider<DataTableToolCallbacksFactory> dataTableToolCallbacksFactoryProvider) {

        DataTableToolCallbacksFactory dataTableToolCallbacksFactory = dataTableToolCallbacksFactoryProvider
            .getIfAvailable();

        if (dataTableToolCallbacksFactory == null) {
            return List.of();
        }

        Set<String> readNames = dataTableToolCallbacksFactory.readToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toSet());

        return dataTableToolCallbacksFactory.writeToolCallbacks()
            .stream()
            .filter(toolCallback -> !readNames.contains(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();
    }

    /**
     * The two read-only knowledge-base tools ({@code listKnowledgeBases}, {@code queryKnowledgeBase}) flattened onto
     * BOTH ai_hub agents (ticket 732, Task 6 of the CRUD-delegate unwind), replacing the read leg of the dissolved
     * {@code knowledge_base_agent} delegate. Like {@link #dataTableFlatCrudToolCallbacks} there is no {@code writable}
     * parameter — ASK and BUILD pin the exact same two reads; the five mutations are catalog-demoted instead of joining
     * BUILD's pinned list (see {@link #knowledgeBaseCatalogToolCallbacks}). An absent factory bean (both Copilot AND AI
     * Hub disabled, or the knowledge-base feature itself off — {@link KnowledgeBaseToolCallbacksFactory} is registered
     * only when {@code bytechef.ai.knowledge-base.enabled=true} AND either surface is on, see
     * {@code KnowledgeBaseAgentConfiguration}) resolves to an empty list.
     *
     * <p>
     * No context wrapping is needed here (unlike the management MCP surface's
     * {@code com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback}): every tool call routed through
     * {@link AiHubSpringAIAgent#toolContext} already carries {@code AgentToolInvocationContext}-compatible keys — the
     * same family the flat data-table tools read (see {@link #dataTableFlatCrudToolCallbacks}'s javadoc), and the
     * family every one of these two knowledge-base tools reads too.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationKnowledgeBaseFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> knowledgeBaseFlatCrudToolCallbacks(
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider) {

        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = knowledgeBaseToolCallbacksFactoryProvider
            .getIfAvailable();

        if (knowledgeBaseToolCallbacksFactory == null) {
            return List.of();
        }

        return knowledgeBaseToolCallbacksFactory.readToolCallbacks();
    }

    /**
     * The five mutation knowledge-base tools ({@code createKnowledgeBase}, {@code addKnowledgeBaseDocument},
     * {@code deleteKnowledgeBaseDocument}, {@code cloneKnowledgeBase}, {@code deleteKnowledgeBase}) registered on the
     * BUILD agent's searchable tool catalog rather than pinned (ticket 732, Task 6 of the CRUD-delegate unwind),
     * matching {@link #dataTableCatalogToolCallbacks}'s split rather than the smaller domains' precedent of pinning
     * everything — the task brief calls for the same reads-pinned/mutations-catalog treatment here. The two reads still
     * pin (see {@link #knowledgeBaseFlatCrudToolCallbacks}) since grounding queries ("what knowledge bases do I have",
     * "what does this KB contain") are far higher frequency than any single mutation. The prompt documents "find with
     * searchTool first" for each demoted name — see {@code prompt_ai_hub_build.txt}'s Knowledge-base mutations section.
     *
     * <p>
     * Derives the mutation set as {@code writeToolCallbacks() minus readToolCallbacks()} by name rather than hand-
     * listing five classes, so this list can never silently drift out of step with the factory's own read/write split —
     * mirrors {@link #dataTableCatalogToolCallbacks}.
     * </p>
     *
     * <p>
     * Catalog tools are security-context-rehydration-wrapped by {@code ToolSearchAdvisorConfiguration} and receive the
     * same {@link AiHubSpringAIAgent#toolContext} as every pinned tool once a {@code searchTool} hit surfaces them —
     * catalog membership does not change which context family a tool reads, so no extra wrapping is needed here either.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationKnowledgeBaseFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> knowledgeBaseCatalogToolCallbacks(
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider) {

        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = knowledgeBaseToolCallbacksFactoryProvider
            .getIfAvailable();

        if (knowledgeBaseToolCallbacksFactory == null) {
            return List.of();
        }

        Set<String> readNames = knowledgeBaseToolCallbacksFactory.readToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toSet());

        return knowledgeBaseToolCallbacksFactory.writeToolCallbacks()
            .stream()
            .filter(toolCallback -> !readNames.contains(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();
    }

    /**
     * The six read-only context-store tools ({@code listContextSources}, {@code searchContextStore},
     * {@code getContextStoreRecord}, {@code listAvailableSourceComponents}, {@code describeSourceComponentEntities},
     * {@code semanticSearchContextStore} — the last one only when the semantic-search service is on the classpath)
     * flattened onto BOTH ai_hub agents (ticket 732, Task 7 of the CRUD-delegate unwind), replacing the read leg of the
     * dissolved {@code context_store_agent} delegate. Like {@link #dataTableFlatCrudToolCallbacks} and
     * {@link #knowledgeBaseFlatCrudToolCallbacks} there is no {@code writable} parameter — ASK and BUILD pin the exact
     * same reads; the six mutations are catalog-demoted instead of joining BUILD's pinned list (see
     * {@link #contextStoreCatalogToolCallbacks}). An absent factory bean (both Copilot AND AI Hub disabled, or the
     * context-store feature itself off — {@link ContextStoreToolCallbacksFactory} is registered only when
     * {@code bytechef.context-store.enabled=true} AND either surface is on, see {@code ContextStoreAgentConfiguration})
     * resolves to an empty list.
     *
     * <p>
     * No context wrapping is needed here (unlike the management MCP surface's
     * {@code com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback}): every tool call routed through
     * {@link AiHubSpringAIAgent#toolContext} already carries {@code AgentToolInvocationContext}-compatible keys — the
     * same family the flat data-table and knowledge-base tools read (see {@link #dataTableFlatCrudToolCallbacks}'s
     * javadoc), and the family every one of these context-store reads that looks at workspace scope reads too. Four of
     * the six write-side siblings (see {@link #contextStoreCatalogToolCallbacks}) instead resolve their owning
     * workspace by looking up the entity id in their own input, reading no tool-context family at all — a pre-existing
     * property of those tool classes unaffected by dissolving the delegate that used to wrap them.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationContextStoreFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> contextStoreFlatCrudToolCallbacks(
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider) {

        ContextStoreToolCallbacksFactory contextStoreToolCallbacksFactory = contextStoreToolCallbacksFactoryProvider
            .getIfAvailable();

        if (contextStoreToolCallbacksFactory == null) {
            return List.of();
        }

        return contextStoreToolCallbacksFactory.readToolCallbacks();
    }

    /**
     * The six mutation context-store tools ({@code createContextStoreSource}, {@code updateContextStoreSource},
     * {@code deleteContextStoreSource}, {@code refreshContextStoreSource}, {@code setContextStoreSourceEnabled},
     * {@code deleteContextStore}) registered on the BUILD agent's searchable tool catalog rather than pinned (ticket
     * 732, Task 7 of the CRUD-delegate unwind) — the largest tool count of any delegate this plan dissolves (twelve
     * total), so it gets the same reads-pinned/mutations-catalog treatment {@link #dataTableCatalogToolCallbacks} and
     * {@link #knowledgeBaseCatalogToolCallbacks} already established for the other large domains, rather than the
     * smaller domains' precedent of pinning everything. The six reads still pin (see
     * {@link #contextStoreFlatCrudToolCallbacks}) since grounding queries ("what context stores/sources do I have",
     * "what's in this source") are far higher frequency than any single mutation. The prompt documents "find with
     * searchTool first" for each demoted name — see {@code prompt_ai_hub_build.txt}'s Context-store mutations section.
     *
     * <p>
     * Derives the mutation set as {@code writeToolCallbacks() minus readToolCallbacks()} by name rather than hand-
     * listing six classes, so this list can never silently drift out of step with the factory's own read/write split —
     * mirrors {@link #dataTableCatalogToolCallbacks} and {@link #knowledgeBaseCatalogToolCallbacks}.
     * </p>
     *
     * <p>
     * Catalog tools are security-context-rehydration-wrapped by {@code ToolSearchAdvisorConfiguration} and receive the
     * same {@link AiHubSpringAIAgent#toolContext} as every pinned tool once a {@code searchTool} hit surfaces them —
     * catalog membership does not change which context family a tool reads, so no extra wrapping is needed here either.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationContextStoreFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> contextStoreCatalogToolCallbacks(
        ObjectProvider<ContextStoreToolCallbacksFactory> contextStoreToolCallbacksFactoryProvider) {

        ContextStoreToolCallbacksFactory contextStoreToolCallbacksFactory = contextStoreToolCallbacksFactoryProvider
            .getIfAvailable();

        if (contextStoreToolCallbacksFactory == null) {
            return List.of();
        }

        Set<String> readNames = contextStoreToolCallbacksFactory.readToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toSet());

        return contextStoreToolCallbacksFactory.writeToolCallbacks()
            .stream()
            .filter(toolCallback -> !readNames.contains(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();
    }

    /**
     * The two read-only AI-Agent-builder tools ({@code listAiAgents}, {@code getAiAgent}) flattened onto BOTH ai_hub
     * agents (ticket 732, Task 8 of the CRUD-delegate unwind — the LAST delegate in the plan), replacing the read leg
     * of the dissolved {@code ai_agent_agent} delegate. Like {@link #dataTableFlatCrudToolCallbacks},
     * {@link #knowledgeBaseFlatCrudToolCallbacks}, and {@link #contextStoreFlatCrudToolCallbacks} there is no
     * {@code writable} parameter — ASK and BUILD pin the exact same two reads; the nine mutations are catalog-demoted
     * instead of joining BUILD's pinned list (see {@link #aiAgentCatalogToolCallbacks}). An absent factory bean (both
     * Copilot AND AI Hub disabled — {@link AiAgentToolCallbacksFactory} is registered whenever either is, see
     * {@code AiAgentAgentConfiguration}) resolves to an empty list.
     *
     * <p>
     * No context wrapping is needed here (unlike the management MCP surface's
     * {@code com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback}): every tool call routed through
     * {@link AiHubSpringAIAgent#toolContext} already carries {@code AgentToolInvocationContext}-compatible keys — the
     * same family every AI-Agent-builder tool reads (an {@code AiAgent} is an automation entity managed through the CE
     * {@code AiAgentFacade}, not an AI-hub-owned one, so this mirrors data-table/knowledge-base/context-store rather
     * than the {@code AutomationToolInvocationContext} family the earlier, smaller delegates in this plan read). Of
     * these two reads, only {@code listAiAgents} actually looks at workspace scope; {@code getAiAgent} resolves
     * everything from the {@code id} already in its own input.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationAiAgentFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> aiAgentFlatCrudToolCallbacks(
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider) {

        AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory = aiAgentToolCallbacksFactoryProvider
            .getIfAvailable();

        if (aiAgentToolCallbacksFactory == null) {
            return List.of();
        }

        return aiAgentToolCallbacksFactory.readToolCallbacks();
    }

    /**
     * The nine mutation AI-Agent-builder tools ({@code createAiAgent}, {@code updateAiAgent},
     * {@code addAiAgentChannel}, {@code deleteAiAgentChannel}, {@code addAiAgentElement}, {@code updateAiAgentElement},
     * {@code deleteAiAgentElement}, {@code updateAiAgentSettings}, {@code publishAiAgent}) registered on the BUILD
     * agent's searchable tool catalog rather than pinned (ticket 732, Task 8 of the CRUD-delegate unwind — the LAST
     * delegate) — the second-largest mutation set of any domain this plan dissolves (nine, versus context-store's six
     * and data-table's eight), so it gets the same reads-pinned/mutations-catalog treatment
     * {@link #dataTableCatalogToolCallbacks}, {@link #knowledgeBaseCatalogToolCallbacks}, and
     * {@link #contextStoreCatalogToolCallbacks} already established for the other large domains, rather than the
     * smaller domains' precedent of pinning everything. The two reads still pin (see
     * {@link #aiAgentFlatCrudToolCallbacks}) since grounding queries ("what agents do I have", "what does this agent
     * look like") are far higher frequency than any single mutation. The prompt documents "find with searchTool first"
     * for each demoted name — see {@code prompt_ai_hub_build.txt}'s AI-Agent-builder mutations section.
     *
     * <p>
     * Derives the mutation set as {@code writeToolCallbacks() minus readToolCallbacks()} by name rather than hand-
     * listing nine classes, so this list can never silently drift out of step with the factory's own read/write split —
     * mirrors {@link #dataTableCatalogToolCallbacks}, {@link #knowledgeBaseCatalogToolCallbacks}, and
     * {@link #contextStoreCatalogToolCallbacks}.
     * </p>
     *
     * <p>
     * Of the nine, only {@code createAiAgent} reads workspace scope from the tool context; the other eight resolve
     * their owning workspace by looking up the entity id already in their own input, reading no tool-context family at
     * all — a pre-existing property of those tool classes unaffected by dissolving the delegate that used to wrap them.
     * </p>
     *
     * <p>
     * Catalog tools are security-context-rehydration-wrapped by {@code ToolSearchAdvisorConfiguration} and receive the
     * same {@link AiHubSpringAIAgent#toolContext} as every pinned tool once a {@code searchTool} hit surfaces them —
     * catalog membership does not change which context family a tool reads, so no extra wrapping is needed here either.
     * </p>
     *
     * <p>
     * Package-private for {@code AiHubConfigurationAiAgentFlatCrudToolCallbacksTest}.
     * </p>
     */
    static List<ToolCallback> aiAgentCatalogToolCallbacks(
        ObjectProvider<AiAgentToolCallbacksFactory> aiAgentToolCallbacksFactoryProvider) {

        AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory = aiAgentToolCallbacksFactoryProvider
            .getIfAvailable();

        if (aiAgentToolCallbacksFactory == null) {
            return List.of();
        }

        Set<String> readNames = aiAgentToolCallbacksFactory.readToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toSet());

        return aiAgentToolCallbacksFactory.writeToolCallbacks()
            .stream()
            .filter(toolCallback -> !readNames.contains(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();
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
