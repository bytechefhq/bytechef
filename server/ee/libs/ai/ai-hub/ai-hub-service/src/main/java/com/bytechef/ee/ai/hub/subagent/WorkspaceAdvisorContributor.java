/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.advisor.AiGuardrailsAdvisor;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.ee.platform.ai.workspaceprompt.advisor.WorkspaceSystemPromptAdvisor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Attaches the calling workspace's guardrails advisor and system-prompt advisor to a subagent delegate request.
 * Extracted verbatim from {@code SubAgentGuardrailedChatClient} when that decorator was generalized to a list of
 * contributors; the conditions are unchanged, including the null-workspace tenant-default fallback.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class WorkspaceAdvisorContributor implements SubAgentAdvisorContributor {

    private final @Nullable AiGuardrails aiGuardrails;
    private final @Nullable AiGuardrailMetrics aiGuardrailMetrics;
    private final @Nullable WorkspaceSystemPrompts workspaceSystemPrompts;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceAdvisorContributor(
        @Nullable AiGuardrails aiGuardrails, @Nullable AiGuardrailMetrics aiGuardrailMetrics,
        @Nullable WorkspaceSystemPrompts workspaceSystemPrompts) {

        this.aiGuardrails = aiGuardrails;
        this.aiGuardrailMetrics = aiGuardrailMetrics;
        this.workspaceSystemPrompts = workspaceSystemPrompts;
    }

    @Override
    public ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext) {

        Long workspaceId = resolveWorkspaceId(toolContext);

        ChatClientRequestSpec resultSpec = chatClientRequestSpec;

        if (aiGuardrails != null && aiGuardrailMetrics != null && aiGuardrails.isActive(workspaceId)) {
            resultSpec = resultSpec.advisors(new AiGuardrailsAdvisor(aiGuardrails, workspaceId, aiGuardrailMetrics));
        }

        if (workspaceSystemPrompts != null && workspaceSystemPrompts.fetchPrompt(workspaceId) != null) {
            resultSpec = resultSpec.advisors(new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, workspaceId));
        }

        return resultSpec;
    }

    private static @Nullable Long resolveWorkspaceId(@Nullable Map<String, Object> toolContext) {
        if (toolContext == null || toolContext.isEmpty()) {
            return null;
        }

        AgentToolInvocationContext context = AgentToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        return context == null ? null : context.workspaceId();
    }
}
