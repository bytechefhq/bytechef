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

import com.bytechef.ai.agent.eval.domain.AiAgentScenarioJudge;
import com.bytechef.ai.agent.eval.repository.AiAgentScenarioJudgeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AgentScenarioJudgeServiceImpl implements AiAgentScenarioJudgeService {

    private final AiAgentScenarioJudgeRepository agentScenarioJudgeRepository;

    AgentScenarioJudgeServiceImpl(AiAgentScenarioJudgeRepository agentScenarioJudgeRepository) {
        this.agentScenarioJudgeRepository = agentScenarioJudgeRepository;
    }

    @Override
    public AiAgentScenarioJudge createAiAgentScenarioJudge(AiAgentScenarioJudge agentScenarioJudge) {
        return agentScenarioJudgeRepository.save(agentScenarioJudge);
    }

    @Override
    public void deleteAiAgentScenarioJudge(long id) {
        agentScenarioJudgeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentScenarioJudge getAgentScenarioJudge(long id) {
        return agentScenarioJudgeRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("AiAgentScenarioJudge not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentScenarioJudge> getAgentScenarioJudges(long agentEvalScenarioId) {
        return agentScenarioJudgeRepository.findByAgentEvalScenarioId(agentEvalScenarioId);
    }

    @Override
    public AiAgentScenarioJudge updateAiAgentScenarioJudge(AiAgentScenarioJudge agentScenarioJudge) {
        return agentScenarioJudgeRepository.save(agentScenarioJudge);
    }
}
