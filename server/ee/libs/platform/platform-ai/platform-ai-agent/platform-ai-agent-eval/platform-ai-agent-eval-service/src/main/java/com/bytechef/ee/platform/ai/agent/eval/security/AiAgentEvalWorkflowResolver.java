/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.eval.security;

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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("aiAgentEvalWorkflowResolver")
public class AiAgentEvalWorkflowResolver {

    private final AiAgentEvalRunService agentEvalRunService;
    private final AiAgentEvalScenarioService agentEvalScenarioService;
    private final AiAgentEvalTestService agentEvalTestService;
    private final AiAgentJudgeService agentJudgeService;
    private final AiAgentScenarioJudgeService agentScenarioJudgeService;
    private final AiAgentScenarioToolSimulationService agentScenarioToolSimulationService;

    @SuppressFBWarnings("EI")
    public AiAgentEvalWorkflowResolver(
        AiAgentEvalRunService agentEvalRunService, AiAgentEvalScenarioService agentEvalScenarioService,
        AiAgentEvalTestService agentEvalTestService, AiAgentJudgeService agentJudgeService,
        AiAgentScenarioJudgeService agentScenarioJudgeService,
        AiAgentScenarioToolSimulationService agentScenarioToolSimulationService) {

        this.agentEvalRunService = agentEvalRunService;
        this.agentEvalScenarioService = agentEvalScenarioService;
        this.agentEvalTestService = agentEvalTestService;
        this.agentJudgeService = agentJudgeService;
        this.agentScenarioJudgeService = agentScenarioJudgeService;
        this.agentScenarioToolSimulationService = agentScenarioToolSimulationService;
    }

    public String getWorkflowId(String resourceType, long id) {
        return switch (resourceType) {
            case "AiAgentEvalRun" -> getAgentEvalRun(id).getWorkflowId();
            case "AiAgentEvalScenario" -> getAgentEvalScenarioWorkflowId(id);
            case "AiAgentEvalTest" -> getAgentEvalTestWorkflowId(id);
            case "AiAgentJudge" -> {
                AiAgentJudge agentJudge = agentJudgeService.getAgentJudge(id);

                yield agentJudge.getWorkflowId();
            }
            case "AiAgentScenarioJudge" -> {
                AiAgentScenarioJudge agentScenarioJudge = agentScenarioJudgeService.getAgentScenarioJudge(id);

                yield getAgentEvalScenarioWorkflowId(agentScenarioJudge.getAgentEvalScenarioId());
            }
            case "AiAgentScenarioToolSimulation" -> {
                AiAgentScenarioToolSimulation toolSimulation =
                    agentScenarioToolSimulationService.getAgentScenarioToolSimulation(id);

                yield getAgentEvalScenarioWorkflowId(toolSimulation.getAgentEvalScenarioId());
            }
            default -> throw new IllegalArgumentException("Unsupported resource type: " + resourceType);
        };
    }

    public Long getEnvironmentId(String resourceType, long id) {
        if (!"AiAgentEvalRun".equals(resourceType)) {
            throw new IllegalArgumentException("Unsupported resource type: " + resourceType);
        }

        return getAgentEvalRun(id).getEnvironmentId();
    }

    private AiAgentEvalRun getAgentEvalRun(long id) {
        return agentEvalRunService.getAgentEvalRun(id);
    }

    private String getAgentEvalScenarioWorkflowId(long agentEvalScenarioId) {
        AiAgentEvalScenario agentEvalScenario = agentEvalScenarioService.getAgentEvalScenario(agentEvalScenarioId);

        return getAgentEvalTestWorkflowId(agentEvalScenario.getAgentEvalTestId());
    }

    private String getAgentEvalTestWorkflowId(long agentEvalTestId) {
        AiAgentEvalTest agentEvalTest = agentEvalTestService.getAgentEvalTest(agentEvalTestId);

        return agentEvalTest.getWorkflowId();
    }
}
