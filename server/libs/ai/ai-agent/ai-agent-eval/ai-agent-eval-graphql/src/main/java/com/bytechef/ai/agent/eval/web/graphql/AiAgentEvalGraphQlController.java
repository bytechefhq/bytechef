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

package com.bytechef.ai.agent.eval.web.graphql;

import com.bytechef.ai.agent.eval.constant.AiAgentJudgeType;
import com.bytechef.ai.agent.eval.constant.AiAgentScenarioType;
import com.bytechef.ai.agent.eval.domain.AiAgentEvalResult;
import com.bytechef.ai.agent.eval.domain.AiAgentEvalRun;
import com.bytechef.ai.agent.eval.domain.AiAgentEvalScenario;
import com.bytechef.ai.agent.eval.domain.AiAgentEvalTest;
import com.bytechef.ai.agent.eval.domain.AiAgentJudge;
import com.bytechef.ai.agent.eval.domain.AiAgentJudgeVerdict;
import com.bytechef.ai.agent.eval.domain.AiAgentScenarioJudge;
import com.bytechef.ai.agent.eval.domain.AiAgentScenarioToolSimulation;
import com.bytechef.ai.agent.eval.facade.AiAgentEvalRunFacade;
import com.bytechef.ai.agent.eval.file.storage.AiAgentEvalFileStorage;
import com.bytechef.ai.agent.eval.service.AiAgentEvalResultService;
import com.bytechef.ai.agent.eval.service.AiAgentEvalRunService;
import com.bytechef.ai.agent.eval.service.AiAgentEvalScenarioService;
import com.bytechef.ai.agent.eval.service.AiAgentEvalTestService;
import com.bytechef.ai.agent.eval.service.AiAgentJudgeService;
import com.bytechef.ai.agent.eval.service.AiAgentJudgeVerdictService;
import com.bytechef.ai.agent.eval.service.AiAgentScenarioJudgeService;
import com.bytechef.ai.agent.eval.service.AiAgentScenarioToolSimulationService;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@SuppressFBWarnings("EI") // Spring GraphQL controllers intentionally return domain objects for serialization
class AiAgentEvalGraphQlController {

    private final AiAgentEvalFileStorage agentEvalFileStorage;
    private final AiAgentEvalResultService agentEvalResultService;
    private final AiAgentEvalRunFacade agentEvalRunFacade;
    private final AiAgentEvalRunService agentEvalRunService;
    private final AiAgentEvalScenarioService agentEvalScenarioService;
    private final AiAgentEvalTestService agentEvalTestService;
    private final AiAgentJudgeService agentJudgeService;
    private final AiAgentJudgeVerdictService agentJudgeVerdictService;
    private final AiAgentScenarioJudgeService agentScenarioJudgeService;
    private final AiAgentScenarioToolSimulationService agentScenarioToolSimulationService;

    AiAgentEvalGraphQlController(
        AiAgentEvalFileStorage agentEvalFileStorage, AiAgentEvalResultService agentEvalResultService,
        AiAgentEvalRunFacade agentEvalRunFacade, AiAgentEvalRunService agentEvalRunService,
        AiAgentEvalScenarioService agentEvalScenarioService, AiAgentEvalTestService agentEvalTestService,
        AiAgentJudgeService agentJudgeService, AiAgentJudgeVerdictService agentJudgeVerdictService,
        AiAgentScenarioJudgeService agentScenarioJudgeService,
        AiAgentScenarioToolSimulationService agentScenarioToolSimulationService) {

        this.agentEvalFileStorage = agentEvalFileStorage;
        this.agentEvalResultService = agentEvalResultService;
        this.agentEvalRunFacade = agentEvalRunFacade;
        this.agentEvalRunService = agentEvalRunService;
        this.agentEvalScenarioService = agentEvalScenarioService;
        this.agentEvalTestService = agentEvalTestService;
        this.agentJudgeService = agentJudgeService;
        this.agentJudgeVerdictService = agentJudgeVerdictService;
        this.agentScenarioJudgeService = agentScenarioJudgeService;
        this.agentScenarioToolSimulationService = agentScenarioToolSimulationService;
    }

