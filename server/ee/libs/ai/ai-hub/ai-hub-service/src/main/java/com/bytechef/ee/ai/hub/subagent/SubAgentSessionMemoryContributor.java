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
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubAgentSessionMemoryContributor implements SubAgentAdvisorContributor {

    public static final int MAX_EVENTS = 10;

    private static final Logger log = LoggerFactory.getLogger(SubAgentSessionMemoryContributor.class);

    private final AiHubSessionMemory aiHubSessionMemory;
    private final String agentTypeKey;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SubAgentSessionMemoryContributor(AiHubSessionMemory aiHubSessionMemory, String agentTypeKey) {
        this.aiHubSessionMemory = aiHubSessionMemory;
        this.agentTypeKey = agentTypeKey;
    }

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
