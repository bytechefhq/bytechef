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

package com.bytechef.component.ai.agenticai.embabel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.embabel.agent.core.AgentPlatform;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers the five pre-platform validation rules in {@link EmbabelAgentRunner#run}. These rules encode the safety
 * guarantees called out in the commit history (unreachable goal, unreachable entry point, duplicate action names) and
 * must fail fast with actionable messages instead of deferring to an opaque Embabel runtime error.
 *
 * @author Ivica Cardic
 */
class EmbabelAgentRunnerValidationTest {

    private static final String ACTION_NAME = "generate-result";
    private static final String ACTION_DESCRIPTION = "Generate the final result";
    private static final String ACTION_PROMPT = "Produce the requested output";
    private static final String USER_GOAL_BINDING = "userGoal";
    private static final String RESULT_BINDING = "result";
    private static final double DEFAULT_COST = 1.0;

    private final AgentPlatform agentPlatform = mock(AgentPlatform.class);
    private final EmbabelAgentRunner runner = new EmbabelAgentRunner(agentPlatform);

    @Test
    void testEmptyActionStepsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> runner.run(List.of(), "goal", RESULT_BINDING, false, null));

        assertThat(exception.getMessage()).contains("action step");
    }

    @Test
    void testBlankGoalOutputBindingRejected() {
        List<ActionStep> actionSteps = List.of(
            new ActionStep(
                ACTION_NAME, ACTION_DESCRIPTION, ACTION_PROMPT, USER_GOAL_BINDING, RESULT_BINDING, List.of(),
                DEFAULT_COST));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> runner.run(actionSteps, "goal", "   ", false, null));

        assertThat(exception.getMessage()).contains("goalOutputBinding");
    }

    @Test
    void testUnreachableGoalOutputBindingRejected() {
        List<ActionStep> actionSteps = List.of(
            new ActionStep(
                ACTION_NAME, ACTION_DESCRIPTION, ACTION_PROMPT, USER_GOAL_BINDING, "intermediate", List.of(),
                DEFAULT_COST));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> runner.run(actionSteps, "goal", RESULT_BINDING, false, null));

        assertThat(exception.getMessage()).contains(RESULT_BINDING);
    }

    @Test
    void testMissingUserGoalEntryPointRejected() {
        List<ActionStep> actionSteps = List.of(
            new ActionStep(
                ACTION_NAME, ACTION_DESCRIPTION, ACTION_PROMPT, "unreachable", RESULT_BINDING, List.of(),
                DEFAULT_COST));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> runner.run(actionSteps, "goal", RESULT_BINDING, false, null));

        assertThat(exception.getMessage()).contains(USER_GOAL_BINDING);
    }

    @Test
    void testDuplicateActionNamesRejected() {
        List<ActionStep> actionSteps = List.of(
            new ActionStep(
                "same", ACTION_DESCRIPTION, ACTION_PROMPT, USER_GOAL_BINDING, RESULT_BINDING, List.of(), DEFAULT_COST),
            new ActionStep(
                "same", ACTION_DESCRIPTION, ACTION_PROMPT, USER_GOAL_BINDING, RESULT_BINDING, List.of(),
                DEFAULT_COST));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> runner.run(actionSteps, "goal", RESULT_BINDING, false, null));

        assertThat(exception.getMessage()).contains("Duplicate");
    }
}
