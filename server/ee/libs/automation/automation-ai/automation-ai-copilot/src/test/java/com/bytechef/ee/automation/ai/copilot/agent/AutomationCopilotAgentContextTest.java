/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.CopilotSpringAIAgent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Both agents were moved from extending {@code SpringAIAgent} directly onto {@link CopilotSpringAIAgent} so that the
 * tenant binding applied on the agent worker thread reaches them; without that parent their runs read the
 * {@code public} schema in a multi-tenant deployment.
 *
 * <p>
 * Asserting the parent is the whole test on this side, and it is sufficient rather than merely convenient:
 * {@code CopilotSpringAIAgent.run} is {@code final}, so being on that base IS being inside the tenant and environment
 * bindings — an agent here cannot override {@code run} and step around them. (An earlier version of this comment
 * claimed the seam methods were unreachable from an out-of-package test. That was wrong — these tests share a package
 * with their agents and {@code run} is {@code protected} — and it is moot now that the base seals {@code run}.) The
 * binding behaviour itself belongs to {@code CopilotSpringAIAgent} and is covered in its own package by
 * {@code CopilotSpringAIAgentTenantContextTest}, which also pins the {@code final}.
 *
 * <p>
 * Reparenting also binds {@code EnvironmentContext} from the client-supplied {@code environmentId} for these agents,
 * which moves their chat-model resolution off the {@code PRODUCTION} fallback — see
 * {@code ReparentedCopilotAgentContextTest} for why that is a behaviour change rather than a no-op.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationCopilotAgentContextTest {

    @Test
    void testCodeWorkflowAgentIsOnTheSharedBase() throws AGUIException {
        CodeWorkflowSpringAIAgent agent = CodeWorkflowSpringAIAgent.builder()
            .agentId("code_workflow_ask")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .build();

        assertThat(agent).isInstanceOf(CopilotSpringAIAgent.class);
    }

    @Test
    void testCustomComponentAgentIsOnTheSharedBase() throws AGUIException {
        CustomComponentSpringAIAgent agent = CustomComponentSpringAIAgent.builder()
            .agentId("custom_component_ask")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .build();

        assertThat(agent).isInstanceOf(CopilotSpringAIAgent.class);
    }
}
