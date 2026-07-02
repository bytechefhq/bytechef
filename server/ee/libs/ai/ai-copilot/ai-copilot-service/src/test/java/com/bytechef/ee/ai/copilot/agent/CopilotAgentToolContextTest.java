/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.state.State;
import com.bytechef.ee.ai.copilot.constant.CopilotConstants;
import com.bytechef.ee.ai.copilot.tool.context.AgentToolInvocationContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CopilotAgentToolContextTest {

    @Test
    void testCodeEditorAgentPublishesCallerIdentity() throws Exception {
        CodeEditorSpringAIAgent agent = CodeEditorSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        assertPublishesUserId(agent.toolContext(newInputWithUserId()));
    }

    @Test
    void testClusterElementAgentPublishesCallerIdentity() throws Exception {
        ClusterElementSpringAIAgent agent = ClusterElementSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        assertPublishesUserId(agent.toolContext(newInputWithUserId()));
    }

    @Test
    void testSkillsAgentPublishesCallerIdentity() throws Exception {
        SkillsSpringAIAgent agent = SkillsSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        assertPublishesUserId(agent.toolContext(newInputWithUserId()));
    }

    @Test
    void testConverterAgentPublishesCallerIdentity() throws Exception {
        ConverterSpringAIAgent agent = ConverterSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .build();

        assertPublishesUserId(agent.toolContext(newInputWithUserId()));
    }

    private static void assertPublishesUserId(Map<String, Object> toolContext) {
        assertThat(toolContext).containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 99L);
    }

    private static RunAgentInput newInputWithUserId() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 99L);

        return newInput(stateMap);
    }

    private static RunAgentInput newInput(Map<String, Object> stateMap) {
        return new RunAgentInput(null, null, new State(stateMap), null, null, null, null);
    }
}
