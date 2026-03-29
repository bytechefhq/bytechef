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

import com.bytechef.ai.agent.eval.constant.AgentEvalRunStatus;
import com.bytechef.ai.agent.eval.domain.AgentEvalRun;
import com.bytechef.ai.agent.eval.domain.AgentEvalScenario;
import com.bytechef.ai.agent.eval.domain.AgentEvalTest;
import com.bytechef.ai.agent.eval.executor.AgentEvalRunExecutor;
import com.bytechef.ai.agent.eval.service.AgentEvalRunService;
import com.bytechef.ai.agent.eval.service.AgentEvalScenarioService;
import com.bytechef.ai.agent.eval.service.AgentEvalTestService;
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
class AgentEvalRunFacadeImpl implements AgentEvalRunFacade {

    private static final Logger logger = LoggerFactory.getLogger(AgentEvalRunFacadeImpl.class);

    private final AgentEvalRunExecutor agentEvalRunExecutor;
    private final AgentEvalRunService agentEvalRunService;
    private final AgentEvalScenarioService agentEvalScenarioService;
    private final AgentEvalTestService agentEvalTestService;

    AgentEvalRunFacadeImpl(
        AgentEvalRunExecutor agentEvalRunExecutor, AgentEvalRunService agentEvalRunService,
        AgentEvalScenarioService agentEvalScenarioService, AgentEvalTestService agentEvalTestService) {

        this.agentEvalRunExecutor = agentEvalRunExecutor;
        this.agentEvalRunService = agentEvalRunService;
        this.agentEvalScenarioService = agentEvalScenarioService;
        this.agentEvalTestService = agentEvalTestService;
    }

    @Override
    public AgentEvalRun startEvalRun(
        long agentEvalTestId, String name, long environmentId,
        List<Long> scenarioIds, List<Long> agentJudgeIds) {

        AgentEvalTest agentEvalTest = agentEvalTestService.getAgentEvalTest(agentEvalTestId);

        List<AgentEvalScenario> allScenarios = agentEvalScenarioService.getAgentEvalScenarios(agentEvalTestId);

        List<AgentEvalScenario> scenarios = (scenarioIds == null || scenarioIds.isEmpty())
            ? allScenarios
            : allScenarios.stream()
                .filter(scenario -> scenarioIds.contains(scenario.getId()))
                .toList();

        int totalRuns = scenarios.stream()
            .mapToInt(scenario -> Math.max(1, scenario.getNumberOfRuns()))
            .sum();

        AgentEvalRun agentEvalRun = new AgentEvalRun();

        agentEvalRun.setAgentEvalTestId(agentEvalTestId);
        agentEvalRun.setWorkflowId(agentEvalTest.getWorkflowId());
        agentEvalRun.setWorkflowNodeName(agentEvalTest.getWorkflowNodeName());
        agentEvalRun.setEnvironmentId(environmentId);
        agentEvalRun.setName(name);
        agentEvalRun.setStatus(AgentEvalRunStatus.PENDING);
        agentEvalRun.setTotalScenarios(totalRuns);
        agentEvalRun.setCompletedScenarios(0);

        agentEvalRun = agentEvalRunService.createAgentEvalRun(agentEvalRun);

        long evalRunId = agentEvalRun.getId();

        List<Long> finalScenarioIds = scenarios.stream()
            .map(AgentEvalScenario::getId)
            .toList();

        List<Long> finalAgentJudgeIds = (agentJudgeIds == null) ? List.of() : agentJudgeIds;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                agentEvalRunExecutor.executeRunAsync(evalRunId, finalScenarioIds, finalAgentJudgeIds);
            }
        });

        return agentEvalRun;
    }

    @Override
    public AgentEvalRun cancelEvalRun(long id) {
        AgentEvalRun agentEvalRun = agentEvalRunService.getAgentEvalRun(id);

        agentEvalRun.setStatus(AgentEvalRunStatus.FAILED);

        return agentEvalRunService.updateAgentEvalRun(agentEvalRun);
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanedRuns() {
        List<AgentEvalRun> orphanedRuns = agentEvalRunService.getAgentEvalRunsByStatus(AgentEvalRunStatus.RUNNING);

        for (AgentEvalRun orphanedRun : orphanedRuns) {
            logger.warn("Recovering orphaned eval run {}: server restarted during execution", orphanedRun.getId());

            orphanedRun.setStatus(AgentEvalRunStatus.FAILED);

            agentEvalRunService.updateAgentEvalRun(orphanedRun);
        }
    }
}
