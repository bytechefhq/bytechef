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

import com.bytechef.ai.agent.eval.domain.AgentJudge;
import com.bytechef.ai.agent.eval.repository.AgentJudgeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AgentJudgeServiceImpl implements AgentJudgeService {

    private final AgentJudgeRepository agentJudgeRepository;

    AgentJudgeServiceImpl(AgentJudgeRepository agentJudgeRepository) {
        this.agentJudgeRepository = agentJudgeRepository;
    }

    @Override
    public AgentJudge createAgentJudge(AgentJudge agentJudge) {
        return agentJudgeRepository.save(agentJudge);
    }

    @Override
    public void deleteAgentJudge(long id) {
        agentJudgeRepository.deleteById(id);
    }

    @Override
    public void deleteAgentJudgesByWorkflowId(String workflowId) {
        List<AgentJudge> judges = agentJudgeRepository.findByWorkflowId(workflowId);

        agentJudgeRepository.deleteAll(judges);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentJudge getAgentJudge(long id) {
        return agentJudgeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AgentJudge not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentJudge> getAgentJudges(String workflowId, String workflowNodeName) {
        return agentJudgeRepository.findByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName);
    }

    @Override
    public AgentJudge updateAgentJudge(AgentJudge agentJudge) {
        return agentJudgeRepository.save(agentJudge);
    }
}
