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

package com.bytechef.ai.agent.eval.service;

import com.bytechef.ai.agent.eval.domain.AiAgentScenarioToolSimulation;
import com.bytechef.ai.agent.eval.repository.AiAgentScenarioToolSimulationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AgentScenarioToolSimulationServiceImpl implements AiAgentScenarioToolSimulationService {

    private final AiAgentScenarioToolSimulationRepository agentScenarioToolSimulationRepository;

    AgentScenarioToolSimulationServiceImpl(
        AiAgentScenarioToolSimulationRepository agentScenarioToolSimulationRepository) {

        this.agentScenarioToolSimulationRepository = agentScenarioToolSimulationRepository;
    }

    @Override
    public AiAgentScenarioToolSimulation createAiAgentScenarioToolSimulation(
        AiAgentScenarioToolSimulation toolSimulation) {

        return agentScenarioToolSimulationRepository.save(toolSimulation);
    }

    @Override
    public void deleteAiAgentScenarioToolSimulation(long id) {
        agentScenarioToolSimulationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentScenarioToolSimulation getAgentScenarioToolSimulation(long id) {
        return agentScenarioToolSimulationRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("AiAgentScenarioToolSimulation not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentScenarioToolSimulation> getAgentScenarioToolSimulations(long agentEvalScenarioId) {
        return agentScenarioToolSimulationRepository.findByAgentEvalScenarioId(agentEvalScenarioId);
    }

    @Override
    public AiAgentScenarioToolSimulation updateAiAgentScenarioToolSimulation(
        AiAgentScenarioToolSimulation toolSimulation) {

        return agentScenarioToolSimulationRepository.save(toolSimulation);
    }
}
