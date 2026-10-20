/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunStartedEvent;
import com.agui.core.exception.AGUIException;
import com.agui.server.LocalAgent;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-turn dispatcher that picks {@link WebhookBridgeAgent} or the underlying {@link AiHubSpringAIAgent} based on the
 * chat's {@link AiHubChatKind}. Registered as the {@code ai_hub_ask} / {@code ai_hub_build} {@link LocalAgent} bean so
 * the controller's {@code agentId -> agent} map dispatches every per-turn invocation through this router first.
 *
 * <p>
 * Routing rule:
 * </p>
 * <ul>
 * <li>A webhook-bridged kind ({@code WORKFLOW_CHAT}): delegate to {@link WebhookBridgeAgent}, which translates webhook
 * events to AG-UI events through {@link AgUiStreamBridge}. When no bridge bean is registered the turn ends with a run
 * error instead of reaching the LLM.</li>
 * <li>{@code STANDARD}: delegate to the wrapped {@link AiHubSpringAIAgent}.</li>
 * <li>No chat for the thread (race with delete, unknown thread id): delegate to the LLM agent, whose own error handling
 * surfaces the missing chat if it matters.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubRoutingAgent extends LocalAgent {

    private static final Logger log = LoggerFactory.getLogger(AiHubRoutingAgent.class);

    private static final String BRIDGE_UNAVAILABLE_MESSAGE =
        "Workflow chats are not available: this server has no webhook workflow executor.";

    private final AiHubSpringAIAgent llmAgent;
    private final @Nullable WebhookBridgeAgent webhookBridgeAgent;
    private final AiHubChatService chatService;

    /**
     * Constructs the routing agent. {@code webhookBridgeAgent} is nullable so deployments that do not ship the webhook
     * stack still wire the router; workflow chat turns then end with a run error.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubRoutingAgent(
        String agentId, AiHubSpringAIAgent llmAgent, @Nullable WebhookBridgeAgent webhookBridgeAgent,
        AiHubChatService chatService) throws AGUIException {

        super(agentId, null, null, "(unused, the routing agent delegates to a per-kind agent)", List.of());

        this.llmAgent = llmAgent;
        this.webhookBridgeAgent = webhookBridgeAgent;
        this.chatService = chatService;
    }

    @Override
    public CompletableFuture<Void> runAgent(RunAgentParameters parameters, AgentSubscriber subscriber) {
        AiHubChatKind kind = resolveKind(parameters.getThreadId());

        if (!kind.isWebhookBridged()) {
            return dispatch(llmAgent, parameters, subscriber, kind);
        }

        if (webhookBridgeAgent == null) {
            log.warn(
                "Webhook-bridged chat (kind={}, threadId={}) routed but the WebhookBridgeAgent bean is absent; " +
                    "ending the turn with a run error.",
                kind, parameters.getThreadId());

            return super.runAgent(parameters, subscriber);
        }

        return dispatch(webhookBridgeAgent, parameters, subscriber, kind);
    }

    /**
     * Reached only through {@code super.runAgent} for a webhook-bridged chat when no {@link WebhookBridgeAgent} bean is
     * registered: emits {@code RUN_STARTED} and {@code RUN_ERROR}, then finalizes the run so the client stream closes
     * and the chat is free for its next turn.
     */
    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        RunStartedEvent runStartedEvent = new RunStartedEvent();

        runStartedEvent.setThreadId(input.threadId());
        runStartedEvent.setRunId(input.runId());

        subscriber.onEvent(runStartedEvent);
        subscriber.onRunStartedEvent(runStartedEvent);

        RunErrorEvent runErrorEvent = new RunErrorEvent();

        runErrorEvent.setError(BRIDGE_UNAVAILABLE_MESSAGE);

        subscriber.onEvent(runErrorEvent);
        subscriber.onRunErrorEvent(runErrorEvent);

        subscriber.onRunFinalized(new AgentSubscriberParams(input.messages(), input.state(), this, input));
    }

    private CompletableFuture<Void> dispatch(
        LocalAgent target, RunAgentParameters parameters, AgentSubscriber subscriber, AiHubChatKind kind) {

        if (log.isDebugEnabled()) {
            Class<?> targetClass = target.getClass();

            log.debug(
                "AiHubRoutingAgent dispatching threadId={} kind={} to {}", parameters.getThreadId(), kind,
                targetClass.getSimpleName());
        }

        return target.runAgent(parameters, subscriber);
    }

    private AiHubChatKind resolveKind(@Nullable String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return AiHubChatKind.STANDARD;
        }

        return chatService.findByThreadId(threadId)
            .map(AiHubChat::getKind)
            .orElse(AiHubChatKind.STANDARD);
    }
}
