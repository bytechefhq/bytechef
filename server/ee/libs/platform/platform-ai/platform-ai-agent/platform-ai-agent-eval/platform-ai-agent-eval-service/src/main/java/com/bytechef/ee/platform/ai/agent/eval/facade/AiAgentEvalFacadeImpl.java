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

import com.bytechef.ee.platform.ai.agent.eval.audit.AiAgentEvalAuditEvents;
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
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_EVAL_SCENARIO_CREATED, data = {
        @AuditData(key = "aiAgentEvalScenarioId", value = "#result?.id"),
        @AuditData(key = "aiAgentEvalTestId", value = "#agentEvalTestId"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "type", value = "#type")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_EVAL_TEST_CREATED, data = {
        @AuditData(key = "aiAgentEvalTestId", value = "#result?.id"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "workflowId", value = "#workflowId"),
        @AuditData(key = "workflowNodeName", value = "#workflowNodeName")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_JUDGE_CREATED, data = {
        @AuditData(key = "aiAgentJudgeId", value = "#result?.id"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "type", value = "#type"),
        @AuditData(key = "workflowId", value = "#workflowId"),
        @AuditData(key = "workflowNodeName", value = "#workflowNodeName")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_JUDGE_CREATED, data = {
        @AuditData(key = "aiAgentScenarioJudgeId", value = "#result?.id"),
        @AuditData(key = "aiAgentEvalScenarioId", value = "#agentEvalScenarioId"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "type", value = "#type")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_TOOL_SIMULATION_CREATED, data = {
        @AuditData(key = "aiAgentScenarioToolSimulationId", value = "#result?.id"),
        @AuditData(key = "aiAgentEvalScenarioId", value = "#agentEvalScenarioId"),
        @AuditData(key = "toolName", value = "#toolName")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_EVAL_SCENARIO_DELETED, data = {
        @AuditData(key = "aiAgentEvalScenarioId", value = "#id")
    })
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalScenario', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentEvalScenario(long id) {
        agentEvalScenarioService.deleteAiAgentEvalScenario(id);
    }

    @Override
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_EVAL_TEST_DELETED, data = {
        @AuditData(key = "aiAgentEvalTestId", value = "#id")
    })
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentEvalTest', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentEvalTest(long id) {
        agentEvalTestService.deleteAiAgentEvalTest(id);
    }

    @Override
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_JUDGE_DELETED, data = {
        @AuditData(key = "aiAgentJudgeId", value = "#id")
    })
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentJudge', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentJudge(long id) {
        agentJudgeService.deleteAiAgentJudge(id);
    }

    @Override
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_JUDGE_DELETED, data = {
        @AuditData(key = "aiAgentScenarioJudgeId", value = "#id")
    })
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentScenarioJudge', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentScenarioJudge(long id) {
        agentScenarioJudgeService.deleteAiAgentScenarioJudge(id);
    }

    @Override
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_TOOL_SIMULATION_DELETED, data = {
        @AuditData(key = "aiAgentScenarioToolSimulationId", value = "#id")
    })
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironment("
        + "@aiAgentEvalWorkflowResolver.getWorkflowId('AiAgentScenarioToolSimulation', #id), "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void deleteAiAgentScenarioToolSimulation(long id) {
        agentScenarioToolSimulationService.deleteAiAgentScenarioToolSimulation(id);
    }

    @Override
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_EVAL_SCENARIO_UPDATED, data = {
        @AuditData(key = "aiAgentEvalScenarioId", value = "#id"),
        @AuditData(key = "name", value = "#result?.name")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_EVAL_TEST_UPDATED, data = {
        @AuditData(key = "aiAgentEvalTestId", value = "#id"),
        @AuditData(key = "name", value = "#result?.name")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_JUDGE_UPDATED, data = {
        @AuditData(key = "aiAgentJudgeId", value = "#id"),
        @AuditData(key = "name", value = "#result?.name")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_JUDGE_UPDATED, data = {
        @AuditData(key = "aiAgentScenarioJudgeId", value = "#id"),
        @AuditData(key = "name", value = "#result?.name")
    })
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
    @Audited(event = AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_TOOL_SIMULATION_UPDATED, data = {
        @AuditData(key = "aiAgentScenarioToolSimulationId", value = "#id"),
        @AuditData(key = "toolName", value = "#result?.toolName")
    })
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
