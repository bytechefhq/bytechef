/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;

/**
 * Gives a specialist subagent a durable, per-conversation memory so a multi-turn refinement does not restart from zero
 * on every delegation: without it the specialist never sees its own previous output, and {@code mcp_agent} re-drafts a
 * tool mapping from scratch when the user says "make it shorter".
 *
 * <p>
 * The session is keyed {@code <parentThreadId>:<agentTypeKey>} over the same session store the parent AI Hub agent
 * uses, under {@link AiHubSessionMemory#SESSION_USER_ID}. Every specialist on one conversation therefore gets its own
 * isolated memory, and the same specialist on two conversations never crosses them — a bug in
 * {@link #sessionKey(String, String)} would leak one user's conversation into another's, which is why the derivation is
 * a single public method with its own unit test rather than an inlined concatenation.
 * </p>
 *
 * <p>
 * The conversation id cannot be baked in at construction: the delegate {@code ChatClient} beans are process-wide
 * singletons shared by every workspace <i>and every conversation</i>. It is resolved per request from the
 * {@code ToolContext} the delegate forwards from the parent — the same map the workspace contributor reads.
 * </p>
 *
 * <p>
 * The advisor is built with the default precedence, deliberately unlike the parent agent (which uses
 * {@code ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER} to capture its full tool transcript). Default
 * precedence places this advisor <b>outside</b> the tool-calling loop, so only user and assistant turns are persisted:
 * a specialist's tool traffic is mostly listing calls it re-runs anyway, and persisting it would balloon the memory
 * against the turn budget for little benefit.
 * </p>
 *
 * <p>
 * {@link #MAX_EVENTS} bounds what is <b>loaded</b>, not what is kept — {@code EventFilter} is a read filter, so the
 * full history stays in the store. Compaction would also bound replay but permanently rewrites stored history; since
 * bounding cost is the goal and losing history is not, the read filter is the better lever.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubAgentSessionMemoryContributor implements SubAgentAdvisorContributor {

    /**
     * How many session events a delegation replays — roughly five exchanges. A constant rather than a configuration
     * property: there is no evidence yet that operators need to tune it, and a property is easy to add later.
     */
    public static final int MAX_EVENTS = 10;

    private static final Logger log = LoggerFactory.getLogger(SubAgentSessionMemoryContributor.class);

    private final AiHubSessionMemory aiHubSessionMemory;
    private final String agentTypeKey;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SubAgentSessionMemoryContributor(AiHubSessionMemory aiHubSessionMemory, String agentTypeKey) {
        this.aiHubSessionMemory = aiHubSessionMemory;
        this.agentTypeKey = agentTypeKey;
    }

    /**
     * The session id a specialist's memory lives under. Public so the purge that runs when an AI Hub chat is deleted
     * constructs exactly the same keys this contributor writes — {@code SessionRepository} has no prefix listing, so
     * the two cannot be allowed to drift apart.
     */
    public static String sessionKey(String threadId, String agentTypeKey) {
        return threadId + ":" + agentTypeKey;
    }

    @Override
    public ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext) {

        String conversationId = resolveConversationId(toolContext);

        if (conversationId == null || conversationId.isBlank()) {
            return chatClientRequestSpec;
        }

        String sessionId = sessionKey(conversationId, agentTypeKey);

        try {
            SessionMemoryAdvisor sessionMemoryAdvisor = SessionMemoryAdvisor
                .builder(aiHubSessionMemory.sessionService())
                .defaultUserId(AiHubSessionMemory.SESSION_USER_ID)
                .eventFilter(EventFilter.lastN(MAX_EVENTS))
                .build();

            return chatClientRequestSpec
                .advisors(sessionMemoryAdvisor)
                .advisors(advisorSpec -> advisorSpec.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId));
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to attach subagent session memory for sessionId {}; continuing stateless", sessionId,
                exception);

            return chatClientRequestSpec;
        }
    }

    private static @Nullable String resolveConversationId(@Nullable Map<String, Object> toolContext) {
        if (toolContext == null || toolContext.isEmpty()) {
            return null;
        }

        AgentToolInvocationContext context = AgentToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        return context == null ? null : context.conversationId();
    }
}
