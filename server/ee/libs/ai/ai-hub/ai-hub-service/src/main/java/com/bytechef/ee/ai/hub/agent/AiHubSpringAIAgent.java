/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.Role;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.agent.tool.AgentTypeRegistry;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.ee.ai.hub.progress.SubagentProgressEmitter;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.ai.hub.util.Source;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.advisor.AiGuardrailsAdvisor;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.ee.platform.ai.workspaceprompt.advisor.WorkspaceSystemPromptAdvisor;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * AI Hub LLM agent. Extends {@link SpringAIAgent} with AI Hub-specific behavior: propagates workspace/user/environment
 * context from the run state and injects the client's currently-open tabs and active file id into the system message so
 * the LLM can reason about "the file the user is viewing".
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubSpringAIAgent extends SpringAIAgent {

    private static final Logger log = LoggerFactory.getLogger(AiHubSpringAIAgent.class);

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - The assistant must not produce visual representations of any kind, including diagrams, charts, UI sketches, images, or pseudo-visuals.
            - In BUILD mode, when the user asks for a file (spec, runbook, CSV, JSON, markdown note, code file), produce the content and call createAssetFile to save it. In ASK mode, file creation is unavailable — suggest switching to BUILD mode instead.
            - After creating or referencing a file, always call openResourceTab({type: "FILE", fileId, name}) so the user sees it in the right-hand resource panel.
            - Before referring to existing files, call listAssetFiles to discover what is available.
            - In BUILD mode, when editing an existing file, call getAssetFileContent first, then call updateAssetFileContent with the new content.
            """;

    /**
     * Resolves the memory index (list of memory summaries) for the current turn. Implementations are expected to return
     * {@code null} or an empty list when no memories exist. Injected for BUILD mode only; ASK mode constructs the agent
     * with this field left {@code null}.
     */
    public interface MemoryIndexResolver {

        /**
         * Returns a human-readable, newline-separated index block for the given workspace + user + environment, or
         * {@code null}/blank when the block should be omitted. The environment scopes the resolved memories so a
         * BUILD-mode turn in PRODUCTION sees only PROD memories — preventing dev-only preferences from leaking into
         * production sessions.
         *
         * @param workspaceId the current workspace id (non-null)
         * @param userId      the owning user id for the current AG-UI thread (non-null)
         * @param environment the environment ordinal (DEVELOPMENT=0, STAGING=1, PRODUCTION=2)
         * @return the rendered index block or {@code null}/blank to omit the Context entry
         */
        String resolve(long workspaceId, long userId, int environment);
    }

    /**
     * Per-request resolver of {@link ToolCallback}s synthesized from the chat's attached tools. Hooked into the
     * underlying {@link SpringAIAgent#additionalToolCallbacks(RunAgentInput)} so the LLM sees union(static,
     * chat-attached) for the turn. Implementations look up bindings via
     * {@code AiHubChatToolFacade.listChatTools(chatId)} and convert each to a {@code ClusterElementToolCallback}.
     */
    public interface ChatToolBindingResolver {

        /**
         * Returns per-chat tool callbacks for the current request. Implementations are expected to tolerate any input
         * where the resolver can't determine the chat (no thread id, chat not found, DB outage) by returning an empty
         * list — the agent must still respond from its static tool set in that case.
         */
        List<ToolCallback> resolve(AiHubToolInvocationContext invocationContext);
    }

    /**
     * Per-request resolver for an override {@link ChatClient}. Used to swap the LLM at runtime — e.g. when a chat
     * template has its own model override set, the routing agent puts the (provider, model) pair into state and this
     * resolver returns a ChatClient built against the agent-specific model.
     *
     * <p>
     * Returning {@code null} means "no override — use the agent's builder-time default ChatClient." Implementations are
     * expected to tolerate any input where they can't resolve the override (state keys missing, provider not found,
     * service bean absent) by returning {@code null} so the agent falls back to the default cleanly.
     */
    @FunctionalInterface
    public interface OverrideChatClientResolver {

        @Nullable
        ChatClient resolve(State state);
    }

    private final MemoryIndexResolver memoryIndexResolver;
    private final Function<String, Long> threadUserIdResolver;
    private final ChatToolBindingResolver chatToolBindingResolver;
    private final @Nullable OverrideChatClientResolver overrideChatClientResolver;
    private final @Nullable SecurityContextRehydrator securityContextRehydrator;
    private final @Nullable AiGuardrails aiGuardrails;
    private final @Nullable AiGuardrailMetrics aiGuardrailMetrics;
    private final @Nullable WorkspaceSystemPrompts workspaceSystemPrompts;

    protected AiHubSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder);

        this.memoryIndexResolver = builder.memoryIndexResolver;
        this.threadUserIdResolver = builder.threadUserIdResolver;
        this.chatToolBindingResolver = builder.chatToolBindingResolver;
        this.overrideChatClientResolver = builder.overrideChatClientResolver;
        this.securityContextRehydrator = builder.securityContextRehydrator;
        this.aiGuardrails = builder.aiGuardrails;
        this.aiGuardrailMetrics = builder.aiGuardrailMetrics;
        this.workspaceSystemPrompts = builder.workspaceSystemPrompts;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        AiHubAgentTenantBinder.runWithTenant(
            getAgentId(), input.state(), () -> runWithEnvironment(input, subscriber));
    }

    /**
     * Binds the verified environment for the whole turn so the fallback default ChatModel (the catalog-backed
     * {@code @Primary CatalogChatModel}, resolved per environment from {@link EnvironmentContext}) picks the right
     * environment's API key. The override resolver passes the environment explicitly and so does not depend on this.
     *
     * <p>
     * Restores the thread's previous environment rather than clearing it, matching the tenant binding this nests
     * inside: the common pool is shared, so a worker that arrives already bound must leave as it arrived.
     */
    private void runWithEnvironment(RunAgentInput input, AgentSubscriber subscriber) {
        Integer environmentOrdinal = environmentOrdinal(input);

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        if (environmentOrdinal != null) {
            EnvironmentContext.set(environmentOrdinal);
        }

        try {
            CurrentAgentContext.runWith(
                AgentTypeRegistry.fromKey(getAgentId()), null,
                () -> SubagentProgressEmitter.runWithSubscriber(subscriber, () -> super.run(input, subscriber)));
        } finally {
            if (environmentOrdinal != null) {
                if (previousEnvironment == null) {
                    EnvironmentContext.clear();
                } else {
                    EnvironmentContext.set(previousEnvironment);
                }
            }
        }
    }

    private static @Nullable Integer environmentOrdinal(RunAgentInput input) {
        State state = input.state();

        Long environmentId =
            state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID));

        if (environmentId == null || environmentId < 0 || environmentId >= Environment.values().length) {
            return null;
        }

        return environmentId.intValue();
    }

    /**
     * Publishes the environment id into the advisor request context so embedding-bearing advisors can rebind the right
     * {@link EnvironmentContext} on their own {@code Schedulers.boundedElastic()} threads. The base agent's
     * {@code .contextCapture()} covers chat-model resolution, but the tool-search advisor embeds (session indexing and
     * {@code searchTool} query embedding) on an inner scheduler hop the outer capture does not reach — leaving the
     * embedding thread on the {@link Environment#PRODUCTION} fallback, which fails when only a non-prod provider is
     * activated. {@link com.bytechef.ee.ai.hub.toolsearch.PinnedToolSearchToolCallingAdvisor} reads this param and
     * rebinds the environment there (mirrors the copilot {@code WorkflowEditorSpringAIAgent} +
     * {@code EnvironmentAwareQuestionAnswerAdvisor} pattern).
     */
    @Override
    protected Map<String, Object> advisorParams(RunAgentInput input) {
        Map<String, Object> advisorParams = new HashMap<>();

        // The session memory advisor resolves its session from SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, whose
        // literal equals ChatMemory.CONVERSATION_ID. The parent agent only sets this param on its own chatMemory
        // branch (not taken — the AI Hub mounts SessionMemoryAdvisor directly), so publish the thread id here.
        String threadId = input.threadId();

        if (threadId != null && !threadId.isBlank()) {
            advisorParams.put(ChatMemory.CONVERSATION_ID, threadId);
        }

        State state = input.state();

        Long environmentId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.ENVIRONMENT_ID));

        if (environmentId != null && environmentId >= 0 && environmentId < Environment.values().length) {
            advisorParams.put(AiHubStateKeys.ENVIRONMENT_ID, environmentId);
        }

        // Publish the controller-verified workspace + user ids so AiHubModelUsageAdvisor can attribute each turn's
        // token usage to the right workspace in the ai_llm_usage metering store.
        Long workspaceId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID));

        if (workspaceId != null && workspaceId > 0) {
            advisorParams.put(AiHubStateKeys.VERIFIED_WORKSPACE_ID, workspaceId);
        }

        Long userId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.AUTHENTICATED_USER_ID));

        if (userId != null && userId > 0) {
            advisorParams.put(AiHubStateKeys.AUTHENTICATED_USER_ID, userId);
        }

        return advisorParams;
    }

    /**
     * The {@code llmProvider}/{@code llmModel} keys populated here MUST stay consistent with
     * {@link AiHubChatClientResolver}, this surface's {@code OverrideChatClientResolver} — a mismatch would run a
     * delegate subagent on a different model than the one its caller resolved for this turn.
     */
    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        AiHubToolInvocationContext aiHubContext = buildInvocationContext(input);

        Map<String, Object> toolContext = new HashMap<>(aiHubContext.toToolContext());

        String tenantId = input.state() == null
            ? null
            : asString(input.state()
                .get(AiHubStateKeys.VERIFIED_TENANT_ID));

        SelectedLlm selectedLlm = resolveSelectedLlm(input.state());

        toolContext.putAll(
            AgentToolInvocationContext.builder()
                .workspaceId(aiHubContext.workspaceId())
                .userId(aiHubContext.userId())
                .environmentId(aiHubContext.environmentId())
                .conversationId(aiHubContext.threadId())
                .tenantId(tenantId)
                .llmProvider(selectedLlm == null ? null : selectedLlm.provider())
                .llmModel(selectedLlm == null ? null : selectedLlm.model())
                .build()
                .toToolContext());

        return toolContext;
    }

    /**
     * The (provider, model) pair a subagent delegate should be handed. Precedence mirrors
     * {@link AiHubChatClientResolver#resolve(State)} exactly: the user's per-conversation selection
     * ({@link AiHubStateKeys#USER_SELECTED_LLM_PROVIDER_KEY} / {@link AiHubStateKeys#USER_SELECTED_LLM_MODEL_KEY}). A
     * half-set pair (only one of provider/model present) is a transient client artifact, not malicious input, so it
     * yields null after a single warning log rather than failing the turn — the delegate then inherits the workspace
     * default, exactly as the resolver's own fallback does.
     */
    static @Nullable SelectedLlm resolveSelectedLlm(@Nullable State state) {
        if (state == null) {
            return null;
        }

        String llmProvider = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY));
        String llmModel = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY));

        if ((llmProvider == null) != (llmModel == null)) {
            log.warn(
                "User-selected LLM half-set (provider={}, model={}); falling back to the workspace default",
                llmProvider, llmModel);
        }

        if (llmProvider == null || llmModel == null) {
            return null;
        }

        return new SelectedLlm(llmProvider, llmModel);
    }

    /**
     * The (provider, model) pair resolved by {@link #resolveSelectedLlm(State)}, carried into the tool context for
     * subagent delegates.
     */
    record SelectedLlm(String provider, String model) {
    }

    /**
     * Returns the per-request {@link ChatClient}. Tries the override resolver first (used for the user's
     * per-conversation model selection); falls back to the builder-time default whenever the resolver is absent,
     * returns null, or throws. Either way, the returned client always has the workspace's {@link AiGuardrailsAdvisor}
     * attached — see {@link #attachGuardrailsAdvisor}.
     *
     * <p>
     * This single method is the seam every AI Hub LLM turn passes through before the request spec is built (the
     * vendored {@code SpringAIAgent.getChatRequest} calls {@code resolveChatClient(input).prompt(...)} first thing), so
     * attaching the guardrail here covers every conversation kind that reaches the model: COPILOT (default client),
     * TASK including its model-override clients (override branch above), and any future kind routed through this same
     * {@code run()}. WORKFLOW_CHAT is exempt by construction, not by a check here — it never calls this agent at all,
     * it is dispatched through {@code WebhookBridgeAgent} instead.
     * </p>
     *
     * <p>
     * Subagent one-shot delegate calls do NOT go through this method — each specialist owns its own {@link ChatClient},
     * constructed once in {@code AiHubConfiguration} and invoked directly by its hand-rolled {@code ToolCallback}
     * ({@code SkillsAgentToolCallback}, {@code ProjectWorkflowAgentToolCallback}, {@code ResearchToolCallback}, etc.),
     * never through {@code resolveChatClient}. That gap is closed separately: every delegate {@link ChatClient} handed
     * to a {@code ToolCallback} constructor in {@code AiHubConfiguration} is wrapped with
     * {@code com.bytechef.ee.ai.hub.guardrails.SubAgentGuardrailedChatClient#wrap}, which attaches a fresh,
     * workspace-scoped {@link AiGuardrailsAdvisor} to the delegate's own {@code .call()}/{@code .stream()} — the
     * workspace id is resolved per call from the {@code ToolContext} the delegate forwards via
     * {@code .toolContext(Map)}, since the delegate {@link ChatClient} bean is a singleton shared by every workspace
     * and cannot have the id baked in at construction time the way this method's builder-time default can. See that
     * class's javadoc for the full mechanism, and the AI Guardrails spec's decisions log for what remains uncovered
     * even after that fix (a delegate's completion still reaches the PARENT as an unscanned tool message).
     * </p>
     */
    @Override
    protected ChatClient resolveChatClient(RunAgentInput input) {
        ChatClient chatClient = attachGuardrailsAdvisor(resolveConfiguredChatClient(input), input);

        return attachWorkspaceSystemPromptAdvisor(chatClient, input);
    }

    private ChatClient resolveConfiguredChatClient(RunAgentInput input) {
        if (overrideChatClientResolver == null) {
            return super.resolveChatClient(input);
        }

        try {
            ChatClient override = overrideChatClientResolver.resolve(input.state());

            if (override != null) {
                return override;
            }
        } catch (RuntimeException exception) {
            // The resolver is best-effort; any failure (missing provider row, factory throw, malformed state) falls
            // back to the workspace default rather than failing the turn. The override path is opt-in and absence
            // simply means "use the configured default."
            log.warn(
                "AiHubSpringAIAgent: override ChatClient resolver threw; falling back to default. {}",
                exception.getMessage());
        }

        return super.resolveChatClient(input);
    }

    /**
     * Registers a fresh, workspace-bound {@link AiGuardrailsAdvisor} as a default advisor on {@code chatClient} so it
     * runs on this turn's outbound request and inbound completion (self-orders at
     * {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} regardless of where in the advisor list it lands —
     * see {@code DefaultAroundAdvisorChain}, which sorts the merged default + request advisors before dispatch).
     * Constructing the advisor per request is cheap (it holds only field references); the workspace id can only be
     * known per request, so the advisor cannot be attached once at builder time the way the other static advisors are.
     * A missing {@link AiGuardrails} bean (guardrails module absent) or an inactive policy for this workspace (every
     * guardrail disabled, the common case) both skip the {@code mutate()} call entirely so the no-op case pays no
     * per-turn overhead.
     *
     * <p>
     * <b>Block-mode UX:</b> a BLOCK-mode violation makes the advisor raise {@code AiGuardrailViolationException} whose
     * message carries only the violation category, never the offending content (see the exception's own javadoc). On
     * the streaming path (the only path this agent uses — see {@code SpringAIAgent.run}'s {@code .stream()} call) the
     * advisor reports this as {@code Flux.error(...)}, which reaches this class's inherited {@code run()} through the
     * ordinary model-call-failure branch it already has: the stream's {@code err} consumer logs and calls
     * {@code onError(input, err.getMessage(), subscriber)}, emitting a {@code RunErrorEvent} instead of any tool
     * response. The AI Hub client renders {@code RunErrorEvent} as an inline chat notice (red banner, alert icon — see
     * {@code RunErrorMessage.tsx}), through {@code humanizeAgentErrorMessage} which passes the already-safe,
     * category-only guardrail message through unchanged. No raw exception class name, HTTP status, or offending content
     * ever reaches the transcript — the same path an upstream model-provider error (e.g. Anthropic 400) takes today, so
     * no separate exception-to-chat-notice mapping is needed here.
     * </p>
     */
    private ChatClient attachGuardrailsAdvisor(ChatClient chatClient, RunAgentInput input) {
        if (aiGuardrails == null || aiGuardrailMetrics == null) {
            return chatClient;
        }

        State state = input.state();
        Long workspaceId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID));

        if (!aiGuardrails.isActive(workspaceId)) {
            return chatClient;
        }

        AiGuardrailsAdvisor advisor = new AiGuardrailsAdvisor(aiGuardrails, workspaceId, aiGuardrailMetrics);

        return chatClient.mutate()
            .defaultAdvisors(advisor)
            .build();
    }

    /**
     * Registers a fresh, workspace-bound {@link WorkspaceSystemPromptAdvisor} so the admin's standing instructions are
     * appended to this turn's system message. Self-orders AFTER the guardrails advisor (HIGHEST_PRECEDENCE + 100), so
     * the admin text is never redacted or blocked by the workspace's own guardrail policy. A missing engine bean
     * (module absent) or a workspace without a prompt (the common case — {@code fetchPrompt} is memoized) skips the
     * {@code mutate()} entirely so the no-op case pays no per-turn overhead.
     */
    private ChatClient attachWorkspaceSystemPromptAdvisor(ChatClient chatClient, RunAgentInput input) {
        if (workspaceSystemPrompts == null) {
            return chatClient;
        }

        State state = input.state();
        Long workspaceId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID));

        if (workspaceSystemPrompts.fetchPrompt(workspaceId) == null) {
            return chatClient;
        }

        return chatClient.mutate()
            .defaultAdvisors(new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, workspaceId))
            .build();
    }

    @Override
    protected List<ToolCallback> additionalToolCallbacks(RunAgentInput input) {
        if (chatToolBindingResolver == null) {
            return List.of();
        }

        AiHubToolInvocationContext invocationContext = buildInvocationContext(input);

        // Best-effort: a broken resolver row, DB outage, or NPE in the lookup must not abort the whole turn
        // before any SSE events flow back. Mirror the memory-index-context guard in createSystemMessage.
        try {
            // Mirror the static-callback wrapping done in the Builder so per-request bindings also get
            // (1) tenant + SecurityContext rehydration (so @PreAuthorize-protected facade calls work on
            // Reactor scheduler threads — see RehydrateContextToolCallback) and (2) empty-return defense
            // (so Anthropic doesn't reject the turn — see NonEmptyToolCallback).
            return chatToolBindingResolver.resolve(invocationContext)
                .stream()
                .map(this::wrapToolCallback)
                .toList();
        } catch (RuntimeException exception) {
            log.warn(
                "AiHubChat tool binding resolution failed for thread={}, workspace={}; continuing with static "
                    + "callbacks only",
                invocationContext.threadId(), invocationContext.workspaceId(), exception);

            return List.of();
        }
    }

    /**
     * Applies the two ai-hub wrappers in the canonical order: context rehydration (tenant + SecurityContext) OUTERMOST
     * so the context is set before any inner code (including the empty-return guard and the actual callback) runs. Used
     * by both the per-request {@link #additionalToolCallbacks} path and the static-builder path.
     */
    ToolCallback wrapToolCallback(ToolCallback callback) {
        return AiHubToolCallbackWrappers.wrap(callback, securityContextRehydrator);
    }

    AiHubToolInvocationContext buildInvocationContext(RunAgentInput input) {
        State state = input.state();

        // workspaceId and userId come from the controller-verified keys, not from raw request fields. The controller
        // checks workspace membership and chat ownership BEFORE this method runs and rewrites the verified
        // values into reserved keys; tool callbacks then operate against authenticated-session data, not user-
        // controlled request body.
        Long workspaceId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID));
        Long userId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.AUTHENTICATED_USER_ID));
        Long rawEnvironmentId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.ENVIRONMENT_ID));
        long environmentId = rawEnvironmentId != null ? rawEnvironmentId : 0L;
        Short sourceOrdinal = Source.AI_HUB.toAgentSourceOrdinal();
        String lastUserPrompt = lastUserPrompt(input.messages());
        String threadId = state == null ? null : asString(state.get(AiHubStateKeys.VERIFIED_THREAD_ID));

        return new AiHubToolInvocationContext(workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId,
            threadId);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Object activeFileId = state == null ? null : state.get("activeFileId");

        if (activeFileId != null) {
            contexts.add(new Context("Active File", String.valueOf(activeFileId)));
        }

        Object currentTabs = state == null ? null : state.get("currentTabs");

        if (currentTabs instanceof List<?> tabs && !tabs.isEmpty()) {
            contexts.add(new Context("Open Tabs", formatTabs(tabs)));
        }

        Object activeTab = state == null ? null : state.get("activeTab");

        if (activeTab instanceof Map<?, ?> activeTabMap) {
            contexts.add(new Context("Active Tab", formatActiveTab(activeTabMap)));
        }

        Object referencedResources = state == null ? null : state.get("referencedResources");

        if (referencedResources instanceof List<?> resources && !resources.isEmpty()) {
            contexts.add(new Context("Referenced Resources", formatReferencedResources(resources)));
        }

        appendMemoryIndexContext(state, contexts);

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    private void appendMemoryIndexContext(State state, List<Context> contexts) {
        if (memoryIndexResolver == null) {
            return;
        }

        Long workspaceId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID));

        if (workspaceId == null) {
            return;
        }

        // Prefer the controller-verified userId; fall back to threadId-based resolution only as belt-and-braces
        // (the controller already enforces ownership, so the verified userId should always be present).
        Long userId = NumberUtils.asLong(state.get(AiHubStateKeys.AUTHENTICATED_USER_ID));

        if (userId == null) {
            String threadId = asString(state.get(AiHubStateKeys.VERIFIED_THREAD_ID));

            userId = threadUserIdResolver != null && threadId != null ? threadUserIdResolver.apply(threadId) : null;
        }

        if (userId == null) {
            return;
        }

        // Match how buildInvocationContext reads environmentId so the memory index sees the same env partition the
        // tool callbacks will write into for this turn.
        Long rawEnvironmentId = NumberUtils.asLong(state.get(AiHubStateKeys.ENVIRONMENT_ID));
        int environment = rawEnvironmentId != null ? rawEnvironmentId.intValue() : 0;

        // Memory index is best-effort context enrichment. A broken row, DB outage, or NPE in the resolver must not
        // abort the entire agent run before any SSE events flow back — without this guard a single bad memory row
        // would prevent the user from chatting at all. Log at WARN with workspace+user so ops can correlate.
        String index;

        try {
            index = memoryIndexResolver.resolve(workspaceId, userId, environment);
        } catch (RuntimeException exception) {
            log.warn(
                "Memory index resolution failed for workspaceId={}, userId={}, environment={}; continuing without memory context",
                workspaceId, userId, environment, exception);

            return;
        }

        if (index == null || index.isBlank()) {
            return;
        }

        contexts.add(new Context("Memory Index", index));
    }

    private static String formatTabs(List<?> tabs) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Object tab : tabs) {
            if (tab instanceof Map<?, ?> tabMap) {
                stringBuilder.append("- fileId=")
                    .append(tabMap.get("fileId"))
                    .append(", name=")
                    .append(tabMap.get("name"))
                    .append(", viewMode=")
                    .append(tabMap.get("viewMode"))
                    .append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private static String formatActiveTab(Map<?, ?> activeTabMap) {
        return "kind=" + activeTabMap.get("kind") + ", id=" + activeTabMap.get("id") + ", name="
            + activeTabMap.get("name");
    }

    private static String formatReferencedResources(List<?> resources) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Object resource : resources) {
            if (resource instanceof Map<?, ?> resourceMap) {
                stringBuilder.append("- kind=")
                    .append(resourceMap.get("kind"))
                    .append(", id=")
                    .append(resourceMap.get("id"))
                    .append(", name=")
                    .append(resourceMap.get("name"))
                    .append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private static String lastUserPrompt(List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            BaseMessage message = messages.get(i);

            if (message instanceof UserMessage userMessage && Role.user.equals(userMessage.getRole())) {
                return userMessage.getContent();
            }
        }

        return null;
    }

    public static class Builder extends SpringAIAgent.Builder {

        private MemoryIndexResolver memoryIndexResolver;
        private Function<String, Long> threadUserIdResolver;
        private ChatToolBindingResolver chatToolBindingResolver;
        private @Nullable OverrideChatClientResolver overrideChatClientResolver;
        private @Nullable SecurityContextRehydrator securityContextRehydrator;
        private @Nullable LlmUsageRecorder llmUsageRecorder;
        private @Nullable AiGuardrails aiGuardrails;
        private @Nullable AiGuardrailMetrics aiGuardrailMetrics;
        private @Nullable WorkspaceSystemPrompts workspaceSystemPrompts;
        // Captured from agentId() so the usage advisor can tag ai_llm_usage rows with the agent that served the turn.
        private @Nullable String usageAgentName;
        // Holds the unwrapped tool callbacks the caller registers via toolCallbacks/toolCallback. Deferred
        // wrapping in build() lets us apply the context rehydration wrapper and the empty-return guard
        // regardless of the order the caller supplies securityContextRehydrator / callbacks.
        private final List<ToolCallback> pendingToolCallbacks = new ArrayList<>();

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder securityContextRehydrator(@Nullable SecurityContextRehydrator securityContextRehydrator) {
            this.securityContextRehydrator = securityContextRehydrator;

            return this;
        }

        public Builder memoryIndexResolver(MemoryIndexResolver memoryIndexResolver) {
            this.memoryIndexResolver = memoryIndexResolver;

            return this;
        }

        public Builder threadUserIdResolver(Function<String, Long> threadUserIdResolver) {
            this.threadUserIdResolver = threadUserIdResolver;

            return this;
        }

        public Builder chatToolBindingResolver(
            ChatToolBindingResolver chatToolBindingResolver) {

            this.chatToolBindingResolver = chatToolBindingResolver;

            return this;
        }

        public Builder overrideChatClientResolver(@Nullable OverrideChatClientResolver overrideChatClientResolver) {
            this.overrideChatClientResolver = overrideChatClientResolver;

            return this;
        }

        public Builder chatModel(ChatModel chatModel) {
            super.chatModel(chatModel);

            return this;
        }

        public Builder advisors(List<Advisor> advisors) {
            super.advisors(advisors);

            return this;
        }

        public Builder advisor(Advisor advisor) {
            super.advisor(advisor);

            return this;
        }

        public Builder tools(List<Object> tools) {
            super.tools(tools);

            return this;
        }

        public Builder tool(Object tool) {
            super.tool(tool);

            return this;
        }

        public Builder agentId(String agentId) {
            super.agentId(agentId);

            this.usageAgentName = agentId;

            return this;
        }

        public Builder llmUsageRecorder(@Nullable LlmUsageRecorder llmUsageRecorder) {
            this.llmUsageRecorder = llmUsageRecorder;

            return this;
        }

        /**
         * Wires the workspace content-guardrails engine into every LLM turn this agent resolves a {@link ChatClient}
         * for — see {@link #attachGuardrailsAdvisor}. Both arguments are expected together: {@code aiGuardrails} is the
         * standalone engine bean (absent when the EE guardrails module isn't on the classpath), and
         * {@code aiGuardrailMetrics} is a caller-owned {@code AiGuardrailMetrics} instance fixed to the {@code ai_hub}
         * surface — deliberately NOT the shared engine-internal metrics bean, which is tagged with a single
         * deployment-wide surface property and conditional on the AI Gateway being enabled (this agent must record
         * under {@code ai_hub} regardless of that flag).
         */
        public Builder
            aiGuardrails(@Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics) {
            this.aiGuardrails = aiGuardrails;
            this.aiGuardrailMetrics = aiGuardrailMetrics;

            return this;
        }

        /**
         * Wires the workspace system prompt engine so {@link #resolveChatClient} appends the admin's standing
         * instructions to every LLM turn — see {@link #attachWorkspaceSystemPromptAdvisor}. Null (module absent)
         * disables the overlay.
         */
        public Builder workspaceSystemPrompts(@Nullable WorkspaceSystemPrompts workspaceSystemPrompts) {
            this.workspaceSystemPrompts = workspaceSystemPrompts;

            return this;
        }

        public Builder state(State state) {
            super.state(state);

            return this;
        }

        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            // Defer wrapping until build() — by that point securityContextRehydrator is set (or known
            // absent) so we can apply RehydrateContextToolCallback consistently. Super's own toolCallbacks
            // list stays empty so we don't end up with a half-wrapped duplicate at the ChatClient
            // registration boundary.
            pendingToolCallbacks.addAll(toolCallbacks);

            return this;
        }

        public Builder toolCallback(ToolCallback toolCallback) {
            pendingToolCallbacks.add(toolCallback);

            return this;
        }

        public Builder systemMessage(String systemMessage) {
            super.systemMessage(systemMessage);

            return this;
        }

        public Builder systemMessageProvider(Function<LocalAgent, String> systemMessageProvider) {
            super.systemMessageProvider(systemMessageProvider);

            return this;
        }

        public Builder chatMemory(ChatMemory chatMemory) {
            super.chatMemory(chatMemory);

            return this;
        }

        public Builder messages(List<BaseMessage> messages) {
            super.messages(messages);

            return this;
        }

        public AiHubSpringAIAgent build() throws AGUIException {
            // Defensive last-mile guard against Anthropic's "messages.<N>: user messages must have
            // non-empty content" HTTP 400. NonEmptyToolCallback catches empties at the tool boundary;
            // this catches them on the outbound chat request itself (chat-memory replay of older rows,
            // framework paths that bypass the callback wrapper, etc.). See NonEmptyMessagesAdvisor for
            // the full failure-mode catalogue.
            super.advisor(new NonEmptyMessagesAdvisor());

            // Diagnostic. Ordered AFTER NonEmptyMessagesAdvisor (which sits at Integer.MAX_VALUE - 1)
            // so the log captures the post-strip request shape that actually hits the LLM — diagnosing
            // a 400 from Anthropic is much easier when the log shows what we really sent, not the
            // pre-strip version. Output is gated on the log
            // `org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor` at DEBUG; default INFO
            // root level means zero overhead in production.
            super.advisor(new SimpleLoggerAdvisor(Integer.MAX_VALUE));

            // Records each turn's token usage into the ai_llm_usage metering store (source = AI_HUB, attributed to
            // the verified workspace/user published via advisorParams) and logs the provider's native prompt-cache
            // counters (cache_read / cache_creation) at DEBUG so caching effectiveness can be measured directly
            // rather than inferred from SimpleLoggerAdvisor's flattened usage getters. See AiHubModelUsageAdvisor.
            super.advisor(new AiHubModelUsageAdvisor(usageAgentName, llmUsageRecorder));

            // Now wrap and register the deferred tool callbacks. We delay until build() so that
            // securityContextRehydrator order doesn't matter for the caller — it can be set before or
            // after toolCallbacks/.toolCallback and the wrap still applies. Wrapping order is canonical:
            // context rehydration OUTERMOST so it sets tenant + SecurityContext for both the empty-return
            // guard and the actual callback. See RehydrateContextToolCallback for the
            // @PreAuthorize-on-Reactor-thread failure mode this prevents.
            super.toolCallbacks(pendingToolCallbacks.stream()
                .map(this::wrapForAgent)
                .toList());

            return new AiHubSpringAIAgent(this);
        }

        private ToolCallback wrapForAgent(ToolCallback callback) {
            return AiHubToolCallbackWrappers.wrap(callback, securityContextRehydrator);
        }
    }
}
