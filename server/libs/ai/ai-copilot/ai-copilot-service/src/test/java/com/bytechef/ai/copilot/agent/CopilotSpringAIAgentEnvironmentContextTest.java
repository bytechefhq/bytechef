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

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
class CopilotSpringAIAgentEnvironmentContextTest {

    @BeforeEach
    @AfterEach
    void clearEnvironmentContext() {
        EnvironmentContext.clear();
    }

    @Test
    void testRunWithEnvironmentBindsEnvironmentFromState() throws AGUIException {
        CopilotSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 0L);

        AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

        agent.runWithEnvironment(
            newInput(state), () -> capturedEnvironment.set(EnvironmentContext.getCurrentEnvironment()));

        assertThat(capturedEnvironment.get()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
    }

    @Test
    void testRunWithEnvironmentRestoresPreviousEnvironment() throws AGUIException {
        CopilotSpringAIAgent agent = newAgent();

        EnvironmentContext.set(Environment.STAGING);

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, 0L);

        agent.runWithEnvironment(newInput(state), () -> {});

        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isEqualTo(Environment.STAGING);
    }

    @Test
    void testRunWithEnvironmentLeavesContextUntouchedWhenStateHasNoEnvironment() throws AGUIException {
        CopilotSpringAIAgent agent = newAgent();

        AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

        agent.runWithEnvironment(
            newInput(new State()), () -> capturedEnvironment.set(EnvironmentContext.fetchCurrentEnvironment()));

        assertThat(capturedEnvironment.get()).isNull();
        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
    }

    private static RunAgentInput newInput(State state) {
        return new RunAgentInput("thread", "run", state, List.of(), List.of(), List.of(), null);
    }

    private static CopilotSpringAIAgent newAgent() throws AGUIException {
        return ConverterSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .build();
    }
}
