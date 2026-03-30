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

package com.bytechef.ai.agent.eval.executor;

import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;

import com.bytechef.ai.agent.eval.constant.AgentEvalResultStatus;
import com.bytechef.ai.agent.eval.constant.AgentEvalRunStatus;
import com.bytechef.ai.agent.eval.constant.AgentJudgeScope;
import com.bytechef.ai.agent.eval.constant.AgentJudgeType;
import com.bytechef.ai.agent.eval.constant.AgentScenarioType;
import com.bytechef.ai.agent.eval.domain.AgentEvalResult;
import com.bytechef.ai.agent.eval.domain.AgentEvalRun;
import com.bytechef.ai.agent.eval.domain.AgentEvalScenario;
import com.bytechef.ai.agent.eval.domain.AgentJudge;
import com.bytechef.ai.agent.eval.domain.AgentJudgeVerdict;
import com.bytechef.ai.agent.eval.domain.AgentScenarioJudge;
import com.bytechef.ai.agent.eval.domain.AgentScenarioToolSimulation;
import com.bytechef.ai.agent.eval.file.storage.AgentEvalFileStorage;
import com.bytechef.ai.agent.eval.judge.AgentJudgeFactory;
import com.bytechef.ai.agent.eval.service.AgentEvalResultService;
import com.bytechef.ai.agent.eval.service.AgentEvalRunService;
import com.bytechef.ai.agent.eval.service.AgentEvalScenarioService;
import com.bytechef.ai.agent.eval.service.AgentJudgeService;
import com.bytechef.ai.agent.eval.service.AgentJudgeVerdictService;
import com.bytechef.ai.agent.eval.service.AgentScenarioJudgeService;
import com.bytechef.ai.agent.eval.service.AgentScenarioToolSimulationService;
import com.bytechef.ai.agent.eval.simulator.UserSimulator;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.workflow.test.facade.AiAgentTestFacade;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.judge.Judge;
import org.springaicommunity.judge.context.ExecutionStatus;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.jury.AverageVotingStrategy;
import org.springaicommunity.judge.jury.SimpleJury;
import org.springaicommunity.judge.jury.Verdict;
import org.springaicommunity.judge.result.Judgment;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Executes agent evaluation runs asynchronously. Each service it calls handles its own transactions.
 *
 * @author Ivica Cardic
 */
