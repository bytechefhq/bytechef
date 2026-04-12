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

import com.bytechef.ai.agent.eval.domain.AiAgentEvalScenario;
import com.bytechef.ai.agent.eval.repository.AiAgentEvalScenarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AgentEvalScenarioServiceImpl implements AiAgentEvalScenarioService {

    private final AiAgentEvalScenarioRepository agentEvalScenarioRepository;

    AgentEvalScenarioServiceImpl(AiAgentEvalScenarioRepository agentEvalScenarioRepository) {
        this.agentEvalScenarioRepository = agentEvalScenarioRepository;
    }

    @Override
    public AiAgentEvalScenario createAiAgentEvalScenario(AiAgentEvalScenario aiAgentEvalScenario) {
        int maxTurns = aiAgentEvalScenario.getMaxTurns();

        if (maxTurns < 1 || maxTurns > 50) {
            aiAgentEvalScenario.setMaxTurns(10);
        }

        return agentEvalScenarioRepository.save(aiAgentEvalScenario);
    }

    @Override
    public void deleteAiAgentEvalScenario(long id) {
        agentEvalScenarioRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentEvalScenario getAgentEvalScenario(long id) {
        return agentEvalScenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiAgentEvalScenario not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentEvalScenario> getAgentEvalScenarios(long agentEvalTestId) {
        return agentEvalScenarioRepository.findByAgentEvalTestId(agentEvalTestId);
    }

    @Override
    public AiAgentEvalScenario updateAiAgentEvalScenario(AiAgentEvalScenario aiAgentEvalScenario) {
        int maxTurns = aiAgentEvalScenario.getMaxTurns();

        if (maxTurns < 1 || maxTurns > 50) {
            aiAgentEvalScenario.setMaxTurns(10);
        }

        return agentEvalScenarioRepository.save(aiAgentEvalScenario);
    }
}