    // Query mappings

    @QueryMapping
    List<AiAgentJudge> aiAgentJudges(@Argument String workflowId, @Argument String workflowNodeName) {
        return agentJudgeService.getAgentJudges(workflowId, workflowNodeName);
    }

    @QueryMapping
    List<AiAgentEvalTest> aiAgentEvalTests(@Argument String workflowId, @Argument String workflowNodeName) {
        return agentEvalTestService.getAgentEvalTests(workflowId, workflowNodeName);
    }

    @QueryMapping
    @Nullable
    AiAgentEvalTest aiAgentEvalTest(@Argument Long id) {
        return agentEvalTestService.fetchAgentEvalTest(id)
            .orElse(null);
    }

    @QueryMapping
    List<AiAgentEvalRun> aiAgentEvalRuns(
        @Argument Long agentEvalTestId, @Argument @Nullable Integer limit,
        @Argument @Nullable Integer offset) {

        List<AiAgentEvalRun> runs = agentEvalRunService.getAgentEvalRuns(agentEvalTestId);

        int startIndex = (offset != null) ? offset : 0;

        if (startIndex > 0) {
            runs = runs.subList(Math.min(startIndex, runs.size()), runs.size());
        }

        if (limit != null && limit > 0) {
            runs = runs.subList(0, Math.min(limit, runs.size()));
        }

        return runs;
    }

    @QueryMapping
    @Nullable
    AiAgentEvalRun aiAgentEvalRun(@Argument Long id) {
        return agentEvalRunService.fetchAgentEvalRun(id)
            .orElse(null);
    }

    @QueryMapping
    @Nullable
    AiAgentEvalResult aiAgentEvalResult(@Argument Long id) {
        return agentEvalResultService.fetchAgentEvalResult(id)
            .orElse(null);
    }

