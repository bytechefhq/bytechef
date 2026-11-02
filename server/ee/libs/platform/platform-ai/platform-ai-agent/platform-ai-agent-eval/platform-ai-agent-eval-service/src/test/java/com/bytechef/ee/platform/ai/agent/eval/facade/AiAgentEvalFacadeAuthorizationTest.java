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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Evals belong to an AI agent node of a workflow, and a run executes the agent's model with the workflow's test
 * connections, so every change needs {@code WORKFLOW_EDIT} on the workflow the eval belongs to.
 *
 * @author Ivica Cardic
 */
class AiAgentEvalFacadeAuthorizationTest {

    private static final String IN_DEVELOPMENT = "'WORKFLOW_EDIT', "
        + "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)";

    @Test
    void testEveryEvalChangeNeedsTheWorkflowEditScopeOfTheEvalsWorkflow() {
        assertThat(getGates(AiAgentEvalFacadeImpl.class)).isEqualTo(Map.ofEntries(
            Map.entry("createAiAgentEvalScenario", resolved("AiAgentEvalTest", "#agentEvalTestId")),
            Map.entry(
                "createAiAgentEvalTest",
                "hasWorkflowScopeIfProjectWorkflowInEnvironment(#workflowId, " + IN_DEVELOPMENT),
            Map.entry(
                "createAiAgentJudge", "hasWorkflowScopeIfProjectWorkflowInEnvironment(#workflowId, " + IN_DEVELOPMENT),
            Map.entry("createAiAgentScenarioJudge", resolved("AiAgentEvalScenario", "#agentEvalScenarioId")),
            Map.entry(
                "createAiAgentScenarioToolSimulation", resolved("AiAgentEvalScenario", "#agentEvalScenarioId")),
            Map.entry("deleteAiAgentEvalScenario", resolved("AiAgentEvalScenario", "#id")),
            Map.entry("deleteAiAgentEvalTest", resolved("AiAgentEvalTest", "#id")),
            Map.entry("deleteAiAgentJudge", resolved("AiAgentJudge", "#id")),
            Map.entry("deleteAiAgentScenarioJudge", resolved("AiAgentScenarioJudge", "#id")),
            Map.entry("deleteAiAgentScenarioToolSimulation", resolved("AiAgentScenarioToolSimulation", "#id")),
            Map.entry("updateAiAgentEvalScenario", resolved("AiAgentEvalScenario", "#id")),
            Map.entry("updateAiAgentEvalTest", resolved("AiAgentEvalTest", "#id")),
            Map.entry("updateAiAgentJudge", resolved("AiAgentJudge", "#id")),
            Map.entry("updateAiAgentScenarioJudge", resolved("AiAgentScenarioJudge", "#id")),
            Map.entry("updateAiAgentScenarioToolSimulation", resolved("AiAgentScenarioToolSimulation", "#id"))));
    }

    @Test
    void testARunNeedsTheWorkflowEditScopeInTheEnvironmentItRunsIn() {
        // Recovering orphaned runs happens at startup, with no user to check.
        assertThat(getGates(AiAgentEvalRunFacadeImpl.class)).isEqualTo(Map.of(
            "cancelEvalRun",
            "hasWorkflowScopeIfProjectWorkflowInEnvironmentId("
                + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalRun', #id), 'WORKFLOW_EDIT', "
                + "@aiAgentEvalWorkflowResolver.getEnvironmentId('AiAgentEvalRun', #id))",
            "startEvalRun",
            "hasWorkflowScopeIfProjectWorkflowInEnvironmentId("
                + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalTest', #agentEvalTestId), "
                + "'WORKFLOW_EDIT', #environmentId)"));
    }

    private static Map<String, String> getGates(Class<?> facadeClass) {
        return Arrays.stream(facadeClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .collect(Collectors.toMap(
                Method::getName, method -> method.getAnnotation(PreAuthorize.class)
                    .value()));
    }

    private static String resolved(String resourceType, String idExpression) {
        return "hasWorkflowScopeIfProjectWorkflowInEnvironment(@aiAgentEvalWorkflowResolver.getWorkflowId('"
            + resourceType + "', " + idExpression + "), " + IN_DEVELOPMENT;
    }
}
