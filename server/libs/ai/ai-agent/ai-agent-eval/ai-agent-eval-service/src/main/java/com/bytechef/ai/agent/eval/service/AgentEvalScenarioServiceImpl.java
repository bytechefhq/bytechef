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

import com.bytechef.ai.agent.eval.domain.AgentEvalScenario;
import com.bytechef.ai.agent.eval.repository.AgentEvalScenarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AgentEvalScenarioServiceImpl implements AgentEvalScenarioService {

    private final AgentEvalScenarioRepository agentEvalScenarioRepository;

    AgentEvalScenarioServiceImpl(AgentEvalScenarioRepository agentEvalScenarioRepository) {
        this.agentEvalScenarioRepository = agentEvalScenarioRepository;
    }

    @Override
    public AgentEvalScenario createAgentEvalScenario(AgentEvalScenario agentEvalScenario) {
        int maxTurns = agentEvalScenario.getMaxTurns();

        if (maxTurns < 1 || maxTurns > 50) {
            agentEvalScenario.setMaxTurns(10);
        }

        return agentEvalScenarioRepository.save(agentEvalScenario);
    }

    @Override
    public void deleteAgentEvalScenario(long id) {
        agentEvalScenarioRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentEvalScenario getAgentEvalScenario(long id) {
        return agentEvalScenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AgentEvalScenario not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentEvalScenario> getAgentEvalScenarios(long agentEvalTestId) {
        return agentEvalScenarioRepository.findByAgentEvalTestId(agentEvalTestId);
    }

    @Override
    public AgentEvalScenario updateAgentEvalScenario(AgentEvalScenario agentEvalScenario) {
        int maxTurns = agentEvalScenario.getMaxTurns();

        if (maxTurns < 1 || maxTurns > 50) {
            agentEvalScenario.setMaxTurns(10);
        }

        return agentEvalScenarioRepository.save(agentEvalScenario);
    }
}
