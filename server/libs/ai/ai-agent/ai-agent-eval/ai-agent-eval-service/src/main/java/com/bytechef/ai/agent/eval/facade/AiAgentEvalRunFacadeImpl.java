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

package com.bytechef.ai.agent.eval.facade;

import com.bytechef.ai.agent.eval.constant.AiAgentEvalRunStatus;
import com.bytechef.ai.agent.eval.domain.AiAgentEvalRun;
import com.bytechef.ai.agent.eval.domain.AiAgentEvalScenario;
import com.bytechef.ai.agent.eval.domain.AiAgentEvalTest;
import com.bytechef.ai.agent.eval.executor.AiAgentEvalRunExecutor;
import com.bytechef.ai.agent.eval.service.AiAgentEvalRunService;
import com.bytechef.ai.agent.eval.service.AiAgentEvalScenarioService;
import com.bytechef.ai.agent.eval.service.AiAgentEvalTestService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
@Component
@Transactional
class AiAgentEvalRunFacadeImpl implements AiAgentEvalRunFacade {

    private static final Logger logger = LoggerFactory.getLogger(AiAgentEvalRunFacadeImpl.class);

    private final AiAgentEvalRunExecutor agentEvalRunExecutor;
    private final AiAgentEvalRunService agentEvalRunService;
    private final AiAgentEvalScenarioService agentEvalScenarioService;
    private final AiAgentEvalTestService agentEvalTestService;

    AiAgentEvalRunFacadeImpl(
        AiAgentEvalRunExecutor agentEvalRunExecutor, AiAgentEvalRunService agentEvalRunService,
        AiAgentEvalScenarioService agentEvalScenarioService, AiAgentEvalTestService agentEvalTestService) {

        this.agentEvalRunExecutor = agentEvalRunExecutor;
        this.agentEvalRunService = agentEvalRunService;
        this.agentEvalScenarioService = agentEvalScenarioService;
        this.agentEvalTestService = agentEvalTestService;
    }

    @Override
    public AiAgentEvalRun startEvalRun(
        long agentEvalTestId, String name, long environmentId,
        List<Long> scenarioIds, List<Long> aiAgentJudgeIds) {

        AiAgentEvalTest aiAgentEvalTest = agentEvalTestService.getAgentEvalTest(agentEvalTestId);

        List<AiAgentEvalScenario> allScenarios = agentEvalScenarioService.getAgentEvalScenarios(agentEvalTestId);

        List<AiAgentEvalScenario> scenarios = (scenarioIds == null || scenarioIds.isEmpty())
            ? allScenarios
            : allScenarios.stream()
                .filter(scenario -> scenarioIds.contains(scenario.getId()))
                .toList();

        int totalRuns = scenarios.stream()
            .mapToInt(scenario -> Math.max(1, scenario.getNumberOfRuns()))
            .sum();

        AiAgentEvalRun aiAgentEvalRun = new AiAgentEvalRun();

        aiAgentEvalRun.setAgentEvalTestId(agentEvalTestId);
        aiAgentEvalRun.setWorkflowId(aiAgentEvalTest.getWorkflowId());
        aiAgentEvalRun.setWorkflowNodeName(aiAgentEvalTest.getWorkflowNodeName());
        aiAgentEvalRun.setEnvironmentId(environmentId);
        aiAgentEvalRun.setName(name);
        aiAgentEvalRun.setStatus(AiAgentEvalRunStatus.PENDING);
        aiAgentEvalRun.setTotalScenarios(totalRuns);
        aiAgentEvalRun.setCompletedScenarios(0);

        aiAgentEvalRun = agentEvalRunService.createAgentEvalRun(aiAgentEvalRun);

        long evalRunId = aiAgentEvalRun.getId();

        List<Long> finalScenarioIds = scenarios.stream()
            .map(AiAgentEvalScenario::getId)
            .toList();

        List<Long> finalAgentJudgeIds = (aiAgentJudgeIds == null) ? List.of() : aiAgentJudgeIds;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                agentEvalRunExecutor.executeRunAsync(evalRunId, finalScenarioIds, finalAgentJudgeIds);
            }
        });

        return aiAgentEvalRun;
    }

    @Override
    public AiAgentEvalRun cancelEvalRun(long id) {
        AiAgentEvalRun aiAgentEvalRun = agentEvalRunService.getAgentEvalRun(id);

        aiAgentEvalRun.setStatus(AiAgentEvalRunStatus.FAILED);

        return agentEvalRunService.updateAgentEvalRun(aiAgentEvalRun);
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanedRuns() {
        List<AiAgentEvalRun> orphanedRuns = agentEvalRunService.getAgentEvalRunsByStatus(AiAgentEvalRunStatus.RUNNING);

        for (AiAgentEvalRun orphanedRun : orphanedRuns) {
            logger.warn("Recovering orphaned eval run {}: server restarted during execution", orphanedRun.getId());

            orphanedRun.setStatus(AiAgentEvalRunStatus.FAILED);

            agentEvalRunService.updateAgentEvalRun(orphanedRun);
        }
    }
}
