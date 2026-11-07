/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.advisor.AiGuardrailsAdvisor;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceAdvisorContributorTest {

    private static final long WORKSPACE_ID = 42L;

    private final AiGuardrails aiGuardrails = mock(AiGuardrails.class);
    private final AiGuardrailMetrics aiGuardrailMetrics = mock(AiGuardrailMetrics.class);
    private final ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);
    private final ChatClientRequestSpec advisedChatClientRequestSpec = mock(ChatClientRequestSpec.class);

    @Test
    void testContributeSkipsAdvisorWhenGuardrailsInactive() {
        when(aiGuardrails.isActive(any(AiGuardrailsSettingsTarget.class))).thenReturn(false);

        WorkspaceAdvisorContributor contributor = new WorkspaceAdvisorContributor(aiGuardrails, aiGuardrailMetrics);

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, workspaceToolContext());

        assertThat(result).isSameAs(chatClientRequestSpec);

        verify(chatClientRequestSpec, never()).advisors(any(Advisor[].class));
    }

    @Test
    void testContributeAttachesGuardrailsAdvisorForTheForwardedWorkspace() {
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(true);
        when(chatClientRequestSpec.advisors(any(AiGuardrailsAdvisor.class))).thenReturn(advisedChatClientRequestSpec);

        WorkspaceAdvisorContributor contributor = new WorkspaceAdvisorContributor(aiGuardrails, aiGuardrailMetrics);

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, workspaceToolContext());

        assertThat(result).isSameAs(advisedChatClientRequestSpec);

        verify(chatClientRequestSpec).advisors(any(AiGuardrailsAdvisor.class));
    }

    @Test
    void testContributeResolvesTheTenantDefaultWithoutAToolContext() {
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.platform())).thenReturn(false);

        WorkspaceAdvisorContributor contributor = new WorkspaceAdvisorContributor(aiGuardrails, aiGuardrailMetrics);

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, null);

        assertThat(result).isSameAs(chatClientRequestSpec);

        verify(aiGuardrails).isActive(AiGuardrailsSettingsTarget.platform());
    }

    private static Map<String, Object> workspaceToolContext() {
        return AgentToolInvocationContext.builder()
            .workspaceId(WORKSPACE_ID)
            .build()
            .toToolContext();
    }
}
