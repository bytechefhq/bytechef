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

import com.bytechef.ai.agent.eval.domain.AgentJudgeVerdict;
import com.bytechef.ai.agent.eval.repository.AgentJudgeVerdictRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AgentJudgeVerdictServiceImpl implements AgentJudgeVerdictService {

    private final AgentJudgeVerdictRepository agentJudgeVerdictRepository;

    AgentJudgeVerdictServiceImpl(AgentJudgeVerdictRepository agentJudgeVerdictRepository) {
        this.agentJudgeVerdictRepository = agentJudgeVerdictRepository;
    }

    @Override
    public AgentJudgeVerdict createAgentJudgeVerdict(AgentJudgeVerdict agentJudgeVerdict) {
        return agentJudgeVerdictRepository.save(agentJudgeVerdict);
    }

    @Override
    public void deleteAgentJudgeVerdict(long id) {
        agentJudgeVerdictRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentJudgeVerdict getAgentJudgeVerdict(long id) {
        return agentJudgeVerdictRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AgentJudgeVerdict not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentJudgeVerdict> getAgentJudgeVerdicts(long agentEvalResultId) {
        return agentJudgeVerdictRepository.findByAgentEvalResultId(agentEvalResultId);
    }
}
