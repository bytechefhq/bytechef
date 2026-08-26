/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Base class for every Copilot AG-UI agent. It exists to re-bind, on the agent's own worker thread, the per-request
 * context that thread cannot inherit: {@link TenantContext} and {@link EnvironmentContext}.
 *
 * <p>
 * {@code LocalAgent.runAgent} dispatches the agent body through a bare {@code CompletableFuture.runAsync} with no
 * executor argument, so {@link #run} executes on a {@code ForkJoinPool.commonPool()} worker. Both context holders are
 * plain, non-inheritable {@code ThreadLocal}s, and {@code TenantContext}'s initial value is
 * {@code TenantContext.DEFAULT_TENANT_ID} — so without this binding every query issued from the agent body silently
 * reads the {@code public} schema instead of the caller's. Nothing throws and nothing logs; the run simply sees the
 * wrong tenant's data. Reactor's automatic context propagation does not rescue it: it acts at Reactor scheduler
 * boundaries, and the request-to-agent hop is a plain {@code CompletableFuture}, so the {@code .contextCapture()} in
 * {@code SpringAIAgent.run} captures on a worker that has already lost the tenant.
 *
 * <p>
 * This is the single point that covers the whole family, deliberately in preference to per-agent overrides that a new
 * agent can forget: every Copilot agent extends this class, and {@code SpringAIAgent.run} — the body handed to
 * {@code runAsync} — is only reachable through {@link #run} here.
 *
 * <p>
 * Both bindings save the previous value and restore it in a {@code finally}. The common pool is shared with unrelated
 * work, so a leaked binding would contaminate whatever runs next on that thread. Restoring rather than clearing also
 * makes the binding safely re-entrant with {@code RehydrateContextToolCallback}, which performs the same save/set/
 * restore around each tool call further down the stack.
 *
 * @author Ivica Cardic
 */
public abstract class CopilotSpringAIAgent extends SpringAIAgent {

    private static final Logger log = LoggerFactory.getLogger(CopilotSpringAIAgent.class);

    private final @Nullable OverrideChatClientResolver overrideChatClientResolver;

    protected CopilotSpringAIAgent(
        SpringAIAgent.Builder builder, @Nullable OverrideChatClientResolver overrideChatClientResolver)
        throws AGUIException {

        super(builder);

        this.overrideChatClientResolver = overrideChatClientResolver;
    }

    /**
     * {@code final} on purpose: this is the only path by which a Copilot agent reaches {@code SpringAIAgent.run}, and
     * therefore the only place the per-run context bindings below can be applied. An agent that overrode it and forgot
     * to call {@code super.run} would silently run against the {@code public} schema — which is exactly the drift that
     * once left five agents extending {@code SpringAIAgent} directly and outside this seam. Sealing the method turns
     * "somebody must remember" into a compile error.
     *
     * <p>
     * Subclasses that need to wrap the run have {@link #decorateSubscriber} and the hooks {@code SpringAIAgent} already
     * provides ({@code toolContext}, {@code additionalToolCallbacks}, {@code advisorParams},
     * {@code resolveChatClient}). If a future agent needs something none of those express, add a hook here rather than
     * reopening this method.
     */
    @Override
    protected final void run(RunAgentInput input, AgentSubscriber subscriber) {
        AgentSubscriber decoratedSubscriber = decorateSubscriber(subscriber);

        runWithTenant(input, () -> runWithEnvironment(input, () -> super.run(input, decoratedSubscriber)));
    }

    /**
     * Hook for subclasses that need to observe or rewrite the event stream for the whole run — the one thing a
     * {@code run} override was previously used for here. The returned subscriber is what {@code SpringAIAgent.run}
     * emits to. Default returns the subscriber unchanged.
     *
     * @param subscriber the subscriber the caller passed to {@code runAgent}
     * @return the subscriber the run should emit to (never {@code null})
     */
    protected AgentSubscriber decorateSubscriber(AgentSubscriber subscriber) {
        return subscriber;
    }

    /**
     * Binds {@link TenantContext} for the whole agent run from the tenant id the request thread captured into
     * {@link CopilotConstants#STATE_TENANT_ID}.
     *
     * <p>
     * When the key is absent the ambient tenant is left exactly as it is and the omission is logged, rather than the
     * carried value being defaulted. Binding {@code TenantContext.DEFAULT_TENANT_ID} here would fix nothing and would
     * make a producer that forgot to capture the tenant indistinguishable from one that captured it correctly. All
     * three producers ({@code CopilotChatFacadeImpl}, {@code ConnectedUserCopilotApiController} and
     * {@code CopilotWorkflowGeneratorImpl}) set the key unconditionally, so an absent key means a new entry point that
     * has not been wired up yet.
     *
     * <p>
     * Deliberately not {@link TenantContext#runWithTenantId}, which has the same save/set/restore shape: that helper
     * catches every {@code Exception} the body throws and rethrows it wrapped in a {@code RuntimeException}. The agent
     * body's exceptions are load-bearing here — {@code LocalAgent.runAgent} unwraps only {@code CompletionException}
     * before handing the cause to {@code AgentSubscriber.onRunFailed}, and {@code SpringAIAgent.run} reports
     * {@code getMessage()} of what it catches as the client-visible RUN_ERROR text — so wrapping would replace every
     * failure's message with "Unable to execute run with tenant ID …". Do not "simplify" this to that helper.
     */
    void runWithTenant(RunAgentInput input, Runnable action) {
        String tenantId = getTenantId(input);

        if (tenantId == null) {
            log.warn(
                "{}: run state carries no '{}'; leaving the agent thread on the ambient tenant '{}'. Every entry point "
                    + "must capture the tenant id on the request thread.",
                getClass().getSimpleName(), CopilotConstants.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

            action.run();

            return;
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            action.run();
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    void runWithEnvironment(RunAgentInput input, Runnable action) {
        Integer environmentId = getEnvironmentId(input);

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        if (environmentId != null) {
            EnvironmentContext.set(environmentId);
        }

        try {
            action.run();
        } finally {
            if (environmentId != null) {
                if (previousEnvironment == null) {
                    EnvironmentContext.clear();
                } else {
                    EnvironmentContext.set(previousEnvironment);
                }
            }
        }
    }

    /**
     * Returns the per-request {@link ChatClient}. Consults the override resolver first (for the user-selected
     * (provider, model) pair supplied via AG-UI state); falls back to the builder-time default whenever the resolver is
     * absent, returns {@code null}, or throws.
     *
     * <p>
     * The override path is best-effort by design: any failure (missing provider, factory throw, malformed state) must
     * fall back to the workspace default rather than failing the turn, and the absence of an override simply means "use
     * the configured default". The warn logs the full exception rather than just its message, since a resolver that
     * throws is a configuration problem someone has to diagnose.
     */
    @Override
    protected ChatClient resolveChatClient(RunAgentInput input) {
        if (overrideChatClientResolver == null) {
            return super.resolveChatClient(input);
        }

        try {
            ChatClient override = overrideChatClientResolver.resolve(input.state());

            if (override != null) {
                return override;
            }
        } catch (RuntimeException exception) {
            log.warn(
                "{}: override ChatClient resolver threw; falling back to default.", getClass().getSimpleName(),
                exception);
        }

        return super.resolveChatClient(input);
    }

    private static @Nullable Integer getEnvironmentId(RunAgentInput input) {
        State state = input.state();

        if (state == null) {
            return null;
        }

        Long environmentId = NumberUtils.asLong(state.get(CopilotConstants.STATE_ENVIRONMENT_ID));

        if (environmentId == null || environmentId < 0 || environmentId >= Environment.values().length) {
            return null;
        }

        return environmentId.intValue();
    }

    private static @Nullable String getTenantId(RunAgentInput input) {
        State state = input.state();

        if (state == null) {
            return null;
        }

        String tenantId = StringUtils.asString(state.get(CopilotConstants.STATE_TENANT_ID));

        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }

        return tenantId;
    }
}
