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

import com.bytechef.ai.agent.eval.domain.AiAgentEvalTest;
import com.bytechef.ai.agent.eval.repository.AiAgentEvalTestRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AgentEvalTestServiceImpl implements AiAgentEvalTestService {

    private final AiAgentEvalTestRepository agentEvalTestRepository;

    AgentEvalTestServiceImpl(AiAgentEvalTestRepository agentEvalTestRepository) {
        this.agentEvalTestRepository = agentEvalTestRepository;
    }

    @Override
    public AiAgentEvalTest createAiAgentEvalTest(AiAgentEvalTest aiAgentEvalTest) {
        return agentEvalTestRepository.save(aiAgentEvalTest);
    }

    @Override
    public void deleteAiAgentEvalTest(long id) {
        agentEvalTestRepository.deleteById(id);
    }

    @Override
    public void deleteAgentEvalTestsByWorkflowId(String workflowId) {
        List<AiAgentEvalTest> tests = agentEvalTestRepository.findByWorkflowId(workflowId);

        agentEvalTestRepository.deleteAll(tests);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiAgentEvalTest> fetchAgentEvalTest(long id) {
        return agentEvalTestRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentEvalTest getAgentEvalTest(long id) {
        return agentEvalTestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiAgentEvalTest not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentEvalTest> getAgentEvalTests(String workflowId, String workflowNodeName) {
        return agentEvalTestRepository.findByWorkflowIdAndWorkflowNodeName(workflowId, workflowNodeName);
    }

    @Override
    public AiAgentEvalTest updateAiAgentEvalTest(AiAgentEvalTest aiAgentEvalTest) {
        return agentEvalTestRepository.save(aiAgentEvalTest);
    }
}
