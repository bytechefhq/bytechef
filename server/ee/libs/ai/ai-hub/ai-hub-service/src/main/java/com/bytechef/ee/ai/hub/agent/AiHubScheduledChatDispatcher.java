/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Non-HTTP entry point for posting a user turn to an AI Hub agent. Used by the scheduled-run listener — a scheduler
 * fires, the listener creates a task row, then calls this dispatcher with the resolved identity and prompt.
 *
 * <p>
 * Synthesizes an {@link AgUiParameters} that mirrors what {@code AiHubApiController.chat(...)} builds after the HTTP
 * auth and ownership checks, then delegates to {@link AiHubChatStreamer#runAgent}. The returned
 * {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter SseEmitter} is discarded — the agent runs to
 * completion and writes its output to {@code SPRING_AI_CHAT_MEMORY} regardless of whether any HTTP client is listening
 * to the stream.
 * </p>
 *
 * <p>
 * State-key injection mirrors {@code AiHubApiController.injectAuthenticatedContext}: both the server-controlled
 * {@link AiHubStateKeys} keys and the plain convenience aliases ({@code "userId"}, {@code "workspaceId"},
 * {@code "threadId"}) are populated so routing-agent and tool-callback code that reads either key form sees consistent
 * values. The {@code "environmentId"} key is also injected because {@code
 * AiHubSpringAIAgent.buildInvocationContext} reads it for memory-index scoping.
 * </p>
 *
 * <p>
 * The routing agent ({@code ai_hub_ask}) looks up the task row by {@code threadId} to determine the
 * {@link com.bytechef.ee.ai.hub.task.AiHubTaskKind} and, for {@code PERSONAL_AGENT} tasks, fetches the agent's
 * instructions from the database — so the caller does NOT need to inject agent instructions into state. Passing the
 * correct {@code threadId} (the one stamped on the pre-created task row) is sufficient.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AiHubScheduledChatDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AiHubScheduledChatDispatcher.class);

    private final AiHubChatStreamer chatStreamer;
    private final Map<String, LocalAgent> localAgentMap;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubScheduledChatDispatcher(AiHubChatStreamer chatStreamer, List<LocalAgent> localAgents) {
        this.chatStreamer = chatStreamer;
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
    }

    /**
     * Dispatches a scheduled user turn to the {@code ai_hub_ask} agent.
     *
     * @param userId               the authenticated user id whose identity the agent runs under
     * @param workspaceId          the workspace the task belongs to
     * @param environmentId        the environment ordinal (DEVELOPMENT=0, STAGING=1, PRODUCTION=2) for memory-index
     *                             scoping
     * @param aiHubPersonalAgentId the personal-agent id for {@code PERSONAL_AGENT} tasks; unused by this method but
     *                             read by the routing agent from the task row keyed on {@code threadId}
     * @param threadId             the thread id of the pre-created {@code ai_hub_task} row; the routing agent uses this
     *                             to look up the task and determine its kind
     * @param prompt               the user-turn text to submit
     */
    @SuppressWarnings("unused")
    public void dispatch(
        long userId, long workspaceId, int environmentId, long aiHubPersonalAgentId,
        String threadId, String prompt) {

        LocalAgent agent = localAgentMap.get("ai_hub_ask");

        if (agent == null) {
            throw new IllegalStateException(
                "ai_hub_ask agent is not registered; known agents: " + localAgentMap.keySet());
        }

        AgUiParameters agUiParameters = buildParameters(userId, workspaceId, environmentId, threadId, prompt);

        if (log.isDebugEnabled()) {
            log.debug(
                "Dispatching scheduled chat: agent={}, threadId={}, userId={}, workspaceId={}, environmentId={}",
                agent.getAgentId(), threadId, userId, workspaceId, environmentId);
        }

        // The returned SseEmitter is discarded — no HTTP client is listening on a scheduled run.
        // The agent runs to completion and writes to SPRING_AI_CHAT_MEMORY regardless.
        chatStreamer.runAgent(agent, agUiParameters, threadId);
    }

    private static AgUiParameters buildParameters(
        long userId, long workspaceId, int environmentId, String threadId, String prompt) {

        AgUiParameters agUiParameters = new AgUiParameters();

        agUiParameters.setThreadId(threadId);
        agUiParameters.setRunId(UUID.randomUUID()
            .toString());

        UserMessage userMessage = new UserMessage();

        userMessage.setId(UUID.randomUUID()
            .toString());
        userMessage.setContent(prompt);

        agUiParameters.setMessages(List.of(userMessage));
        agUiParameters.setTools(List.of());
        agUiParameters.setContext(List.of());

        State state = new State();

        // Server-controlled keys read by AiHubSpringAIAgent.buildInvocationContext and tool callbacks.
        // These mirror what AiHubApiController.injectAuthenticatedContext writes after verifying
        // workspace membership and task ownership for HTTP requests.
        state.set(AiHubStateKeys.AUTHENTICATED_USER_ID, userId);
        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, workspaceId);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, threadId);

        // Plain convenience aliases — also written by the controller to ensure any code that reads
        // state.userId / state.workspaceId / state.threadId directly still sees server-controlled values.
        state.set("userId", userId);
        state.set("workspaceId", workspaceId);
        state.set("threadId", threadId);

        // environmentId read by AiHubSpringAIAgent.buildInvocationContext for memory-index scoping.
        state.set("environmentId", (long) environmentId);

        // The scheduler's environmentId comes from the persisted task row (server-trusted), so it doubles as the
        // verified key the catalog resolver reads — keeping scheduled AI Hub runs able to resolve catalog models.
        state.set(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID, (long) environmentId);

        agUiParameters.setState(state);

        return agUiParameters;
    }
}
