/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.eval.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.agent.eval.audit.AiAgentEvalAuditEvents;
import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentJudgeType;
import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentScenarioType;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalRun;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalScenario;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalTest;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentJudge;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentScenarioJudge;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentScenarioToolSimulation;
import com.bytechef.ee.platform.ai.agent.eval.executor.AiAgentEvalRunExecutor;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalRunService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalScenarioService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalTestService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentJudgeService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentScenarioJudgeService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentScenarioToolSimulationService;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    AiAgentEvalFacadeAuditIntTest.AiAgentEvalFacadeConfiguration.class, PostgreSQLContainerConfiguration.class
})
class AiAgentEvalFacadeAuditIntTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long EVAL_RUN_ID = 31L;
    private static final long EVAL_SCENARIO_ID = 11L;
    private static final long EVAL_TEST_ID = 7L;
    private static final long JUDGE_ID = 13L;
    private static final long SCENARIO_JUDGE_ID = 17L;
    private static final long TOOL_SIMULATION_ID = 19L;
    private static final String WORKFLOW_ID = "workflow-uuid";
    private static final String WORKFLOW_NODE_NAME = "aiAgent_1";

    @Autowired
    private AiAgentEvalFacade aiAgentEvalFacade;

    @Autowired
    private AiAgentEvalRunFacade aiAgentEvalRunFacade;

    @MockitoBean
    private AiAgentEvalRunExecutor aiAgentEvalRunExecutor;

    @MockitoBean
    private AiAgentEvalRunService aiAgentEvalRunService;

    @MockitoBean
    private AiAgentEvalScenarioService aiAgentEvalScenarioService;

    @MockitoBean
    private AiAgentEvalTestService aiAgentEvalTestService;

    @MockitoBean
    private AiAgentJudgeService aiAgentJudgeService;

    @MockitoBean
    private AiAgentScenarioJudgeService aiAgentScenarioJudgeService;

    @MockitoBean
    private AiAgentScenarioToolSimulationService aiAgentScenarioToolSimulationService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEvalTestChangesRecordTheTestAndItsName() {
        AiAgentEvalTest aiAgentEvalTest = new AiAgentEvalTest();

        aiAgentEvalTest.setId(EVAL_TEST_ID);
        aiAgentEvalTest.setName("renamed test");

        when(aiAgentEvalTestService.createAiAgentEvalTest(any())).thenAnswer(invocation -> {
            AiAgentEvalTest createdAiAgentEvalTest = invocation.getArgument(0);

            createdAiAgentEvalTest.setId(EVAL_TEST_ID);

            return createdAiAgentEvalTest;
        });
        when(aiAgentEvalTestService.getAgentEvalTest(EVAL_TEST_ID)).thenReturn(aiAgentEvalTest);
        when(aiAgentEvalTestService.updateAiAgentEvalTest(any())).thenReturn(aiAgentEvalTest);

        aiAgentEvalFacade.createAiAgentEvalTest(WORKFLOW_ID, WORKFLOW_NODE_NAME, "test", "confidential description");

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_TEST_CREATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentEvalTestId", String.valueOf(EVAL_TEST_ID))
            .containsEntry("name", "test")
            .containsEntry("workflowId", WORKFLOW_ID)
            .containsEntry("workflowNodeName", WORKFLOW_NODE_NAME)
            .doesNotContainValue("confidential description");

        aiAgentEvalFacade.updateAiAgentEvalTest(EVAL_TEST_ID, "renamed test", null);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_TEST_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentEvalTestId", String.valueOf(EVAL_TEST_ID))
            .containsEntry("name", "renamed test");

        aiAgentEvalFacade.deleteAiAgentEvalTest(EVAL_TEST_ID);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_TEST_DELETED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentEvalTestId", String.valueOf(EVAL_TEST_ID));
    }

    @Test
    void testUpdateOfAnUnknownEvalTestRecordsAnError() {
        when(aiAgentEvalTestService.getAgentEvalTest(EVAL_TEST_ID)).thenThrow(new NoSuchElementException());

        assertThatThrownBy(() -> aiAgentEvalFacade.updateAiAgentEvalTest(EVAL_TEST_ID, "renamed test", null))
            .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_TEST_UPDATED))
            .containsEntry("result", "ERROR")
            .containsEntry("aiAgentEvalTestId", String.valueOf(EVAL_TEST_ID))
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .doesNotContainKey("name");
    }

    @Test
    void testEvalScenarioChangesRecordTheScenarioWithoutItsMessages() {
        AiAgentEvalScenario aiAgentEvalScenario = new AiAgentEvalScenario();

        aiAgentEvalScenario.setId(EVAL_SCENARIO_ID);
        aiAgentEvalScenario.setName("renamed scenario");

        when(aiAgentEvalScenarioService.createAiAgentEvalScenario(any())).thenAnswer(invocation -> {
            AiAgentEvalScenario createdAiAgentEvalScenario = invocation.getArgument(0);

            createdAiAgentEvalScenario.setId(EVAL_SCENARIO_ID);

            return createdAiAgentEvalScenario;
        });
        when(aiAgentEvalScenarioService.getAgentEvalScenario(EVAL_SCENARIO_ID)).thenReturn(aiAgentEvalScenario);
        when(aiAgentEvalScenarioService.updateAiAgentEvalScenario(any())).thenReturn(aiAgentEvalScenario);

        aiAgentEvalFacade.createAiAgentEvalScenario(
            EVAL_TEST_ID, "scenario", AiAgentScenarioType.SINGLE_TURN, "confidential message", "confidential output",
            "confidential persona", null, null);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_SCENARIO_CREATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentEvalScenarioId", String.valueOf(EVAL_SCENARIO_ID))
            .containsEntry("aiAgentEvalTestId", String.valueOf(EVAL_TEST_ID))
            .containsEntry("name", "scenario")
            .containsEntry("type", "SINGLE_TURN")
            .doesNotContainValue("confidential message")
            .doesNotContainValue("confidential output")
            .doesNotContainValue("confidential persona");

        aiAgentEvalFacade.updateAiAgentEvalScenario(
            EVAL_SCENARIO_ID, "renamed scenario", "confidential message", null, null, null, null);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_SCENARIO_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentEvalScenarioId", String.valueOf(EVAL_SCENARIO_ID))
            .containsEntry("name", "renamed scenario")
            .doesNotContainValue("confidential message");

        aiAgentEvalFacade.deleteAiAgentEvalScenario(EVAL_SCENARIO_ID);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_SCENARIO_DELETED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentEvalScenarioId", String.valueOf(EVAL_SCENARIO_ID));
    }

    @Test
    void testJudgeChangesRecordTheJudgeWithoutItsConfiguration() {
        AiAgentJudge aiAgentJudge = new AiAgentJudge();

        aiAgentJudge.setId(JUDGE_ID);
        aiAgentJudge.setName("renamed judge");

        when(aiAgentJudgeService.createAiAgentJudge(any())).thenAnswer(invocation -> {
            AiAgentJudge createdAiAgentJudge = invocation.getArgument(0);

            createdAiAgentJudge.setId(JUDGE_ID);

            return createdAiAgentJudge;
        });
        when(aiAgentJudgeService.getAgentJudge(JUDGE_ID)).thenReturn(aiAgentJudge);
        when(aiAgentJudgeService.updateAiAgentJudge(any())).thenReturn(aiAgentJudge);

        aiAgentEvalFacade.createAiAgentJudge(
            WORKFLOW_ID, WORKFLOW_NODE_NAME, "judge", AiAgentJudgeType.CONTAINS_TEXT,
            Map.of("text", "confidential text"));

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_JUDGE_CREATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentJudgeId", String.valueOf(JUDGE_ID))
            .containsEntry("name", "judge")
            .containsEntry("type", "CONTAINS_TEXT")
            .containsEntry("workflowId", WORKFLOW_ID)
            .containsEntry("workflowNodeName", WORKFLOW_NODE_NAME)
            .doesNotContainKey("configuration");

        aiAgentEvalFacade.updateAiAgentJudge(JUDGE_ID, "renamed judge", Map.of("text", "confidential text"));

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_JUDGE_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentJudgeId", String.valueOf(JUDGE_ID))
            .containsEntry("name", "renamed judge")
            .doesNotContainKey("configuration");

        aiAgentEvalFacade.deleteAiAgentJudge(JUDGE_ID);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_JUDGE_DELETED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentJudgeId", String.valueOf(JUDGE_ID));
    }

    @Test
    void testScenarioJudgeChangesRecordTheJudgeAndItsScenario() {
        AiAgentScenarioJudge aiAgentScenarioJudge = new AiAgentScenarioJudge();

        aiAgentScenarioJudge.setId(SCENARIO_JUDGE_ID);
        aiAgentScenarioJudge.setName("renamed scenario judge");

        when(aiAgentScenarioJudgeService.createAiAgentScenarioJudge(any())).thenAnswer(invocation -> {
            AiAgentScenarioJudge createdAiAgentScenarioJudge = invocation.getArgument(0);

            createdAiAgentScenarioJudge.setId(SCENARIO_JUDGE_ID);

            return createdAiAgentScenarioJudge;
        });
        when(aiAgentScenarioJudgeService.getAgentScenarioJudge(SCENARIO_JUDGE_ID)).thenReturn(aiAgentScenarioJudge);
        when(aiAgentScenarioJudgeService.updateAiAgentScenarioJudge(any())).thenReturn(aiAgentScenarioJudge);

        aiAgentEvalFacade.createAiAgentScenarioJudge(
            EVAL_SCENARIO_ID, "scenario judge", AiAgentJudgeType.REGEX_MATCH, Map.of("pattern", "confidential"));

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_JUDGE_CREATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentScenarioJudgeId", String.valueOf(SCENARIO_JUDGE_ID))
            .containsEntry("aiAgentEvalScenarioId", String.valueOf(EVAL_SCENARIO_ID))
            .containsEntry("name", "scenario judge")
            .containsEntry("type", "REGEX_MATCH")
            .doesNotContainKey("configuration");

        aiAgentEvalFacade.updateAiAgentScenarioJudge(SCENARIO_JUDGE_ID, "renamed scenario judge", null);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_JUDGE_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentScenarioJudgeId", String.valueOf(SCENARIO_JUDGE_ID))
            .containsEntry("name", "renamed scenario judge");

        aiAgentEvalFacade.deleteAiAgentScenarioJudge(SCENARIO_JUDGE_ID);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_JUDGE_DELETED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentScenarioJudgeId", String.valueOf(SCENARIO_JUDGE_ID));
    }

    @Test
    void testToolSimulationChangesRecordTheToolWithoutItsPrompt() {
        AiAgentScenarioToolSimulation aiAgentScenarioToolSimulation = new AiAgentScenarioToolSimulation();

        aiAgentScenarioToolSimulation.setId(TOOL_SIMULATION_ID);
        aiAgentScenarioToolSimulation.setToolName("renamedTool");

        when(aiAgentScenarioToolSimulationService.createAiAgentScenarioToolSimulation(any())).thenAnswer(
            invocation -> {
                AiAgentScenarioToolSimulation createdAiAgentScenarioToolSimulation = invocation.getArgument(0);

                createdAiAgentScenarioToolSimulation.setId(TOOL_SIMULATION_ID);

                return createdAiAgentScenarioToolSimulation;
            });
        when(aiAgentScenarioToolSimulationService.getAgentScenarioToolSimulation(TOOL_SIMULATION_ID))
            .thenReturn(aiAgentScenarioToolSimulation);
        when(aiAgentScenarioToolSimulationService.updateAiAgentScenarioToolSimulation(any()))
            .thenReturn(aiAgentScenarioToolSimulation);

        aiAgentEvalFacade.createAiAgentScenarioToolSimulation(
            EVAL_SCENARIO_ID, "tool", "confidential prompt", null);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_TOOL_SIMULATION_CREATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentScenarioToolSimulationId", String.valueOf(TOOL_SIMULATION_ID))
            .containsEntry("aiAgentEvalScenarioId", String.valueOf(EVAL_SCENARIO_ID))
            .containsEntry("toolName", "tool")
            .doesNotContainValue("confidential prompt");

        aiAgentEvalFacade.updateAiAgentScenarioToolSimulation(
            TOOL_SIMULATION_ID, "renamedTool", "confidential prompt", null);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_TOOL_SIMULATION_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentScenarioToolSimulationId", String.valueOf(TOOL_SIMULATION_ID))
            .containsEntry("toolName", "renamedTool")
            .doesNotContainValue("confidential prompt");

        aiAgentEvalFacade.deleteAiAgentScenarioToolSimulation(TOOL_SIMULATION_ID);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_SCENARIO_TOOL_SIMULATION_DELETED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentScenarioToolSimulationId", String.valueOf(TOOL_SIMULATION_ID));
    }

    @Test
    void testEvalRunStartAndCancelRecordTheRun() {
        AiAgentEvalTest aiAgentEvalTest = new AiAgentEvalTest();

        aiAgentEvalTest.setId(EVAL_TEST_ID);
        aiAgentEvalTest.setWorkflowId(WORKFLOW_ID);
        aiAgentEvalTest.setWorkflowNodeName(WORKFLOW_NODE_NAME);

        AiAgentEvalRun aiAgentEvalRun = new AiAgentEvalRun();

        aiAgentEvalRun.setId(EVAL_RUN_ID);

        when(aiAgentEvalTestService.getAgentEvalTest(EVAL_TEST_ID)).thenReturn(aiAgentEvalTest);
        when(aiAgentEvalRunService.createAgentEvalRun(any())).thenAnswer(invocation -> {
            AiAgentEvalRun createdAiAgentEvalRun = invocation.getArgument(0);

            createdAiAgentEvalRun.setId(EVAL_RUN_ID);

            return createdAiAgentEvalRun;
        });
        when(aiAgentEvalRunService.getAgentEvalRun(EVAL_RUN_ID)).thenReturn(aiAgentEvalRun);
        when(aiAgentEvalRunService.updateAgentEvalRun(any())).thenReturn(aiAgentEvalRun);

        aiAgentEvalRunFacade.startEvalRun(EVAL_TEST_ID, "run", ENVIRONMENT_ID, null, null);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_RUN_STARTED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentEvalRunId", String.valueOf(EVAL_RUN_ID))
            .containsEntry("aiAgentEvalTestId", String.valueOf(EVAL_TEST_ID))
            .containsEntry("name", "run")
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID));

        aiAgentEvalRunFacade.cancelEvalRun(EVAL_RUN_ID);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_RUN_CANCELLED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAgentEvalRunId", String.valueOf(EVAL_RUN_ID));
    }

    @Test
    void testCancelOfAnUnknownEvalRunRecordsAnError() {
        when(aiAgentEvalRunService.getAgentEvalRun(anyLong())).thenThrow(new NoSuchElementException());

        assertThatThrownBy(() -> aiAgentEvalRunFacade.cancelEvalRun(EVAL_RUN_ID))
            .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(AiAgentEvalAuditEvents.AI_AGENT_EVAL_RUN_CANCELLED))
            .containsEntry("result", "ERROR")
            .containsEntry("aiAgentEvalRunId", String.valueOf(EVAL_RUN_ID))
            .containsEntry("errorClass", NoSuchElementException.class.getName());
    }

    private Map<String, String> findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        List<PersistentAuditEvent> persistentAuditEvents = pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");

        return persistentAuditEvent.getData();
    }

    @Configuration
    static class AiAgentEvalFacadeConfiguration {

        @Bean
        AiAgentEvalFacade aiAgentEvalFacade(
            AiAgentEvalScenarioService aiAgentEvalScenarioService, AiAgentEvalTestService aiAgentEvalTestService,
            AiAgentJudgeService aiAgentJudgeService, AiAgentScenarioJudgeService aiAgentScenarioJudgeService,
            AiAgentScenarioToolSimulationService aiAgentScenarioToolSimulationService) {

            return new AiAgentEvalFacadeImpl(
                aiAgentEvalScenarioService, aiAgentEvalTestService, aiAgentJudgeService, aiAgentScenarioJudgeService,
                aiAgentScenarioToolSimulationService);
        }

        @Bean
        AiAgentEvalRunFacade aiAgentEvalRunFacade(
            AiAgentEvalRunExecutor aiAgentEvalRunExecutor, AiAgentEvalRunService aiAgentEvalRunService,
            AiAgentEvalScenarioService aiAgentEvalScenarioService, AiAgentEvalTestService aiAgentEvalTestService) {

            return new AiAgentEvalRunFacadeImpl(
                aiAgentEvalRunExecutor, aiAgentEvalRunService, aiAgentEvalScenarioService, aiAgentEvalTestService);
        }
    }
}