@Component
public class AgentEvalRunExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AgentEvalRunExecutor.class);

    private final AgentEvalFileStorage agentEvalFileStorage;
    private final AgentEvalResultService agentEvalResultService;
    private final AgentEvalRunService agentEvalRunService;
    private final AgentEvalScenarioService agentEvalScenarioService;
    private final AgentJudgeFactory agentJudgeFactory;
    private final AgentJudgeService agentJudgeService;
    private final AgentJudgeVerdictService agentJudgeVerdictService;
    private final AgentScenarioJudgeService agentScenarioJudgeService;
    private final AgentScenarioToolSimulationService agentScenarioToolSimulationService;
    private final AiAgentTestFacade aiAgentTestFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConnectionService connectionService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    AgentEvalRunExecutor(
        AgentEvalFileStorage agentEvalFileStorage, AgentEvalResultService agentEvalResultService,
        AgentEvalRunService agentEvalRunService, AgentEvalScenarioService agentEvalScenarioService,
        AgentJudgeFactory agentJudgeFactory, AgentJudgeService agentJudgeService,
        AgentJudgeVerdictService agentJudgeVerdictService, AgentScenarioJudgeService agentScenarioJudgeService,
        AgentScenarioToolSimulationService agentScenarioToolSimulationService,
        AiAgentTestFacade aiAgentTestFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        ConnectionService connectionService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.agentEvalFileStorage = agentEvalFileStorage;
        this.agentEvalResultService = agentEvalResultService;
        this.agentEvalRunService = agentEvalRunService;
        this.agentEvalScenarioService = agentEvalScenarioService;
        this.agentJudgeFactory = agentJudgeFactory;
        this.agentJudgeService = agentJudgeService;
        this.agentJudgeVerdictService = agentJudgeVerdictService;
        this.agentScenarioJudgeService = agentScenarioJudgeService;
        this.agentScenarioToolSimulationService = agentScenarioToolSimulationService;
        this.aiAgentTestFacade = aiAgentTestFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectionService = connectionService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Async("agentEvalExecutor")
    public void executeRunAsync(long evalRunId, List<Long> scenarioIds, List<Long> agentJudgeIds) {
        AgentEvalRun evalRun = agentEvalRunService.getAgentEvalRun(evalRunId);

        evalRun.setStatus(AgentEvalRunStatus.RUNNING);
        evalRun.setStartedDate(Instant.now());

        evalRun = agentEvalRunService.updateAgentEvalRun(evalRun);

        ChatClient.Builder judgeChatClientBuilder = buildJudgeChatClientBuilder(evalRun);

        List<AgentEvalScenario> allScenarios = agentEvalScenarioService.getAgentEvalScenarios(
            evalRun.getAgentEvalTestId());

        List<AgentEvalScenario> scenarios = (scenarioIds == null || scenarioIds.isEmpty())
            ? allScenarios
            : allScenarios.stream()
                .filter(scenario -> scenarioIds.contains(scenario.getId()))
                .toList();

        List<Double> resultScores = new ArrayList<>();
        int totalInputTokens = 0;
        int totalOutputTokens = 0;

        for (AgentEvalScenario scenario : scenarios) {
            int numberOfRuns = Math.max(1, scenario.getNumberOfRuns());

            for (int runIndex = 1; runIndex <= numberOfRuns; runIndex++) {
                if (isCancelled(evalRunId)) {
                    logger.info("Eval run {} was cancelled, aborting execution", evalRunId);

                    finalizeRun(evalRunId, resultScores, totalInputTokens, totalOutputTokens);

                    return;
                }

                AgentEvalResult evalResult = new AgentEvalResult();

                evalResult.setAgentEvalRunId(evalRunId);
                evalResult.setAgentEvalScenarioId(scenario.getId());
                evalResult.setStatus(AgentEvalResultStatus.RUNNING);
                evalResult.setRunIndex(runIndex);

                evalResult = agentEvalResultService.createAgentEvalResult(evalResult);

                try {
                    ScenarioExecutionResult executionResult = executeScenario(
                        evalRun, scenario, evalRunId, judgeChatClientBuilder);

                    String agentResponse = executionResult.agentResponse();
                    String transcriptJson = executionResult.transcriptJson();

                    evalResult.setInputTokens(executionResult.inputTokens());
                    evalResult.setOutputTokens(executionResult.outputTokens());

                    totalInputTokens += executionResult.inputTokens();
                    totalOutputTokens += executionResult.outputTokens();

                    evalResult.setTranscriptFileEntry(
                        agentEvalFileStorage.storeTranscriptFile(
                            "transcript_" + evalResult.getId() + ".json",
                            transcriptJson.getBytes(StandardCharsets.UTF_8)));

                    evalResult = judgeResult(
                        evalRun, scenario, evalResult, agentResponse, transcriptJson,
                        judgeChatClientBuilder, agentJudgeIds);

                    resultScores.add(evalResult.getScore());
                } catch (Exception exception) {
                    logger.error(
                        "Error executing scenario {} (run {}) for eval run {}",
                        scenario.getId(), runIndex, evalRunId, exception);

                    evalResult.setStatus(AgentEvalResultStatus.FAILED);
                    evalResult.setErrorMessage(
                        exception.getClass()
                            .getSimpleName() + ": " + exception.getMessage());

                    agentEvalResultService.updateAgentEvalResult(evalResult);

                    resultScores.add(0.0);
                }

                evalRun = agentEvalRunService.getAgentEvalRun(evalRunId);

                evalRun.setCompletedScenarios(evalRun.getCompletedScenarios() + 1);

                evalRun = agentEvalRunService.updateAgentEvalRun(evalRun);
            }
        }

        finalizeRun(evalRunId, resultScores, totalInputTokens, totalOutputTokens);
    }

    @Nullable
    private ChatClient.Builder buildJudgeChatClientBuilder(AgentEvalRun agentEvalRun) {
        try {
            Workflow workflow = workflowService.getWorkflow(agentEvalRun.getWorkflowId());

            WorkflowTask workflowTask = workflow.getTasks(true)
                .stream()
                .filter(task -> Objects.equals(task.getName(), agentEvalRun.getWorkflowNodeName()))
                .findFirst()
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "Workflow task not found: %s".formatted(agentEvalRun.getWorkflowNodeName())));

            Map<String, ?> extensions = workflowTask.getExtensions();

            ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

            ClusterElement modelClusterElement = clusterElementMap.getClusterElement(MODEL);

            List<WorkflowTestConfigurationConnection> testConfigurationConnections =
                workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                    agentEvalRun.getWorkflowId(), modelClusterElement.getWorkflowNodeName(),
                    agentEvalRun.getEnvironmentId());

            if (testConfigurationConnections.isEmpty()) {
                logger.warn(
                    "No test connection configured for MODEL cluster element '{}' in workflow '{}'. " +
                        "LLM judges will not be functional.",
                    modelClusterElement.getWorkflowNodeName(), agentEvalRun.getWorkflowId());

                return null;
            }

            Long connectionId = testConfigurationConnections.getFirst()
                .getConnectionId();

            Connection connection = connectionService.getConnection(connectionId);

            ComponentConnection componentConnection = new ComponentConnection(
                modelClusterElement.getComponentName(), modelClusterElement.getComponentVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());

            ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
                modelClusterElement.getComponentName(), modelClusterElement.getComponentVersion(),
                modelClusterElement.getClusterElementName());

            ChatModel chatModel = (ChatModel) modelFunction.apply(
                ParametersFactory.create(modelClusterElement.getParameters()),
                ParametersFactory.create(componentConnection.getParameters()), false);

            return ChatClient.builder(chatModel);
        } catch (Exception exception) {
            logger.warn(
                "Failed to build ChatClient for LLM judges in eval run {}. LLM judges will not be functional.",
                agentEvalRun.getId(), exception);

            return null;
        }
    }

    private String buildMultiTurnTranscriptJson(
        List<Map<String, String>> transcriptTurns, String expectedOutput) {

        StringBuilder transcriptBuilder = new StringBuilder();

        transcriptBuilder.append("{\"messages\":[");

        for (int turnIndex = 0; turnIndex < transcriptTurns.size(); turnIndex++) {
            Map<String, String> turn = transcriptTurns.get(turnIndex);

            if (turnIndex > 0) {
                transcriptBuilder.append(",");
            }

            transcriptBuilder.append("{\"role\":");
            transcriptBuilder.append(escapeJsonString(turn.get("role")));
            transcriptBuilder.append(",\"content\":");
            transcriptBuilder.append(escapeJsonString(turn.get("content")));
            transcriptBuilder.append(",\"turnNumber\":");
            transcriptBuilder.append(turn.get("turnNumber"));
            transcriptBuilder.append("}");
        }

        transcriptBuilder.append("]");

        if (expectedOutput != null) {
            transcriptBuilder.append(",\"expectedOutput\":");
            transcriptBuilder.append(escapeJsonString(expectedOutput));
        }

        transcriptBuilder.append("}");

        return transcriptBuilder.toString();
    }

    private String buildTranscriptJson(String userMessage, String agentResponse, String expectedOutput) {
        StringBuilder transcriptBuilder = new StringBuilder();

        transcriptBuilder.append("{\"messages\":[");
        transcriptBuilder.append("{\"role\":\"user\",\"content\":");
        transcriptBuilder.append(escapeJsonString(userMessage != null ? userMessage : ""));
        transcriptBuilder.append("},{\"role\":\"assistant\",\"content\":");
        transcriptBuilder.append(escapeJsonString(agentResponse));
        transcriptBuilder.append("}]");

        if (expectedOutput != null) {
            transcriptBuilder.append(",\"expectedOutput\":");
            transcriptBuilder.append(escapeJsonString(expectedOutput));
        }

        transcriptBuilder.append("}");

        return transcriptBuilder.toString();
    }

    @Nullable
    private ChatClient.Builder buildChatClientBuilderFromConfiguration(Map<String, Object> configuration) {
        Object connectionIdValue = configuration.get("connectionId");

        if (!(connectionIdValue instanceof Number connectionIdNumber)) {
            return null;
        }

        long connectionId = connectionIdNumber.longValue();

        try {
            Connection connection = connectionService.getConnection(connectionId);

            ComponentConnection componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());

            String componentName = configuration.containsKey("componentName")
                ? (String) configuration.get("componentName")
                : connection.getComponentName();

            int componentVersion = configuration.containsKey("componentVersion")
                ? ((Number) configuration.get("componentVersion")).intValue()
                : connection.getConnectionVersion();

            ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
                componentName, componentVersion, "model");

            Map<String, Object> modelParameters = new HashMap<>();

            if (configuration.containsKey("model")) {
                modelParameters.put("model", configuration.get("model"));
            }

            ChatModel chatModel = (ChatModel) modelFunction.apply(
                ParametersFactory.create(modelParameters),
                ParametersFactory.create(componentConnection.getParameters()), false);

            return ChatClient.builder(chatModel);
        } catch (Exception exception) {
            logger.warn(
                "Failed to build ChatClient from connection {}. LLM judge will not be functional.",
                connectionId, exception);

            return null;
        }
    }

    private List<Judge> collectJudges(
        AgentEvalRun evalRun, AgentEvalScenario scenario, @Nullable ChatClient.Builder defaultChatClientBuilder,
        List<Long> agentJudgeIds) {

        List<Judge> judges = new ArrayList<>();

        List<AgentJudge> agentJudges = agentJudgeService.getAgentJudges(
            evalRun.getWorkflowId(), evalRun.getWorkflowNodeName());

        for (AgentJudge agentJudge : agentJudges) {
            if (!agentJudgeIds.isEmpty() && !agentJudgeIds.contains(agentJudge.getId())) {
                continue;
            }

            ChatClient.Builder chatClientBuilder = resolveJudgeChatClientBuilder(
                agentJudge.getConfiguration(), defaultChatClientBuilder);

            if (agentJudge.getType() == AgentJudgeType.LLM_RULE && chatClientBuilder == null) {
                logger.warn("Skipping LLM judge '{}' — no ChatClient available", agentJudge.getName());

                continue;
            }

            judges.add(
                agentJudgeFactory.createJudge(
                    agentJudge.getName(), agentJudge.getType(), agentJudge.getConfiguration(), chatClientBuilder));
        }

        List<AgentScenarioJudge> scenarioJudges = agentScenarioJudgeService.getAgentScenarioJudges(scenario.getId());

        for (AgentScenarioJudge scenarioJudge : scenarioJudges) {
            ChatClient.Builder chatClientBuilder = resolveJudgeChatClientBuilder(
                scenarioJudge.getConfiguration(), defaultChatClientBuilder);

            if (scenarioJudge.getType() == AgentJudgeType.LLM_RULE && chatClientBuilder == null) {
                logger.warn("Skipping LLM judge '{}' — no ChatClient available", scenarioJudge.getName());

                continue;
            }

            judges.add(
                agentJudgeFactory.createJudge(
                    scenarioJudge.getName(), scenarioJudge.getType(), scenarioJudge.getConfiguration(),
                    chatClientBuilder));
        }

        return judges;
    }

    private String escapeJsonString(String value) {
        StringBuilder escapedBuilder = new StringBuilder("\"");

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            switch (character) {
                case '"' -> escapedBuilder.append("\\\"");
                case '\\' -> escapedBuilder.append("\\\\");
                case '\n' -> escapedBuilder.append("\\n");
                case '\r' -> escapedBuilder.append("\\r");
                case '\t' -> escapedBuilder.append("\\t");
                default -> escapedBuilder.append(character);
            }
        }

        escapedBuilder.append("\"");

        return escapedBuilder.toString();
    }

    private ScenarioExecutionResult executeScenario(
        AgentEvalRun evalRun, AgentEvalScenario scenario, long evalRunId,
        @Nullable ChatClient.Builder chatClientBuilder) {

        if (scenario.getType() == AgentScenarioType.MULTI_TURN) {
            return executeMultiTurnScenario(evalRun, scenario, evalRunId, chatClientBuilder);
        }

        UUID uuid = UUID.randomUUID();

        String conversationId = uuid.toString();

        Map<String, Map<String, String>> toolSimulations = loadToolSimulations(scenario.getId());

        Object result = aiAgentTestFacade.executeAiAgentAction(
            evalRun.getWorkflowId(), evalRun.getWorkflowNodeName(), evalRun.getEnvironmentId(),
            conversationId, scenario.getUserMessage(), List.of(), toolSimulations);

        String agentResponse = extractAgentResponse(result);

        String transcriptJson = buildTranscriptJson(
            scenario.getUserMessage(), agentResponse, scenario.getExpectedOutput());

        return new ScenarioExecutionResult(agentResponse, transcriptJson, 0, 0);
    }

    private ScenarioExecutionResult executeMultiTurnScenario(
        AgentEvalRun evalRun, AgentEvalScenario scenario, long evalRunId,
        @Nullable ChatClient.Builder chatClientBuilder) {

        if (chatClientBuilder == null) {
            throw new IllegalStateException(
                "Multi-turn scenario '%s' requires an AI connection for user simulation, "
                    .formatted(scenario.getName()) +
                    "but no model connection is configured. Please configure a model connection in the test settings.");
        }

        ChatClient chatClient = chatClientBuilder.build();

        UserSimulator userSimulator = new UserSimulator(chatClient, scenario.getPersonaPrompt());

        Map<String, Map<String, String>> toolSimulations = loadToolSimulations(scenario.getId());

        UUID uuid = UUID.randomUUID();

        String conversationId = uuid.toString();

        List<Map<String, String>> conversationHistory = new ArrayList<>();
        List<Map<String, String>> transcriptTurns = new ArrayList<>();

        String userMessage = (scenario.getUserMessage() != null && !scenario.getUserMessage()
            .isBlank())
                ? scenario.getUserMessage()
                : userSimulator.generateNextMessage(Collections.emptyList());

        String lastAgentResponse = "";
        int turnNumber = 1;

        for (int turnIndex = 0; turnIndex < scenario.getMaxTurns(); turnIndex++) {
            Object result = aiAgentTestFacade.executeAiAgentAction(
                evalRun.getWorkflowId(), evalRun.getWorkflowNodeName(), evalRun.getEnvironmentId(),
                conversationId, userMessage, List.of(), toolSimulations);

            String agentResponse = extractAgentResponse(result);

            lastAgentResponse = agentResponse;

            Map<String, String> userTurn = new HashMap<>();

            userTurn.put("role", "user");
            userTurn.put("content", userMessage);
            userTurn.put("turnNumber", String.valueOf(turnNumber));

            conversationHistory.add(Map.of("role", "user", "content", userMessage));
            transcriptTurns.add(userTurn);

            turnNumber++;

            Map<String, String> assistantTurn = new HashMap<>();

            assistantTurn.put("role", "assistant");
            assistantTurn.put("content", agentResponse);
            assistantTurn.put("turnNumber", String.valueOf(turnNumber));

            conversationHistory.add(Map.of("role", "assistant", "content", agentResponse));
            transcriptTurns.add(assistantTurn);

            turnNumber++;

            if (isCancelled(evalRunId)) {
                break;
            }

            if (turnIndex < scenario.getMaxTurns() - 1) {
                String nextUserMessage = userSimulator.generateNextMessage(conversationHistory);

                if (userSimulator.isConversationComplete(nextUserMessage)) {
                    break;
                }

                userMessage = nextUserMessage;
            }
        }

        String transcriptJson = buildMultiTurnTranscriptJson(transcriptTurns, scenario.getExpectedOutput());

        return new ScenarioExecutionResult(lastAgentResponse, transcriptJson, 0, 0);
    }

    @SuppressWarnings("unchecked")
    private String extractAgentResponse(Object result) {
        if (result instanceof String stringResult) {
            return stringResult;
        }

        if (result instanceof Map<?, ?> mapResult) {
            Object content = mapResult.get("content");

            if (content != null) {
                return content.toString();
            }

            Object output = mapResult.get("output");

            if (output != null) {
                return output.toString();
            }

            return mapResult.toString();
        }

        if (result == null) {
            return "";
        }

        return result.toString();
    }

    private AgentEvalResult judgeResult(
        AgentEvalRun evalRun, AgentEvalScenario scenario, AgentEvalResult evalResult, String agentResponse,
        String transcriptJson, @Nullable ChatClient.Builder judgeChatClientBuilder, List<Long> agentJudgeIds) {

        JudgmentContext judgmentContext = JudgmentContext.builder()
            .agentOutput(agentResponse)
            .status(ExecutionStatus.SUCCESS)
            .goal(scenario.getName())
            .startedAt(Instant.now())
            .executionTime(Duration.ZERO)
            .metadata("transcript", transcriptJson)
            .metadata("expectedOutput",
                scenario.getExpectedOutput() != null ? scenario.getExpectedOutput() : "")
            .build();

        List<Judge> judges = collectJudges(evalRun, scenario, judgeChatClientBuilder, agentJudgeIds);

        if (judges.isEmpty()) {
            evalResult.setScore(1.0);
            evalResult.setStatus(AgentEvalResultStatus.COMPLETED);

            return agentEvalResultService.updateAgentEvalResult(evalResult);
        }

        SimpleJury.Builder juryBuilder = SimpleJury.builder()
            .votingStrategy(new AverageVotingStrategy())
            .parallel(false);

        for (Judge judge : judges) {
            juryBuilder.judge(judge);
        }

        SimpleJury jury = juryBuilder.build();

        Verdict verdict = jury.vote(judgmentContext);

        Map<String, Judgment> individualByName = verdict.individualByName();

        int passedCount = 0;
        int totalCount = individualByName.size();

        for (Map.Entry<String, Judgment> entry : individualByName.entrySet()) {
            AgentJudgeVerdict judgeVerdict = new AgentJudgeVerdict();

            judgeVerdict.setAgentEvalResultId(evalResult.getId());
            judgeVerdict.setJudgeName(entry.getKey());
            judgeVerdict.setPassed(entry.getValue()
                .pass());
            judgeVerdict.setScore(entry.getValue()
                .pass() ? 1.0 : 0.0);
            judgeVerdict.setExplanation(entry.getValue()
                .reasoning());
            judgeVerdict.setJudgeType(resolveJudgeType(entry.getKey(), evalRun, scenario));
            judgeVerdict.setJudgeScope(resolveJudgeScope(entry.getKey(), evalRun, scenario));

            agentJudgeVerdictService.createAgentJudgeVerdict(judgeVerdict);

            if (entry.getValue()
                .pass()) {
                passedCount++;
            }
        }

        double score = (totalCount > 0) ? ((double) passedCount / totalCount) : 1.0;

        evalResult.setScore(score);
        evalResult.setStatus(AgentEvalResultStatus.COMPLETED);

        return agentEvalResultService.updateAgentEvalResult(evalResult);
    }

    private boolean isCancelled(long evalRunId) {
        AgentEvalRun evalRun = agentEvalRunService.getAgentEvalRun(evalRunId);

        return evalRun.getStatus() == AgentEvalRunStatus.FAILED;
    }

    private Map<String, Map<String, String>> loadToolSimulations(long scenarioId) {
        List<AgentScenarioToolSimulation> simulations =
            agentScenarioToolSimulationService.getAgentScenarioToolSimulations(scenarioId);

        return simulations.stream()
            .collect(
                Collectors.toMap(
                    AgentScenarioToolSimulation::getToolName,
                    simulation -> {
                        Map<String, String> config = new HashMap<>();

                        config.put("responsePrompt", simulation.getResponsePrompt());

                        if (simulation.getSimulationModel() != null) {
                            config.put("simulationModel", simulation.getSimulationModel());
                        }

                        return config;
                    },
                    (existing, replacement) -> replacement));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private AgentJudgeScope resolveJudgeScope(
        String judgeName, AgentEvalRun evalRun, AgentEvalScenario scenario) {

        List<AgentJudge> agentJudges = agentJudgeService.getAgentJudges(
            evalRun.getWorkflowId(), evalRun.getWorkflowNodeName());

        for (AgentJudge agentJudge : agentJudges) {
            if (agentJudge.getName()
                .equals(judgeName)) {
                return AgentJudgeScope.AGENT;
            }
        }

        return AgentJudgeScope.SCENARIO;
    }

    private AgentJudgeType resolveJudgeType(
        String judgeName, AgentEvalRun evalRun, AgentEvalScenario scenario) {

        List<AgentJudge> agentJudges = agentJudgeService.getAgentJudges(
            evalRun.getWorkflowId(), evalRun.getWorkflowNodeName());

        for (AgentJudge agentJudge : agentJudges) {
            if (agentJudge.getName()
                .equals(judgeName)) {
                return agentJudge.getType();
            }
        }

        List<AgentScenarioJudge> scenarioJudges = agentScenarioJudgeService.getAgentScenarioJudges(scenario.getId());

        for (AgentScenarioJudge scenarioJudge : scenarioJudges) {
            if (scenarioJudge.getName()
                .equals(judgeName)) {
                return scenarioJudge.getType();
            }
        }

        logger.warn("Could not resolve judge type for judge '{}', defaulting to CONTAINS_TEXT", judgeName);

        return AgentJudgeType.CONTAINS_TEXT;
    }

    @Nullable
    private ChatClient.Builder resolveJudgeChatClientBuilder(
        Map<String, Object> configuration, @Nullable ChatClient.Builder defaultChatClientBuilder) {

        if (configuration.containsKey("connectionId")) {
            ChatClient.Builder perJudgeBuilder = buildChatClientBuilderFromConfiguration(configuration);

            if (perJudgeBuilder != null) {
                return perJudgeBuilder;
            }
        }

        return defaultChatClientBuilder;
    }

    private void finalizeRun(
        long evalRunId, List<Double> resultScores, int totalInputTokens, int totalOutputTokens) {

        double averageScore = resultScores.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        AgentEvalRun evalRun = agentEvalRunService.getAgentEvalRun(evalRunId);

        evalRun.setAverageScore(averageScore);
        evalRun.setCompletedDate(Instant.now());
        evalRun.setTotalInputTokens(totalInputTokens);
        evalRun.setTotalOutputTokens(totalOutputTokens);

        if (evalRun.getStatus() != AgentEvalRunStatus.FAILED) {
            evalRun.setStatus(AgentEvalRunStatus.COMPLETED);
        }

        agentEvalRunService.updateAgentEvalRun(evalRun);
    }

    private record ScenarioExecutionResult(String agentResponse, String transcriptJson, int inputTokens,
        int outputTokens) {
    }
}
