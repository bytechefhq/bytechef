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

package com.bytechef.ee.platform.ai.agent.eval.facade;

import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentJudgeType;
import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentScenarioType;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalScenario;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalTest;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentJudge;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentScenarioJudge;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentScenarioToolSimulation;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface AiAgentEvalFacade {

    AiAgentEvalScenario createAiAgentEvalScenario(
        long agentEvalTestId, String name, AiAgentScenarioType type, @Nullable String userMessage,
        @Nullable String expectedOutput, @Nullable String personaPrompt, @Nullable Integer maxTurns,
        @Nullable Integer numberOfRuns);

    AiAgentEvalTest createAiAgentEvalTest(
        String workflowId, String workflowNodeName, String name, @Nullable String description);

    AiAgentJudge createAiAgentJudge(
        String workflowId, String workflowNodeName, String name, AiAgentJudgeType type,
        Map<String, Object> configuration);

    AiAgentScenarioJudge createAiAgentScenarioJudge(
        long agentEvalScenarioId, String name, AiAgentJudgeType type, Map<String, Object> configuration);

    AiAgentScenarioToolSimulation createAiAgentScenarioToolSimulation(
        long agentEvalScenarioId, String toolName, String responsePrompt, @Nullable String simulationModel);

    void deleteAiAgentEvalScenario(long id);

    void deleteAiAgentEvalTest(long id);

    void deleteAiAgentJudge(long id);

    void deleteAiAgentScenarioJudge(long id);

    void deleteAiAgentScenarioToolSimulation(long id);

    AiAgentEvalScenario updateAiAgentEvalScenario(
        long id, @Nullable String name, @Nullable String userMessage, @Nullable String expectedOutput,
        @Nullable String personaPrompt, @Nullable Integer maxTurns, @Nullable Integer numberOfRuns);

    AiAgentEvalTest updateAiAgentEvalTest(long id, @Nullable String name, @Nullable String description);

    AiAgentJudge updateAiAgentJudge(long id, @Nullable String name, @Nullable Map<String, Object> configuration);

    AiAgentScenarioJudge updateAiAgentScenarioJudge(
        long id, @Nullable String name, @Nullable Map<String, Object> configuration);

    AiAgentScenarioToolSimulation updateAiAgentScenarioToolSimulation(
        long id, @Nullable String toolName, @Nullable String responsePrompt, @Nullable String simulationModel);
}
