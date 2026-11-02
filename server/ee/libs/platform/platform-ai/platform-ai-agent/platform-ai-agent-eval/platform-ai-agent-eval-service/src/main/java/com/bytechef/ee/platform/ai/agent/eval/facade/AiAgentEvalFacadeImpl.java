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
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalScenarioService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalTestService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentJudgeService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentScenarioJudgeService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentScenarioToolSimulationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Judges, tests, scenarios and tool simulations belong to an AI agent node of a workflow; changing them needs
 * {@code WORKFLOW_EDIT} on that workflow in development, where they are edited.
 *
 * @author Ivica Cardic
 */
@Component
@Transactional
class AiAgentEvalFacadeImpl implements AiAgentEvalFacade {

    private static final String DEVELOPMENT = "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT";

    private final AiAgentEvalScenarioService agentEvalScenarioService;
    private final AiAgentEvalTestService agentEvalTestService;
    private final AiAgentJudgeService agentJudgeService;
    private final AiAgentScenarioJudgeService agentScenarioJudgeService;
    private final AiAgentScenarioToolSimulationService agentScenarioToolSimulationService;

    @SuppressFBWarnings("EI")
    AiAgentEvalFacadeImpl(
        AiAgentEvalScenarioService agentEvalScenarioService, AiAgentEvalTestService agentEvalTestService,
        AiAgentJudgeService agentJudgeService, AiAgentScenarioJudgeService agentScenarioJudgeService,
        AiAgentScenarioToolSimulationService agentScenarioToolSimulationService) {

        this.agentEvalScenarioService = agentEvalScenarioService;
        this.agentEvalTestService = agentEvalTestService;
        this.agentJudgeService = agentJudgeService;
        this.agentScenarioJudgeService = agentScenarioJudgeService;
        this.agentScenarioToolSimulationService = agentScenarioToolSimulationService;
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalTest', #agentEvalTestId), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentEvalScenario createAiAgentEvalScenario(
        long agentEvalTestId, String name, AiAgentScenarioType type, String userMessage, String expectedOutput,
        String personaPrompt, Integer maxTurns, Integer numberOfRuns) {

        AiAgentEvalScenario aiAgentEvalScenario = new AiAgentEvalScenario();

        aiAgentEvalScenario.setAgentEvalTestId(agentEvalTestId);
        aiAgentEvalScenario.setName(name);
        aiAgentEvalScenario.setType(type);
        aiAgentEvalScenario.setUserMessage(userMessage);
        aiAgentEvalScenario.setExpectedOutput(expectedOutput);
        aiAgentEvalScenario.setPersonaPrompt(personaPrompt);

        if (maxTurns != null) {
            aiAgentEvalScenario.setMaxTurns(maxTurns);
        }

        if (numberOfRuns != null) {
            aiAgentEvalScenario.setNumberOfRuns(numberOfRuns);
        }

        return agentEvalScenarioService.createAiAgentEvalScenario(aiAgentEvalScenario);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment(#workflowId, 'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentEvalTest createAiAgentEvalTest(
        String workflowId, String workflowNodeName, String name, String description) {

        AiAgentEvalTest aiAgentEvalTest = new AiAgentEvalTest();

        aiAgentEvalTest.setWorkflowId(workflowId);
        aiAgentEvalTest.setWorkflowNodeName(workflowNodeName);
        aiAgentEvalTest.setName(name);
        aiAgentEvalTest.setDescription(description);

        return agentEvalTestService.createAiAgentEvalTest(aiAgentEvalTest);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment(#workflowId, 'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentJudge createAiAgentJudge(
        String workflowId, String workflowNodeName, String name, AiAgentJudgeType type,
        Map<String, Object> configuration) {

        AiAgentJudge agentJudge = new AiAgentJudge();

        agentJudge.setWorkflowId(workflowId);
        agentJudge.setWorkflowNodeName(workflowNodeName);
        agentJudge.setName(name);
        agentJudge.setType(type);
        agentJudge.setConfiguration(configuration);

        return agentJudgeService.createAiAgentJudge(agentJudge);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalScenario', #agentEvalScenarioId), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentScenarioJudge createAiAgentScenarioJudge(
        long agentEvalScenarioId, String name, AiAgentJudgeType type, Map<String, Object> configuration) {

        AiAgentScenarioJudge agentScenarioJudge = new AiAgentScenarioJudge();

        agentScenarioJudge.setAgentEvalScenarioId(agentEvalScenarioId);
        agentScenarioJudge.setName(name);
        agentScenarioJudge.setType(type);
        agentScenarioJudge.setConfiguration(configuration);

        return agentScenarioJudgeService.createAiAgentScenarioJudge(agentScenarioJudge);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalScenario', #agentEvalScenarioId), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentScenarioToolSimulation createAiAgentScenarioToolSimulation(
        long agentEvalScenarioId, String toolName, String responsePrompt, String simulationModel) {

        AiAgentScenarioToolSimulation toolSimulation = new AiAgentScenarioToolSimulation();

        toolSimulation.setAgentEvalScenarioId(agentEvalScenarioId);
        toolSimulation.setToolName(toolName);
        toolSimulation.setResponsePrompt(responsePrompt);
        toolSimulation.setSimulationModel(simulationModel);

        return agentScenarioToolSimulationService.createAiAgentScenarioToolSimulation(toolSimulation);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalScenario', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentEvalScenario(long id) {
        agentEvalScenarioService.deleteAiAgentEvalScenario(id);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalTest', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentEvalTest(long id) {
        agentEvalTestService.deleteAiAgentEvalTest(id);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentJudge', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentJudge(long id) {
        agentJudgeService.deleteAiAgentJudge(id);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentScenarioJudge', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentScenarioJudge(long id) {
        agentScenarioJudgeService.deleteAiAgentScenarioJudge(id);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentScenarioToolSimulation', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentScenarioToolSimulation(long id) {
        agentScenarioToolSimulationService.deleteAiAgentScenarioToolSimulation(id);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalScenario', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentEvalScenario updateAiAgentEvalScenario(
        long id, String name, String userMessage, String expectedOutput, String personaPrompt, Integer maxTurns,
        Integer numberOfRuns) {

        AiAgentEvalScenario aiAgentEvalScenario = agentEvalScenarioService.getAgentEvalScenario(id);

        if (name != null) {
            aiAgentEvalScenario.setName(name);
        }

        if (userMessage != null) {
            aiAgentEvalScenario.setUserMessage(userMessage);
        }

        if (expectedOutput != null) {
            aiAgentEvalScenario.setExpectedOutput(expectedOutput);
        }

        if (personaPrompt != null) {
            aiAgentEvalScenario.setPersonaPrompt(personaPrompt);
        }

        if (maxTurns != null) {
            aiAgentEvalScenario.setMaxTurns(maxTurns);
        }

        if (numberOfRuns != null) {
            aiAgentEvalScenario.setNumberOfRuns(numberOfRuns);
        }

        return agentEvalScenarioService.updateAiAgentEvalScenario(aiAgentEvalScenario);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalTest', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentEvalTest updateAiAgentEvalTest(long id, String name, String description) {
        AiAgentEvalTest aiAgentEvalTest = agentEvalTestService.getAgentEvalTest(id);

        if (name != null) {
            aiAgentEvalTest.setName(name);
        }

        if (description != null) {
            aiAgentEvalTest.setDescription(description);
        }

        return agentEvalTestService.updateAiAgentEvalTest(aiAgentEvalTest);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentJudge', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentJudge updateAiAgentJudge(long id, String name, Map<String, Object> configuration) {
        AiAgentJudge agentJudge = agentJudgeService.getAgentJudge(id);

        if (name != null) {
            agentJudge.setName(name);
        }

        if (configuration != null) {
            agentJudge.setConfiguration(configuration);
        }

        return agentJudgeService.updateAiAgentJudge(agentJudge);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentScenarioJudge', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentScenarioJudge updateAiAgentScenarioJudge(long id, String name, Map<String, Object> configuration) {
        AiAgentScenarioJudge agentScenarioJudge = agentScenarioJudgeService.getAgentScenarioJudge(id);

        if (name != null) {
            agentScenarioJudge.setName(name);
        }

        if (configuration != null) {
            agentScenarioJudge.setConfiguration(configuration);
        }

        return agentScenarioJudgeService.updateAiAgentScenarioJudge(agentScenarioJudge);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentScenarioToolSimulation', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public AiAgentScenarioToolSimulation updateAiAgentScenarioToolSimulation(
        long id, String toolName, String responsePrompt, String simulationModel) {

        AiAgentScenarioToolSimulation toolSimulation =
            agentScenarioToolSimulationService.getAgentScenarioToolSimulation(id);

        if (toolName != null) {
            toolSimulation.setToolName(toolName);
        }

        if (responsePrompt != null) {
            toolSimulation.setResponsePrompt(responsePrompt);
        }

        toolSimulation.setSimulationModel(simulationModel);

        return agentScenarioToolSimulationService.updateAiAgentScenarioToolSimulation(toolSimulation);
    }
}
