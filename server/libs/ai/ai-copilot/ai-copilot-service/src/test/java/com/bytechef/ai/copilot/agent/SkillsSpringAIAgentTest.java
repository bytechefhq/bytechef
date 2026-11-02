/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.agui.core.message.SystemMessage;
import com.agui.core.state.State;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
class SkillsSpringAIAgentTest {

    @Test
    void testSystemMessageCarriesCreateSkillIntent() throws AGUIException {
        State state = new State();

        state.set("parameters", Map.of("intent", "create_skill"));

        SystemMessage systemMessage = buildAgent().createSystemMessage(state, List.of());

        assertThat(systemMessage.getContent()).contains("open by asking what the skill should do");
    }

    @Test
    void testSystemMessageOmitsIntentWhenParametersAbsent() throws AGUIException {
        SystemMessage systemMessage = buildAgent().createSystemMessage(new State(), List.of());

        assertThat(systemMessage.getContent()).doesNotContain("open by asking what the skill should do");
    }

    @Test
    void testSystemMessageDoesNotContainLiteralFormatSpecifier() throws AGUIException {
        State state = new State();

        state.set("parameters", Map.of("intent", "create_skill"));

        SystemMessage systemMessage = buildAgent().createSystemMessage(state, List.of());

        assertThat(systemMessage.getContent()).doesNotContain("%n");
    }

    @Test
    void testSystemMessageOmitsBuildInstructionInAskMode() throws AGUIException {
        State state = new State();

        state.set("parameters", Map.of("intent", "create_skill"));

        SystemMessage systemMessage = buildAgent("skills_ask").createSystemMessage(state, List.of());

        assertThat(systemMessage.getContent()).doesNotContain("open by asking what the skill should do");
    }

    private static SkillsSpringAIAgent buildAgent() throws AGUIException {
        return buildAgent("test");
    }

    private static SkillsSpringAIAgent buildAgent(String agentId) throws AGUIException {
        return SkillsSpringAIAgent.builder()
            .agentId(agentId)
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .build();
    }
}
