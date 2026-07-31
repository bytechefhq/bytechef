/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.guardrails;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.advisor.AiGuardrailsAdvisor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.Resource;

/**
 * Wraps a subagent delegate's own {@link ChatClient} so its one-shot LLM call runs under the calling workspace's
 * {@link AiGuardrailsAdvisor} — closing the coverage gap documented on {@code AiHubSpringAIAgent#resolveChatClient}:
 * previously only the top-level AI Hub agent's own {@link ChatClient} carried a guardrails advisor, while every
 * delegate ChatClient (Copilot specialists, the AI-hub-owned research/data_analyst/image_generator/slide_builder
 * subagents, and the mcp_manager/personal_agent_manager/deployment_manager/api_collection_manager specialists) ran
 * completely unguarded.
 *
 * <p>
 * The delegate {@link ChatClient} beans backing these specialists are process-wide singletons shared by every
 * workspace, so the workspace id cannot be baked in at bean-construction time the way
 * {@code AiHubSpringAIAgent#attachGuardrailsAdvisor} does for the top-level agent (which knows the workspace as soon as
 * the turn's {@code RunAgentInput} is available). Instead this wrapper defers resolution to the moment the delegate
 * {@code ToolCallback} forwards the parent's {@code ToolContext} via {@link ChatClientRequestSpec#toolContext(Map)} —
 * the exact same map every hand-rolled delegate callback ( {@code SkillsAgentToolCallback},
 * {@code ManagerSubAgentToolCallback}, {@code ResearchToolCallback}, etc.) already builds from its own
 * {@code ToolContext} parameter and forwards so the specialist's own workspace-scoped tools keep working — see
 * {@link AgentToolInvocationContext#TOOL_CONTEXT_WORKSPACE_ID_KEY}. No changes to any of those delegate classes are
 * required: wrapping happens once, in {@code AiHubConfiguration}, at the single point where each delegate's
 * {@code ChatClient} bean is handed to its {@code ToolCallback} constructor.
 * </p>
 *
 * <p>
 * A missing {@link AiGuardrails} bean (guardrails module absent) skips wrapping entirely via {@link #wrap} — the
 * unchanged, pre-existing behaviour. When present, {@link AiGuardrails#isActive(Long)} is still re-checked on every
 * call (not just once at wrap time) since the resolved workspace id is only known per call; a workspace with every
 * guardrail disabled pays no advisor-construction overhead, mirroring
 * {@code AiHubSpringAIAgent#attachGuardrailsAdvisor}'s own fast path.
 * </p>
 *
 * <p>
 * <b>Null/absent workspace id</b> — when the forwarded {@code ToolContext} carries no resolvable workspace id (never
 * captured, or a delegate that calls {@code .call()}/{@code .stream()} without ever calling {@code .toolContext(...)}
 * first), {@link AiGuardrails#isActive(Long)} and {@link AiGuardrailsAdvisor} are invoked with a {@code null} workspace
 * id — the same tenant-default fallback {@code AiHubSpringAIAgent#attachGuardrailsAdvisor} uses when the verified
 * workspace id is absent from turn state.
 * </p>
 *
 * <p>
 * <b>Block-mode UX inside a subagent</b> — a BLOCK-mode violation makes {@link AiGuardrailsAdvisor#adviseCall} throw
 * {@code AiGuardrailViolationException} synchronously out of {@code ChatClientRequestSpec.call()}. Every hand-rolled
 * delegate {@code ToolCallback} already wraps its {@code chatClient.prompt(...).call()} invocation in a
 * {@code catch (RuntimeException exception)} arm that converts the failure into a JSON tool-error string via
 * {@code ToolErrors.runtimeFailure(...)} rather than letting it propagate further — so the violation surfaces to the
 * parent LLM as an ordinary tool result (e.g. {@code {"error":"mcp_manager failed (AiGuardrailViolationException)"}})
 * instead of aborting the whole agent turn. {@code ToolErrors.runtimeFailure} deliberately reports only the exception's
 * simple class name, not {@link Exception#getMessage()} — so even the violation's category never reaches the tool-error
 * payload; this is stricter than the top-level agent's own category-only surfacing, not a regression.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubAgentGuardrailedChatClient implements ChatClient {

    private final ChatClient delegate;
    private final AiGuardrails aiGuardrails;
    private final AiGuardrailMetrics aiGuardrailMetrics;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private SubAgentGuardrailedChatClient(
        ChatClient delegate, AiGuardrails aiGuardrails, AiGuardrailMetrics aiGuardrailMetrics) {

        this.delegate = delegate;
        this.aiGuardrails = aiGuardrails;
        this.aiGuardrailMetrics = aiGuardrailMetrics;
    }

    /**
     * Returns {@code chatClient} wrapped so every call it serves runs under the workspace's
     * {@link AiGuardrailsAdvisor}, or {@code chatClient} itself unchanged when {@code aiGuardrails} (or its paired
     * {@code aiGuardrailMetrics}) is {@code null} — the EE guardrails module not being on the classpath, or the caller
     * not having wired one.
     */
    public static ChatClient wrap(
        ChatClient chatClient, @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics) {

        if (aiGuardrails == null || aiGuardrailMetrics == null) {
            return chatClient;
        }

        return new SubAgentGuardrailedChatClient(chatClient, aiGuardrails, aiGuardrailMetrics);
    }

    @Override
    public ChatClientRequestSpec prompt() {
        return new GuardedRequestSpec(delegate.prompt());
    }

    @Override
    public ChatClientRequestSpec prompt(String content) {
        return new GuardedRequestSpec(delegate.prompt(content));
    }

    @Override
    public ChatClientRequestSpec prompt(Prompt prompt) {
        return new GuardedRequestSpec(delegate.prompt(prompt));
    }

    @Override
    public Builder mutate() {
        return delegate.mutate();
    }

    private static @Nullable Long resolveWorkspaceId(@Nullable Map<String, Object> toolContext) {
        if (toolContext == null || toolContext.isEmpty()) {
            return null;
        }

        AgentToolInvocationContext context = AgentToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        return context == null ? null : context.workspaceId();
    }

    /**
     * Delegates every {@link ChatClientRequestSpec} builder call straight through, except {@link #toolContext(Map)} —
     * captured so the workspace id can be resolved once {@link #call()}/{@link #stream()} attaches the guardrail
     * advisor — and {@link #call()}/{@link #stream()} themselves, which attach the advisor first.
     *
     * <p>
     * {@code delegateSpec} is reassigned to each delegate call's own return value rather than discarded in favor of
     * {@code this} — the real Spring AI implementation happens to mutate itself and return {@code this}, but nothing in
     * the {@link ChatClientRequestSpec} contract guarantees that, so relying on it would be fragile. Reassigning also
     * satisfies SpotBugs' {@code RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT}, which otherwise flags every delegated fluent
     * call as a discarded return value.
     * </p>
     */
    private final class GuardedRequestSpec implements ChatClientRequestSpec {

        private ChatClientRequestSpec delegateSpec;
        private @Nullable Map<String, Object> capturedToolContext;

        private GuardedRequestSpec(ChatClientRequestSpec delegateSpec) {
            this.delegateSpec = delegateSpec;
        }

        @Override
        public Builder mutate() {
            return delegateSpec.mutate();
        }

        @Override
        public ChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer) {
            delegateSpec = delegateSpec.advisors(consumer);

            return this;
        }

        @Override
        public ChatClientRequestSpec advisors(Advisor... advisors) {
            delegateSpec = delegateSpec.advisors(advisors);

            return this;
        }

        @Override
        public ChatClientRequestSpec advisors(List<Advisor> advisors) {
            delegateSpec = delegateSpec.advisors(advisors);

            return this;
        }

        @Override
        public ChatClientRequestSpec messages(Message... messages) {
            delegateSpec = delegateSpec.messages(messages);

            return this;
        }

        @Override
        public ChatClientRequestSpec messages(List<Message> messages) {
            delegateSpec = delegateSpec.messages(messages);

            return this;
        }

        @Override
        public <B extends ChatOptions.Builder<?>> ChatClientRequestSpec options(B optionsBuilder) {
            delegateSpec = delegateSpec.options(optionsBuilder);

            return this;
        }

        @Override
        public ChatClientRequestSpec tools(Object... toolObjects) {
            delegateSpec = delegateSpec.tools(toolObjects);

            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks) {
            delegateSpec = delegateSpec.toolCallbacks(toolCallbacks);

            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(List<ToolCallback> toolCallbacks) {
            delegateSpec = delegateSpec.toolCallbacks(toolCallbacks);

            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(ToolCallbackProvider... toolCallbackProviders) {
            delegateSpec = delegateSpec.toolCallbacks(toolCallbackProviders);

            return this;
        }

        @Override
        public ChatClientRequestSpec toolContext(Map<String, Object> toolContext) {
            this.capturedToolContext = toolContext;

            delegateSpec = delegateSpec.toolContext(toolContext);

            return this;
        }

        @Override
        public ChatClientRequestSpec system(String text) {
            delegateSpec = delegateSpec.system(text);

            return this;
        }

        @Override
        public ChatClientRequestSpec system(Resource text, Charset charset) {
            delegateSpec = delegateSpec.system(text, charset);

            return this;
        }

        @Override
        public ChatClientRequestSpec system(Resource text) {
            delegateSpec = delegateSpec.system(text);

            return this;
        }

        @Override
        public ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer) {
            delegateSpec = delegateSpec.system(consumer);

            return this;
        }

        @Override
        public ChatClientRequestSpec user(String text) {
            delegateSpec = delegateSpec.user(text);

            return this;
        }

        @Override
        public ChatClientRequestSpec user(Resource text, Charset charset) {
            delegateSpec = delegateSpec.user(text, charset);

            return this;
        }

        @Override
        public ChatClientRequestSpec user(Resource text) {
            delegateSpec = delegateSpec.user(text);

            return this;
        }

        @Override
        public ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer) {
            delegateSpec = delegateSpec.user(consumer);

            return this;
        }

        @Override
        public ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer) {
            delegateSpec = delegateSpec.templateRenderer(templateRenderer);

            return this;
        }

        @Override
        public CallResponseSpec call() {
            attachGuardrailAdvisorIfActive();

            return delegateSpec.call();
        }

        @Override
        public StreamResponseSpec stream() {
            attachGuardrailAdvisorIfActive();

            return delegateSpec.stream();
        }

        private void attachGuardrailAdvisorIfActive() {
            Long workspaceId = resolveWorkspaceId(capturedToolContext);

            if (aiGuardrails.isActive(workspaceId)) {
                delegateSpec = delegateSpec.advisors(
                    new AiGuardrailsAdvisor(aiGuardrails, workspaceId, aiGuardrailMetrics));
            }
        }
    }
}