    @QueryMapping
    @Nullable
    String aiAgentEvalResultTranscript(@Argument long id) {
        AiAgentEvalResult aiAgentEvalResult = agentEvalResultService.getAgentEvalResult(id);

        FileEntry transcriptFileEntry = aiAgentEvalResult.getTranscriptFileEntry();

        if (transcriptFileEntry == null) {
            return null;
        }

        byte[] bytes = agentEvalFileStorage.readTranscriptFile(transcriptFileEntry);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Mutation mappings - AiAgentJudge

    @MutationMapping
    AiAgentJudge createAiAgentJudge(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument String name,
        @Argument AiAgentJudgeType type, @Argument Map<String, Object> configuration) {

        AiAgentJudge agentJudge = new AiAgentJudge();

        agentJudge.setWorkflowId(workflowId);
        agentJudge.setWorkflowNodeName(workflowNodeName);
        agentJudge.setName(name);
        agentJudge.setType(type);
        agentJudge.setConfiguration(configuration);

        return agentJudgeService.createAiAgentJudge(agentJudge);
    }

    @MutationMapping
    AiAgentJudge updateAiAgentJudge(
        @Argument Long id, @Argument @Nullable String name,
        @Argument @Nullable Map<String, Object> configuration) {

        AiAgentJudge agentJudge = agentJudgeService.getAgentJudge(id);

        if (name != null) {
            agentJudge.setName(name);
        }

        if (configuration != null) {
            agentJudge.setConfiguration(configuration);
        }

        return agentJudgeService.updateAiAgentJudge(agentJudge);
    }

    @MutationMapping
    boolean deleteAiAgentJudge(@Argument Long id) {
        agentJudgeService.deleteAiAgentJudge(id);

        return true;
    }

    // Mutation mappings - AiAgentEvalTest

    @MutationMapping
    AiAgentEvalTest createAiAgentEvalTest(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument String name,
        @Argument @Nullable String description) {

        AiAgentEvalTest aiAgentEvalTest = new AiAgentEvalTest();

        aiAgentEvalTest.setWorkflowId(workflowId);
        aiAgentEvalTest.setWorkflowNodeName(workflowNodeName);
        aiAgentEvalTest.setName(name);
        aiAgentEvalTest.setDescription(description);

        return agentEvalTestService.createAiAgentEvalTest(aiAgentEvalTest);
    }

    @MutationMapping
    AiAgentEvalTest updateAiAgentEvalTest(
        @Argument Long id, @Argument @Nullable String name, @Argument @Nullable String description) {

        AiAgentEvalTest aiAgentEvalTest = agentEvalTestService.getAgentEvalTest(id);

        if (name != null) {
            aiAgentEvalTest.setName(name);
        }

        if (description != null) {
            aiAgentEvalTest.setDescription(description);
        }

        return agentEvalTestService.updateAiAgentEvalTest(aiAgentEvalTest);
    }

    @MutationMapping
    boolean deleteAiAgentEvalTest(@Argument Long id) {
        agentEvalTestService.deleteAiAgentEvalTest(id);

        return true;
    }

    // Mutation mappings - AiAgentEvalScenario

    @MutationMapping
    AiAgentEvalScenario createAiAgentEvalScenario(
        @Argument Long agentEvalTestId, @Argument String name, @Argument AiAgentScenarioType type,
        @Argument @Nullable String userMessage, @Argument @Nullable String expectedOutput,
        @Argument @Nullable String personaPrompt, @Argument @Nullable Integer maxTurns,
        @Argument @Nullable Integer numberOfRuns) {

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

    @MutationMapping
    AiAgentEvalScenario updateAiAgentEvalScenario(
        @Argument Long id, @Argument @Nullable String name, @Argument @Nullable String userMessage,
        @Argument @Nullable String expectedOutput, @Argument @Nullable String personaPrompt,
        @Argument @Nullable Integer maxTurns, @Argument @Nullable Integer numberOfRuns) {

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

    @MutationMapping
    boolean deleteAiAgentEvalScenario(@Argument Long id) {
        agentEvalScenarioService.deleteAiAgentEvalScenario(id);

        return true;
    }

    // Mutation mappings - AiAgentScenarioJudge

    @MutationMapping
    AiAgentScenarioJudge createAiAgentScenarioJudge(
        @Argument Long agentEvalScenarioId, @Argument String name, @Argument AiAgentJudgeType type,
        @Argument Map<String, Object> configuration) {

        AiAgentScenarioJudge agentScenarioJudge = new AiAgentScenarioJudge();

        agentScenarioJudge.setAgentEvalScenarioId(agentEvalScenarioId);
        agentScenarioJudge.setName(name);
        agentScenarioJudge.setType(type);
        agentScenarioJudge.setConfiguration(configuration);

        return agentScenarioJudgeService.createAiAgentScenarioJudge(agentScenarioJudge);
    }

    @MutationMapping
    AiAgentScenarioJudge updateAiAgentScenarioJudge(
        @Argument Long id, @Argument @Nullable String name,
        @Argument @Nullable Map<String, Object> configuration) {

        AiAgentScenarioJudge agentScenarioJudge = agentScenarioJudgeService.getAgentScenarioJudge(id);

        if (name != null) {
            agentScenarioJudge.setName(name);
        }

        if (configuration != null) {
            agentScenarioJudge.setConfiguration(configuration);
        }

        return agentScenarioJudgeService.updateAiAgentScenarioJudge(agentScenarioJudge);
    }

    @MutationMapping
    boolean deleteAiAgentScenarioJudge(@Argument Long id) {
        agentScenarioJudgeService.deleteAiAgentScenarioJudge(id);

        return true;
    }

    // Mutation mappings - AiAgentScenarioToolSimulation

    @MutationMapping
    AiAgentScenarioToolSimulation createAiAgentScenarioToolSimulation(
        @Argument Long agentEvalScenarioId, @Argument String toolName,
        @Argument String responsePrompt, @Argument @Nullable String simulationModel) {

        AiAgentScenarioToolSimulation toolSimulation = new AiAgentScenarioToolSimulation();

        toolSimulation.setAgentEvalScenarioId(agentEvalScenarioId);
        toolSimulation.setToolName(toolName);
        toolSimulation.setResponsePrompt(responsePrompt);
        toolSimulation.setSimulationModel(simulationModel);

        return agentScenarioToolSimulationService.createAiAgentScenarioToolSimulation(toolSimulation);
    }

    @MutationMapping
    AiAgentScenarioToolSimulation updateAiAgentScenarioToolSimulation(
        @Argument Long id, @Argument @Nullable String toolName,
        @Argument @Nullable String responsePrompt, @Argument @Nullable String simulationModel) {

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

    @MutationMapping
    boolean deleteAiAgentScenarioToolSimulation(@Argument Long id) {
        agentScenarioToolSimulationService.deleteAiAgentScenarioToolSimulation(id);

        return true;
    }

    // Mutation mappings - AiAgentEvalRun

    @MutationMapping
    AiAgentEvalRun startAiAgentEvalRun(
        @Argument Long agentEvalTestId, @Argument String name, @Argument Long environmentId,
        @Argument @Nullable List<Long> scenarioIds, @Argument @Nullable List<Long> aiAgentJudgeIds) {

        return agentEvalRunFacade.startEvalRun(agentEvalTestId, name, environmentId, scenarioIds, aiAgentJudgeIds);
    }

    @MutationMapping
    AiAgentEvalRun cancelAiAgentEvalRun(@Argument Long id) {
        return agentEvalRunFacade.cancelEvalRun(id);
    }

    // Schema mappings - nested fields

    @SchemaMapping(typeName = "AiAgentEvalTest", field = "scenarios")
    List<AiAgentEvalScenario> agentEvalTestScenarios(AiAgentEvalTest aiAgentEvalTest) {
        return agentEvalScenarioService.getAgentEvalScenarios(aiAgentEvalTest.getId());
    }

    @SchemaMapping(typeName = "AiAgentEvalScenario", field = "judges")
    List<AiAgentScenarioJudge> agentEvalScenarioJudges(AiAgentEvalScenario aiAgentEvalScenario) {
        return agentScenarioJudgeService.getAgentScenarioJudges(aiAgentEvalScenario.getId());
    }

    @SchemaMapping(typeName = "AiAgentEvalScenario", field = "toolSimulations")
    List<AiAgentScenarioToolSimulation> scenarioToolSimulations(AiAgentEvalScenario scenario) {
        return agentScenarioToolSimulationService.getAgentScenarioToolSimulations(scenario.getId());
    }

    @SchemaMapping(typeName = "AiAgentEvalRun", field = "results")
    List<AiAgentEvalResult> agentEvalRunResults(AiAgentEvalRun aiAgentEvalRun) {
        return agentEvalResultService.getAgentEvalResults(aiAgentEvalRun.getId());
    }

    @SchemaMapping(typeName = "AiAgentEvalResult", field = "verdicts")
    List<AiAgentJudgeVerdict> agentEvalResultVerdicts(AiAgentEvalResult aiAgentEvalResult) {
        return agentJudgeVerdictService.getAgentJudgeVerdicts(aiAgentEvalResult.getId());
    }

    @SchemaMapping(typeName = "AiAgentEvalResult", field = "scenario")
    AiAgentEvalScenario agentEvalResultScenario(AiAgentEvalResult aiAgentEvalResult) {
        return agentEvalScenarioService.getAgentEvalScenario(aiAgentEvalResult.getAgentEvalScenarioId());
    }
}
