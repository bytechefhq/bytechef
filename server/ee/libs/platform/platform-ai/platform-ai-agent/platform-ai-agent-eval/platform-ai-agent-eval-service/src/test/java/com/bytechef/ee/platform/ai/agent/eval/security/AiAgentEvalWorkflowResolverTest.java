/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.eval.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalRun;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalScenario;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalTest;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentJudge;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentScenarioJudge;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentScenarioToolSimulation;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalRunService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalScenarioService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalTestService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentJudgeService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentScenarioJudgeService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentScenarioToolSimulationService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiAgentEvalWorkflowResolverTest {

    private static final String WORKFLOW_ID = "workflow-1";

    private final AiAgentEvalRunService agentEvalRunService = mock(AiAgentEvalRunService.class);
    private final AiAgentEvalScenarioService agentEvalScenarioService = mock(AiAgentEvalScenarioService.class);
    private final AiAgentEvalTestService agentEvalTestService = mock(AiAgentEvalTestService.class);
    private final AiAgentJudgeService agentJudgeService = mock(AiAgentJudgeService.class);
    private final AiAgentScenarioJudgeService agentScenarioJudgeService = mock(AiAgentScenarioJudgeService.class);
    private final AiAgentScenarioToolSimulationService agentScenarioToolSimulationService =
        mock(AiAgentScenarioToolSimulationService.class);

    private final AiAgentEvalWorkflowResolver aiAgentEvalWorkflowResolver = new AiAgentEvalWorkflowResolver(
        agentEvalRunService, agentEvalScenarioService, agentEvalTestService, agentJudgeService,
        agentScenarioJudgeService, agentScenarioToolSimulationService);

    @BeforeEach
    void beforeEach() {
        AiAgentEvalTest agentEvalTest = new AiAgentEvalTest();

        agentEvalTest.setWorkflowId(WORKFLOW_ID);

        when(agentEvalTestService.getAgentEvalTest(1L)).thenReturn(agentEvalTest);

        AiAgentEvalScenario agentEvalScenario = new AiAgentEvalScenario();

        agentEvalScenario.setAgentEvalTestId(1L);

        when(agentEvalScenarioService.getAgentEvalScenario(2L)).thenReturn(agentEvalScenario);
    }

    @Test
    void testGetWorkflowIdOfATest() {
        assertThat(aiAgentEvalWorkflowResolver.getWorkflowId("AiAgentEvalTest", 1L)).isEqualTo(WORKFLOW_ID);
    }

    @Test
    void testGetWorkflowIdOfAScenarioFollowsItsTest() {
        assertThat(aiAgentEvalWorkflowResolver.getWorkflowId("AiAgentEvalScenario", 2L)).isEqualTo(WORKFLOW_ID);
    }

    @Test
    void testGetWorkflowIdOfAScenarioJudgeAndToolSimulationFollowsTheirScenario() {
        AiAgentScenarioJudge agentScenarioJudge = new AiAgentScenarioJudge();

        agentScenarioJudge.setAgentEvalScenarioId(2L);

        when(agentScenarioJudgeService.getAgentScenarioJudge(3L)).thenReturn(agentScenarioJudge);

        AiAgentScenarioToolSimulation toolSimulation = new AiAgentScenarioToolSimulation();

        toolSimulation.setAgentEvalScenarioId(2L);

        when(agentScenarioToolSimulationService.getAgentScenarioToolSimulation(4L)).thenReturn(toolSimulation);

        assertThat(aiAgentEvalWorkflowResolver.getWorkflowId("AiAgentScenarioJudge", 3L)).isEqualTo(WORKFLOW_ID);
        assertThat(aiAgentEvalWorkflowResolver.getWorkflowId("AiAgentScenarioToolSimulation", 4L))
            .isEqualTo(WORKFLOW_ID);
    }

    @Test
    void testGetWorkflowIdAndEnvironmentIdOfAJudgeAndARun() {
        AiAgentJudge agentJudge = new AiAgentJudge();

        agentJudge.setWorkflowId(WORKFLOW_ID);

        when(agentJudgeService.getAgentJudge(5L)).thenReturn(agentJudge);

        AiAgentEvalRun agentEvalRun = new AiAgentEvalRun();

        agentEvalRun.setWorkflowId(WORKFLOW_ID);
        agentEvalRun.setEnvironmentId(2L);

        when(agentEvalRunService.getAgentEvalRun(6L)).thenReturn(agentEvalRun);

        assertThat(aiAgentEvalWorkflowResolver.getWorkflowId("AiAgentJudge", 5L)).isEqualTo(WORKFLOW_ID);
        assertThat(aiAgentEvalWorkflowResolver.getWorkflowId("AiAgentEvalRun", 6L)).isEqualTo(WORKFLOW_ID);
        assertThat(aiAgentEvalWorkflowResolver.getEnvironmentId("AiAgentEvalRun", 6L)).isEqualTo(2L);
    }

    @Test
    void testAMissingEntityFailsInsteadOfResolvingToNoWorkflow() {
        when(agentJudgeService.getAgentJudge(7L)).thenThrow(new NoSuchElementException("not found"));

        assertThatThrownBy(() -> aiAgentEvalWorkflowResolver.getWorkflowId("AiAgentJudge", 7L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testAnUnknownResourceTypeIsRejected() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> aiAgentEvalWorkflowResolver.getWorkflowId("Workflow", 1L));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> aiAgentEvalWorkflowResolver.getEnvironmentId("AiAgentEvalTest", 1L));
    }
}
