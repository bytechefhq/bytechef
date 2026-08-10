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

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
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

    @Test
    void testContributeSkipsAdvisorWhenGuardrailsInactive() {
        AiGuardrails aiGuardrails = mock(AiGuardrails.class);
        AiGuardrailMetrics aiGuardrailMetrics = mock(AiGuardrailMetrics.class);
        ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);

        when(aiGuardrails.isActive(any())).thenReturn(false);

        WorkspaceAdvisorContributor contributor = new WorkspaceAdvisorContributor(
            aiGuardrails, aiGuardrailMetrics, null);

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, Map.of());

        assertThat(result).isSameAs(chatClientRequestSpec);

        verify(chatClientRequestSpec, never()).advisors(any(Advisor[].class));
    }
}
