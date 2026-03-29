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

import com.bytechef.ai.agent.eval.constant.AgentJudgeType;
import com.bytechef.ai.agent.eval.constant.AgentScenarioType;
import com.bytechef.ai.agent.eval.domain.AgentEvalResult;
import com.bytechef.ai.agent.eval.domain.AgentEvalRun;
import com.bytechef.ai.agent.eval.domain.AgentEvalScenario;
import com.bytechef.ai.agent.eval.domain.AgentEvalTest;
import com.bytechef.ai.agent.eval.domain.AgentJudge;
import com.bytechef.ai.agent.eval.domain.AgentJudgeVerdict;
import com.bytechef.ai.agent.eval.domain.AgentScenarioJudge;
import com.bytechef.ai.agent.eval.facade.AgentEvalRunFacade;
import com.bytechef.ai.agent.eval.file.storage.AgentEvalFileStorage;
import com.bytechef.ai.agent.eval.service.AgentEvalResultService;
import com.bytechef.ai.agent.eval.service.AgentEvalRunService;
import com.bytechef.ai.agent.eval.service.AgentEvalScenarioService;
import com.bytechef.ai.agent.eval.service.AgentEvalTestService;
import com.bytechef.ai.agent.eval.service.AgentJudgeService;
import com.bytechef.ai.agent.eval.service.AgentJudgeVerdictService;
import com.bytechef.ai.agent.eval.service.AgentScenarioJudgeService;
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
class AgentEvalGraphQlController {

    private final AgentEvalFileStorage agentEvalFileStorage;
    private final AgentEvalResultService agentEvalResultService;
    private final AgentEvalRunFacade agentEvalRunFacade;
    private final AgentEvalRunService agentEvalRunService;
    private final AgentEvalScenarioService agentEvalScenarioService;
    private final AgentEvalTestService agentEvalTestService;
    private final AgentJudgeService agentJudgeService;
    private final AgentJudgeVerdictService agentJudgeVerdictService;
    private final AgentScenarioJudgeService agentScenarioJudgeService;

    AgentEvalGraphQlController(
        AgentEvalFileStorage agentEvalFileStorage, AgentEvalResultService agentEvalResultService,
        AgentEvalRunFacade agentEvalRunFacade, AgentEvalRunService agentEvalRunService,
        AgentEvalScenarioService agentEvalScenarioService, AgentEvalTestService agentEvalTestService,
        AgentJudgeService agentJudgeService, AgentJudgeVerdictService agentJudgeVerdictService,
        AgentScenarioJudgeService agentScenarioJudgeService) {

        this.agentEvalFileStorage = agentEvalFileStorage;
        this.agentEvalResultService = agentEvalResultService;
        this.agentEvalRunFacade = agentEvalRunFacade;
        this.agentEvalRunService = agentEvalRunService;
        this.agentEvalScenarioService = agentEvalScenarioService;
        this.agentEvalTestService = agentEvalTestService;
        this.agentJudgeService = agentJudgeService;
        this.agentJudgeVerdictService = agentJudgeVerdictService;
        this.agentScenarioJudgeService = agentScenarioJudgeService;
    }

    // Query mappings

    @QueryMapping
    List<AgentJudge> agentJudges(@Argument String workflowId, @Argument String workflowNodeName) {
        return agentJudgeService.getAgentJudges(workflowId, workflowNodeName);
    }

    @QueryMapping
    List<AgentEvalTest> agentEvalTests(@Argument String workflowId, @Argument String workflowNodeName) {
        return agentEvalTestService.getAgentEvalTests(workflowId, workflowNodeName);
    }

    @QueryMapping
    @Nullable
    AgentEvalTest agentEvalTest(@Argument Long id) {
        return agentEvalTestService.fetchAgentEvalTest(id)
            .orElse(null);
    }

