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
import com.bytechef.ai.copilot.agent.CopilotRunErrorCode;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.advisor.TokenUsageLoggerAdvisor;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGateToolCallback;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.util.Source;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
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
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * AI Hub LLM agent. Extends {@link SpringAIAgent} with AI Hub-specific behavior: binds the verified tenant and
 * environment for the turn, propagates workspace/user/thread context from the run state into every tool call, and
 * injects the client's active tab and referenced resources into the system message.
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
            """;

    private static final String AWAITING_ANSWER_KEY = "awaitingAnswer";
    private static final String AWAITING_APPROVAL_KEY = "awaitingApproval";

    /**
     * Per-request resolver for the turn's {@link ChatClient}: the user's per-conversation model selection or the
     * environment default.
     *
     * <p>
     * Returning {@code null} means "use the agent's builder-time default ChatClient." A thrown exception fails the turn
     * with a RUN_ERROR.
     *
     * <p>
     * Deliberately hub-local rather than the Copilot's {@code OverrideChatClientResolver}: the Copilot resolves that
     * one with {@code ObjectProvider.getIfAvailable()}, so a second implementation in the context would break every
     * Copilot agent bean.
     */
    @FunctionalInterface
    public interface OverrideChatClientResolver {

        @Nullable
        ChatClient resolve(State state);
    }

    private final @Nullable OverrideChatClientResolver overrideChatClientResolver;
    private final @Nullable Mode mode;
    private final List<ToolCallback> pinnedToolCallbacks;
    private final @Nullable SecurityContextRehydrator securityContextRehydrator;
    private final List<ToolCallback> toolCallbacks;
    private final @Nullable WebSearchToolCallbackProvider webSearchToolCallbackProvider;

    protected AiHubSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder);

        this.overrideChatClientResolver = builder.overrideChatClientResolver;
        this.mode = builder.mode;
        this.pinnedToolCallbacks = List.copyOf(builder.pendingToolCallbacks);
        this.securityContextRehydrator = builder.securityContextRehydrator;
        this.toolCallbacks = List.copyOf(builder.wrappedToolCallbacks);
        this.webSearchToolCallbackProvider = builder.webSearchToolCallbackProvider;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns this agent's pinned static tool callbacks, UNWRAPPED: none of the approval-gate, context-rehydration, or
     * empty-guard wrappers {@link Builder#build()} applies before registering them with the underlying
     * {@code ChatClient}. The approval facade re-finds an approved call's tool here.
     */
    public List<ToolCallback> pinnedToolCallbacks() {
        return pinnedToolCallbacks;
    }

    /**
     * Returns the tool callbacks exactly as registered with the underlying {@code ChatClient}, each wrapped by
     * {@link AiHubToolCallbackWrappers}.
     */
    public List<ToolCallback> toolCallbacks() {
        return toolCallbacks;
    }

    /**
     * Adds the web search tools of the providers enabled in the turn's environment. They are read-only, so they pass
     * the empty-result and context wrappers but not the approval gate, and they are not pinned: an approval never
     * resumes one of them.
     */
    @Override
    protected List<ToolCallback> additionalToolCallbacks(RunAgentInput input) {
        Integer environmentOrdinal = environmentOrdinal(input);

        if (webSearchToolCallbackProvider == null || environmentOrdinal == null) {
            return super.additionalToolCallbacks(input);
        }

        return webSearchToolCallbackProvider.getToolCallbacks(environmentOrdinal)
            .stream()
            .map(toolCallback -> AiHubToolCallbackWrappers.wrap(toolCallback, securityContextRehydrator))
            .toList();
    }

    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        AiHubAgentTenantBinder.runWithTenant(
            getAgentId(), input.state(), () -> runWithEnvironment(input, subscriber));
    }

    /**
     * Binds the verified environment to {@link EnvironmentContext} on the agent thread for the turn. Restores the
     * thread's previous environment rather than clearing it: the common pool is shared, so a worker that arrives
     * already bound must leave as it arrived.
     */
    private void runWithEnvironment(RunAgentInput input, AgentSubscriber subscriber) {
        Integer environmentOrdinal = environmentOrdinal(input);

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        if (environmentOrdinal != null) {
            EnvironmentContext.set(environmentOrdinal);
        }

        try {
            CurrentAgentContext.runWith(
                AgentTypeRegistry.fromKey(getAgentId()), null, () -> super.run(input, subscriber));
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
     * Publishes the thread id as {@link ChatMemory#CONVERSATION_ID}, the key the session memory advisor resolves its
     * session from, and the AG-UI id of the run's user message as {@link AiHubSessionMemory#USER_MESSAGE_ID_PARAM}, the
     * id the advisor stores that message under. The parent agent only sets the conversation id on its own chat-memory
     * branch, which the hub does not take.
     */
    @Override
    protected Map<String, Object> advisorParams(RunAgentInput input) {
        Map<String, Object> advisorParams = new HashMap<>();

        String threadId = input.threadId();

        if (threadId != null && !threadId.isBlank()) {
            advisorParams.put(ChatMemory.CONVERSATION_ID, threadId);
        }

        String userMessageId = lastUserMessageId(input.messages());

        if (userMessageId != null && !userMessageId.isBlank()) {
            advisorParams.put(AiHubSessionMemory.USER_MESSAGE_ID_PARAM, userMessageId);
        }

        return advisorParams;
    }

    /**
     * The {@code llmProvider}/{@code llmModel} keys populated here MUST stay consistent with
     * {@link AiHubChatClientResolver}: a mismatch would run a delegate subagent on a different model than the one its
     * caller resolved for this turn.
     */
    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        AiHubToolInvocationContext aiHubContext = buildInvocationContext(input);

        Map<String, Object> toolContext = new HashMap<>(aiHubContext.toToolContext());

        State state = input.state();

        String tenantId = state == null ? null : asString(state.get(AiHubStateKeys.VERIFIED_TENANT_ID));

        SelectedLlm selectedLlm = resolveSelectedLlm(state);

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

        String resolvedMode = resolveMode(state);

        if (resolvedMode != null) {
            toolContext.put(AiHubApprovalGateToolCallback.TOOL_CONTEXT_MODE_KEY, resolvedMode);
        }

        return toolContext;
    }

    private @Nullable String resolveMode(@Nullable State state) {
        if (mode != null) {
            return mode.name();
        }

        Object stateMode = state == null ? null : state.get("mode");

        return stateMode instanceof String stringMode ? stringMode : null;
    }

    /**
     * Ends the turn when a tool result waits on the user: a picker or question ({@code "awaitingAnswer": true},
     * including a delegate's relayed question) or a tool call held for approval ({@code "awaitingApproval": true}).
     */
    @Override
    protected boolean endsTurn(ToolResponseMessage.ToolResponse toolResponse) {
        String responseData = toolResponse.responseData();

        if (responseData == null
            || (!responseData.contains(AWAITING_ANSWER_KEY) && !responseData.contains(AWAITING_APPROVAL_KEY))) {

            return false;
        }

        try {
            return JsonUtils.read(responseData) instanceof Map<?, ?> response
                && (Boolean.TRUE.equals(response.get(AWAITING_ANSWER_KEY))
                    || Boolean.TRUE.equals(response.get(AWAITING_APPROVAL_KEY)));
        } catch (RuntimeException exception) {
            log.warn(
                "Tool {} returned an awaiting payload that isn't valid JSON; continuing the turn", toolResponse.name(),
                exception);

            return false;
        }
    }

    /**
     * The (provider, model) pair a subagent delegate should be handed. Precedence mirrors
     * {@link AiHubChatClientResolver#resolve(State)}: the user's per-conversation selection, or null for the
     * environment default. The resolver fails a turn with a half-set pair before any delegate runs.
     */
    static @Nullable SelectedLlm resolveSelectedLlm(@Nullable State state) {
        if (state == null) {
            return null;
        }

        String llmProvider = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY));
        String llmModel = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY));

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

    @Override
    protected boolean splitsTextAtToolCalls() {
        return true;
    }

    @Override
    protected @Nullable String runErrorCode(RunAgentInput input, Throwable throwable) {
        CopilotRunErrorCode copilotRunErrorCode = CopilotRunErrorCode.of(throwable);

        return copilotRunErrorCode == null ? null : copilotRunErrorCode.name();
    }

    /**
     * Returns the per-request {@link ChatClient} from the override resolver, falling back to the builder-time default
     * only when no resolver is registered or it returns null. A resolver failure is rethrown so the turn ends with a
     * RUN_ERROR.
     */
    @Override
    protected ChatClient resolveChatClient(RunAgentInput input) {
        if (overrideChatClientResolver == null) {
            return resolveDefaultChatClient(input);
        }

        ChatClient override;

        try {
            override = overrideChatClientResolver.resolve(input.state());
        } catch (RuntimeException exception) {
            log.warn("{}: resolving the chat model failed: {}", getAgentId(), exception.getMessage());

            throw exception;
        }

        if (override != null) {
            return override;
        }

        return resolveDefaultChatClient(input);
    }

    private ChatClient resolveDefaultChatClient(RunAgentInput input) {
        try {
            return super.resolveChatClient(input);
        } catch (AGUIException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    /**
     * Workspace and user come from the controller-verified keys, never from raw request fields: the controller checks
     * workspace membership and chat ownership before this runs and rewrites the verified values into reserved keys.
     */
    AiHubToolInvocationContext buildInvocationContext(RunAgentInput input) {
        State state = input.state();

        Long workspaceId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID));
        Long userId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.AUTHENTICATED_USER_ID));
        Long rawEnvironmentId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.ENVIRONMENT_ID));
        long environmentId = rawEnvironmentId != null ? rawEnvironmentId : 0L;
        Short sourceOrdinal = Source.AI_HUB.toAgentSourceOrdinal();
        String lastUserPrompt = lastUserPrompt(input.messages());
        String threadId = state == null ? null : asString(state.get(AiHubStateKeys.VERIFIED_THREAD_ID));
        Long ownerUserId = state == null ? null : NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_OWNER_USER_ID));

        return new AiHubToolInvocationContext(
            workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId, threadId, ownerUserId);
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Object activeTab = state == null ? null : state.get("activeTab");

        if (activeTab instanceof Map<?, ?> activeTabMap) {
            contexts.add(new Context("Active Tab", formatActiveTab(activeTabMap)));
        }

        Object referencedResources = state == null ? null : state.get("referencedResources");

        if (referencedResources instanceof List<?> resources && !resources.isEmpty()) {
            contexts.add(new Context("Referenced Resources", formatReferencedResources(resources)));
        }

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

    private static @Nullable String lastUserPrompt(@Nullable List<BaseMessage> messages) {
        UserMessage userMessage = lastUserMessage(messages);

        return userMessage == null ? null : userMessage.getContent();
    }

    private static @Nullable String lastUserMessageId(@Nullable List<BaseMessage> messages) {
        UserMessage userMessage = lastUserMessage(messages);

        return userMessage == null ? null : userMessage.getId();
    }

    private static @Nullable UserMessage lastUserMessage(@Nullable List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int index = messages.size() - 1; index >= 0; index--) {
            BaseMessage message = messages.get(index);

            if (message instanceof UserMessage userMessage && Role.user.equals(userMessage.getRole())) {
                return userMessage;
            }
        }

        return null;
    }

    public static class Builder extends SpringAIAgent.Builder {

        private static final String TOOL_OBJECTS_UNSUPPORTED_MESSAGE =
            "AI Hub tools must be ToolCallbacks so they pass the approval gate; use toolCallbacks(...)";

        private String agentId = "ai_hub";
        private @Nullable OverrideChatClientResolver overrideChatClientResolver;
        private @Nullable SecurityContextRehydrator securityContextRehydrator;
        private @Nullable AiHubApprovalGate approvalGate;
        private @Nullable Mode mode;
        private @Nullable WebSearchToolCallbackProvider webSearchToolCallbackProvider;
        private final List<ToolCallback> pendingToolCallbacks = new ArrayList<>();
        private final List<ToolCallback> wrappedToolCallbacks = new ArrayList<>();

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder securityContextRehydrator(@Nullable SecurityContextRehydrator securityContextRehydrator) {
            this.securityContextRehydrator = securityContextRehydrator;

            return this;
        }

        /**
         * Offers each turn the web search tools of the providers enabled in its environment. A {@code null} provider
         * offers none.
         */
        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder webSearchToolCallbackProvider(
            @Nullable WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

            this.webSearchToolCallbackProvider = webSearchToolCallbackProvider;

            return this;
        }

        public Builder overrideChatClientResolver(@Nullable OverrideChatClientResolver overrideChatClientResolver) {
            this.overrideChatClientResolver = overrideChatClientResolver;

            return this;
        }

        /**
         * Wires the tool approval gate so a flagged tool call waits for a person's approval before executing. A
         * {@code null} gate skips the gate layer.
         */
        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder approvalGate(@Nullable AiHubApprovalGate approvalGate) {
            this.approvalGate = approvalGate;

            return this;
        }

        /**
         * The mode this agent serves. It is written into every tool call's context so an approval row records the mode
         * whose pinned tools can re-find the call, independent of what the client put in the state.
         */
        public Builder mode(@Nullable Mode mode) {
            this.mode = mode;

            return this;
        }

        @Override
        public Builder chatModel(ChatModel chatModel) {
            super.chatModel(chatModel);

            return this;
        }

        @Override
        public Builder advisors(List<Advisor> advisors) {
            super.advisors(advisors);

            return this;
        }

        @Override
        public Builder advisor(Advisor advisor) {
            super.advisor(advisor);

            return this;
        }

        @Override
        public Builder tools(List<Object> tools) {
            throw new UnsupportedOperationException(TOOL_OBJECTS_UNSUPPORTED_MESSAGE);
        }

        @Override
        public Builder tool(Object tool) {
            throw new UnsupportedOperationException(TOOL_OBJECTS_UNSUPPORTED_MESSAGE);
        }

        @Override
        public Builder agentId(String agentId) {
            super.agentId(agentId);

            this.agentId = agentId;

            return this;
        }

        @Override
        public Builder state(State state) {
            super.state(state);

            return this;
        }

        @Override
        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            pendingToolCallbacks.addAll(toolCallbacks);

            return this;
        }

        @Override
        public Builder toolCallback(ToolCallback toolCallback) {
            pendingToolCallbacks.add(toolCallback);

            return this;
        }

        @Override
        public Builder systemMessage(String systemMessage) {
            super.systemMessage(systemMessage);

            return this;
        }

        @Override
        public Builder systemMessageProvider(Function<LocalAgent, String> systemMessageProvider) {
            super.systemMessageProvider(systemMessageProvider);

            return this;
        }

        @Override
        public Builder chatMemory(ChatMemory chatMemory) {
            super.chatMemory(chatMemory);

            return this;
        }

        @Override
        public Builder messages(List<BaseMessage> messages) {
            super.messages(messages);

            return this;
        }

        /**
         * Wraps and registers the deferred tool callbacks here, so the order in which the caller supplies the
         * rehydrator, the approval gate and the callbacks does not matter.
         */
        @Override
        public AiHubSpringAIAgent build() throws AGUIException {
            super.advisor(new NonEmptyMessagesAdvisor());
            super.advisor(new SimpleLoggerAdvisor(Integer.MAX_VALUE));
            super.advisor(new TokenUsageLoggerAdvisor(agentId));

            wrappedToolCallbacks.clear();

            for (ToolCallback toolCallback : pendingToolCallbacks) {
                wrappedToolCallbacks.add(
                    AiHubToolCallbackWrappers.wrap(toolCallback, securityContextRehydrator, approvalGate));
            }

            super.toolCallbacks(wrappedToolCallbacks);

            return new AiHubSpringAIAgent(this);
        }
    }
}
