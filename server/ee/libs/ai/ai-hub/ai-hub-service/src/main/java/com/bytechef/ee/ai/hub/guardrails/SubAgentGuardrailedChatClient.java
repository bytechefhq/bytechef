/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.guardrails;

import com.bytechef.ee.ai.hub.subagent.SubAgentAdvisorContributor;
import com.bytechef.ee.ai.hub.subagent.WorkspaceAdvisorContributor;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.Resource;

/**
 * Wraps a subagent delegate's own {@link ChatClient} so its one-shot LLM call runs under the per-request advisors
 * contributed by a list of {@link SubAgentAdvisorContributor}s — the calling workspace's {@code AiGuardrailsAdvisor}
 * and {@code WorkspaceSystemPromptAdvisor} (see {@link WorkspaceAdvisorContributor}), plus the specialist's own
 * per-conversation session memory. This closes the coverage gap documented on
 * {@code AiHubSpringAIAgent#resolveChatClient}: previously only the top-level AI Hub agent's own {@link ChatClient}
 * carried these advisors, while every delegate ChatClient (Copilot specialists, the AI-hub-owned
 * research/data_analyst/image_generator/slide_builder subagents, and the
 * mcp_agent/task_agent/project_deployment_agent/api_collection_agent specialists) ran completely unguarded and without
 * the workspace's standing instructions.
 *
 * <p>
 * The class keeps its guardrails-era name even though it now dispatches an arbitrary contributor list — renaming would
 * churn all 24 call sites in {@code AiHubConfiguration} and the git history for no behavior gain. The contributor seam
 * exists because this class is overwhelmingly {@link ChatClientRequestSpec} delegation boilerplate: a second decorator
 * composed alongside it would have to duplicate every one of those methods, and every method added upstream would then
 * have to be implemented twice.
 * </p>
 *
 * <p>
 * The delegate {@link ChatClient} beans backing these specialists are process-wide singletons shared by every
 * workspace, so the workspace id cannot be baked in at bean-construction time the way
 * {@code AiHubSpringAIAgent#attachGuardrailsAdvisor} does for the top-level agent (which knows the workspace as soon as
 * the turn's {@code RunAgentInput} is available). Instead this wrapper defers resolution to the moment the delegate
 * {@code ToolCallback} forwards the parent's {@code ToolContext} via {@link ChatClientRequestSpec#toolContext(Map)} —
 * the exact same map every hand-rolled delegate callback ( {@code SkillsAgentToolCallback},
 * {@code SubAgentToolCallback}, {@code ResearchToolCallback}, etc.) already builds from its own {@code ToolContext}
 * parameter and forwards so the specialist's own workspace-scoped tools keep working — see
 * {@code AgentToolInvocationContext#TOOL_CONTEXT_WORKSPACE_ID_KEY}. No changes to any of those delegate classes are
 * required: wrapping happens once, in {@code AiHubConfiguration}, at the single point where each delegate's
 * {@code ChatClient} bean is handed to its {@code ToolCallback} constructor.
 * </p>
 *
 * <p>
 * An empty contributor list skips wrapping entirely via {@link #wrap} — the unchanged, pre-existing behaviour when the
 * guardrails module is absent. Each contributor re-evaluates its own preconditions on every call (not just once at wrap
 * time) since the resolved workspace and conversation ids are only known per call; a workspace with every guardrail
 * disabled pays no advisor-construction overhead, mirroring {@code AiHubSpringAIAgent#attachGuardrailsAdvisor}'s own
 * fast path.
 * </p>
 *
 * <p>
 * <b>Null/absent workspace id</b> — when the forwarded {@code ToolContext} carries no resolvable workspace id (never
 * captured, or a delegate that calls {@code .call()}/{@code .stream()} without ever calling {@code .toolContext(...)}
 * first), {@link AiGuardrails#isActive(Long)} and {@code AiGuardrailsAdvisor} are invoked with a {@code null} workspace
 * id — the same tenant-default fallback {@code AiHubSpringAIAgent#attachGuardrailsAdvisor} uses when the verified
 * workspace id is absent from turn state.
 * </p>
 *
 * <p>
 * <b>Block-mode UX inside a subagent</b> — a BLOCK-mode violation makes {@code AiGuardrailsAdvisor#adviseCall} throw
 * {@code AiGuardrailViolationException} synchronously out of {@code ChatClientRequestSpec.call()}. Every hand-rolled
 * delegate {@code ToolCallback} already wraps its {@code chatClient.prompt(...).call()} invocation in a
 * {@code catch (RuntimeException exception)} arm that converts the failure into a JSON tool-error string via
 * {@code ToolErrors.runtimeFailure(...)} rather than letting it propagate further — so the violation surfaces to the
 * parent LLM as an ordinary tool result (e.g. {@code {"error":"mcp_agent failed (AiGuardrailViolationException)"}})
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
    private final List<SubAgentAdvisorContributor> contributors;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private SubAgentGuardrailedChatClient(ChatClient delegate, List<SubAgentAdvisorContributor> contributors) {
        this.delegate = delegate;
        this.contributors = contributors;
    }

    /**
     * Returns {@code chatClient} wrapped so every call it serves runs under the advisors the given contributors attach,
     * or {@code chatClient} itself unchanged when there are none to attach.
     */
    public static ChatClient wrap(ChatClient chatClient, List<SubAgentAdvisorContributor> contributors) {
        if (contributors.isEmpty()) {
            return chatClient;
        }

        return new SubAgentGuardrailedChatClient(chatClient, List.copyOf(contributors));
    }

    /**
     * Returns {@code chatClient} wrapped so every call it serves runs under the workspace's {@code AiGuardrailsAdvisor}
     * and {@code WorkspaceSystemPromptAdvisor}, or {@code chatClient} itself unchanged when neither engine is present —
     * {@code aiGuardrails} (or its paired {@code aiGuardrailMetrics}) and {@code workspaceSystemPrompts} are both
     * {@code null}, meaning the corresponding EE module isn't on the classpath, or the caller didn't wire one.
     */
    public static ChatClient wrap(
        ChatClient chatClient, @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics,
        @Nullable WorkspaceSystemPrompts workspaceSystemPrompts) {

        boolean guardrailsPresent = aiGuardrails != null && aiGuardrailMetrics != null;

        if (!guardrailsPresent && workspaceSystemPrompts == null) {
            return chatClient;
        }

        return wrap(
            chatClient,
            List.of(
                new WorkspaceAdvisorContributor(
                    guardrailsPresent ? aiGuardrails : null, guardrailsPresent ? aiGuardrailMetrics : null,
                    workspaceSystemPrompts)));
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

    /**
     * Delegates every {@link ChatClientRequestSpec} builder call straight through, except {@link #toolContext(Map)} —
     * captured so each contributor can resolve what it needs once {@link #call()}/{@link #stream()} runs them — and
     * {@link #call()}/{@link #stream()} themselves, which run the contributors first.
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
            this.capturedToolContext = new HashMap<>(toolContext);

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
            attachContributedAdvisors();

            return delegateSpec.call();
        }

        @Override
        public StreamResponseSpec stream() {
            attachContributedAdvisors();

            return delegateSpec.stream();
        }

        private void attachContributedAdvisors() {
            for (SubAgentAdvisorContributor contributor : contributors) {
                delegateSpec = contributor.contribute(delegateSpec, capturedToolContext);
            }
        }
    }
}