    @QueryMapping
    List<AgentEvalRun> agentEvalRuns(
        @Argument Long agentEvalTestId, @Argument @Nullable Integer limit,
        @Argument @Nullable Integer offset) {

        List<AgentEvalRun> runs = agentEvalRunService.getAgentEvalRuns(agentEvalTestId);

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
    AgentEvalRun agentEvalRun(@Argument Long id) {
        return agentEvalRunService.fetchAgentEvalRun(id)
            .orElse(null);
    }

    @QueryMapping
    @Nullable
    AgentEvalResult agentEvalResult(@Argument Long id) {
        return agentEvalResultService.fetchAgentEvalResult(id)
            .orElse(null);
    }

    @QueryMapping
    @Nullable
    String agentEvalResultTranscript(@Argument long id) {
        AgentEvalResult agentEvalResult = agentEvalResultService.getAgentEvalResult(id);

        FileEntry transcriptFileEntry = agentEvalResult.getTranscriptFileEntry();

        if (transcriptFileEntry == null) {
            return null;
        }

        byte[] bytes = agentEvalFileStorage.readTranscriptFile(transcriptFileEntry);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Mutation mappings - AgentJudge

    @MutationMapping
    AgentJudge createAgentJudge(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument String name,
        @Argument AgentJudgeType type, @Argument Map<String, Object> configuration) {

        AgentJudge agentJudge = new AgentJudge();

        agentJudge.setWorkflowId(workflowId);
        agentJudge.setWorkflowNodeName(workflowNodeName);
        agentJudge.setName(name);
        agentJudge.setType(type);
        agentJudge.setConfiguration(configuration);

        return agentJudgeService.createAgentJudge(agentJudge);
    }

    @MutationMapping
    AgentJudge updateAgentJudge(
        @Argument Long id, @Argument @Nullable String name,
        @Argument @Nullable Map<String, Object> configuration) {

        AgentJudge agentJudge = agentJudgeService.getAgentJudge(id);

        if (name != null) {
            agentJudge.setName(name);
        }

        if (configuration != null) {
            agentJudge.setConfiguration(configuration);
        }

        return agentJudgeService.updateAgentJudge(agentJudge);
    }

    @MutationMapping
    boolean deleteAgentJudge(@Argument Long id) {
        agentJudgeService.deleteAgentJudge(id);

        return true;
    }

    // Mutation mappings - AgentEvalTest

    @MutationMapping
    AgentEvalTest createAgentEvalTest(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument String name,
        @Argument @Nullable String description) {

        AgentEvalTest agentEvalTest = new AgentEvalTest();

        agentEvalTest.setWorkflowId(workflowId);
        agentEvalTest.setWorkflowNodeName(workflowNodeName);
        agentEvalTest.setName(name);
        agentEvalTest.setDescription(description);

        return agentEvalTestService.createAgentEvalTest(agentEvalTest);
    }

    @MutationMapping
    AgentEvalTest updateAgentEvalTest(
        @Argument Long id, @Argument @Nullable String name, @Argument @Nullable String description) {

        AgentEvalTest agentEvalTest = agentEvalTestService.getAgentEvalTest(id);

        if (name != null) {
            agentEvalTest.setName(name);
        }

        if (description != null) {
            agentEvalTest.setDescription(description);
        }

        return agentEvalTestService.updateAgentEvalTest(agentEvalTest);
    }

    @MutationMapping
    boolean deleteAgentEvalTest(@Argument Long id) {
        agentEvalTestService.deleteAgentEvalTest(id);

        return true;
    }

    // Mutation mappings - AgentEvalScenario

    @MutationMapping
    AgentEvalScenario createAgentEvalScenario(
        @Argument Long agentEvalTestId, @Argument String name, @Argument AgentScenarioType type,
        @Argument @Nullable String userMessage, @Argument @Nullable String expectedOutput,
        @Argument @Nullable String personaPrompt, @Argument @Nullable Integer maxTurns) {

        AgentEvalScenario agentEvalScenario = new AgentEvalScenario();

        agentEvalScenario.setAgentEvalTestId(agentEvalTestId);
        agentEvalScenario.setName(name);
        agentEvalScenario.setType(type);
        agentEvalScenario.setUserMessage(userMessage);
        agentEvalScenario.setExpectedOutput(expectedOutput);
        agentEvalScenario.setPersonaPrompt(personaPrompt);

        if (maxTurns != null) {
            agentEvalScenario.setMaxTurns(maxTurns);
        }

        return agentEvalScenarioService.createAgentEvalScenario(agentEvalScenario);
    }

    @MutationMapping
    AgentEvalScenario updateAgentEvalScenario(
        @Argument Long id, @Argument @Nullable String name, @Argument @Nullable String userMessage,
        @Argument @Nullable String expectedOutput, @Argument @Nullable String personaPrompt,
        @Argument @Nullable Integer maxTurns) {

        AgentEvalScenario agentEvalScenario = agentEvalScenarioService.getAgentEvalScenario(id);

        if (name != null) {
            agentEvalScenario.setName(name);
        }

        if (userMessage != null) {
            agentEvalScenario.setUserMessage(userMessage);
        }

        if (expectedOutput != null) {
            agentEvalScenario.setExpectedOutput(expectedOutput);
        }

        if (personaPrompt != null) {
            agentEvalScenario.setPersonaPrompt(personaPrompt);
        }

        if (maxTurns != null) {
            agentEvalScenario.setMaxTurns(maxTurns);
        }

        return agentEvalScenarioService.updateAgentEvalScenario(agentEvalScenario);
    }

    @MutationMapping
    boolean deleteAgentEvalScenario(@Argument Long id) {
        agentEvalScenarioService.deleteAgentEvalScenario(id);

        return true;
    }

    // Mutation mappings - AgentScenarioJudge

    @MutationMapping
    AgentScenarioJudge createAgentScenarioJudge(
        @Argument Long agentEvalScenarioId, @Argument String name, @Argument AgentJudgeType type,
        @Argument Map<String, Object> configuration) {

        AgentScenarioJudge agentScenarioJudge = new AgentScenarioJudge();

        agentScenarioJudge.setAgentEvalScenarioId(agentEvalScenarioId);
        agentScenarioJudge.setName(name);
        agentScenarioJudge.setType(type);
        agentScenarioJudge.setConfiguration(configuration);

        return agentScenarioJudgeService.createAgentScenarioJudge(agentScenarioJudge);
    }

    @MutationMapping
    AgentScenarioJudge updateAgentScenarioJudge(
        @Argument Long id, @Argument @Nullable String name,
        @Argument @Nullable Map<String, Object> configuration) {

        AgentScenarioJudge agentScenarioJudge = agentScenarioJudgeService.getAgentScenarioJudge(id);

        if (name != null) {
            agentScenarioJudge.setName(name);
        }

        if (configuration != null) {
            agentScenarioJudge.setConfiguration(configuration);
        }

        return agentScenarioJudgeService.updateAgentScenarioJudge(agentScenarioJudge);
    }

    @MutationMapping
    boolean deleteAgentScenarioJudge(@Argument Long id) {
        agentScenarioJudgeService.deleteAgentScenarioJudge(id);

        return true;
    }

    // Mutation mappings - AgentEvalRun

    @MutationMapping
    AgentEvalRun startAgentEvalRun(
        @Argument Long agentEvalTestId, @Argument String name, @Argument Long environmentId,
        @Argument @Nullable List<Long> scenarioIds, @Argument @Nullable List<Long> agentJudgeIds) {

        return agentEvalRunFacade.startEvalRun(agentEvalTestId, name, environmentId, scenarioIds, agentJudgeIds);
    }

    @MutationMapping
    AgentEvalRun cancelAgentEvalRun(@Argument Long id) {
        return agentEvalRunFacade.cancelEvalRun(id);
    }

    // Schema mappings - nested fields

    @SchemaMapping(typeName = "AgentEvalTest", field = "scenarios")
    List<AgentEvalScenario> agentEvalTestScenarios(AgentEvalTest agentEvalTest) {
        return agentEvalScenarioService.getAgentEvalScenarios(agentEvalTest.getId());
    }

    @SchemaMapping(typeName = "AgentEvalScenario", field = "judges")
    List<AgentScenarioJudge> agentEvalScenarioJudges(AgentEvalScenario agentEvalScenario) {
        return agentScenarioJudgeService.getAgentScenarioJudges(agentEvalScenario.getId());
    }

    @SchemaMapping(typeName = "AgentEvalRun", field = "results")
    List<AgentEvalResult> agentEvalRunResults(AgentEvalRun agentEvalRun) {
        return agentEvalResultService.getAgentEvalResults(agentEvalRun.getId());
    }

    @SchemaMapping(typeName = "AgentEvalResult", field = "verdicts")
    List<AgentJudgeVerdict> agentEvalResultVerdicts(AgentEvalResult agentEvalResult) {
        return agentJudgeVerdictService.getAgentJudgeVerdicts(agentEvalResult.getId());
    }

    @SchemaMapping(typeName = "AgentEvalResult", field = "scenario")
    AgentEvalScenario agentEvalResultScenario(AgentEvalResult agentEvalResult) {
        return agentEvalScenarioService.getAgentEvalScenario(agentEvalResult.getAgentEvalScenarioId());
    }
}
