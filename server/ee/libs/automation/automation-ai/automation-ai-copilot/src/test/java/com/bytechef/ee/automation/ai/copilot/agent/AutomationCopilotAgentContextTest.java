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
 * @version ee
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
