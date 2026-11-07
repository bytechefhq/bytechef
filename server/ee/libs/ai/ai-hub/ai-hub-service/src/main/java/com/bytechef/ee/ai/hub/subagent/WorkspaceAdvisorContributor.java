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
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.platform.ai.guardrails.RestorationDestination;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class WorkspaceAdvisorContributor implements SubAgentAdvisorContributor {

    private final AiGuardrails aiGuardrails;
    private final AiGuardrailMetrics aiGuardrailMetrics;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceAdvisorContributor(AiGuardrails aiGuardrails, AiGuardrailMetrics aiGuardrailMetrics) {
        this.aiGuardrails = aiGuardrails;
        this.aiGuardrailMetrics = aiGuardrailMetrics;
    }

    @Override
    public ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext) {

        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(null, resolveWorkspaceId(toolContext));

        if (!aiGuardrails.isActive(target)) {
            return chatClientRequestSpec;
        }

        return chatClientRequestSpec.advisors(
            new AiGuardrailsAdvisor(
                aiGuardrails, target, aiGuardrailMetrics, GuardrailSurface.AI_HUB,
                RestorationDestination.CONVERSATION));
    }

    private static @Nullable Long resolveWorkspaceId(@Nullable Map<String, Object> toolContext) {
        if (toolContext == null || toolContext.isEmpty()) {
            return null;
        }

        AgentToolInvocationContext context = AgentToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        return context == null ? null : context.workspaceId();
    }
}
