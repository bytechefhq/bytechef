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

package com.bytechef.automation.ai.agent.service;

import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link AiAgentElementService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiAgentElementServiceImpl implements AiAgentElementService {

    private final AiAgentElementRepository agentElementRepository;

    public AiAgentElementServiceImpl(AiAgentElementRepository agentElementRepository) {
        this.agentElementRepository = agentElementRepository;
    }

    @Override
    public AiAgentElement create(AiAgentElement agentElement) {
        return agentElementRepository.save(agentElement);
    }

    @Override
    public void delete(long id) {
        agentElementRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentElement getAgentElement(long id) {
        return agentElementRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiAgentElement with id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentElement> getByAgentId(long agentId) {
        return agentElementRepository.findAllByAgentIdOrderByPositionAsc(agentId);
    }

    @Override
    public AiAgentElement update(AiAgentElement agentElement) {
        AiAgentElement currentAgentElement = agentElementRepository.findById(agentElement.getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "AiAgentElement with id " + agentElement.getId() + " not found"));

        currentAgentElement.setKind(agentElement.getKind());
        currentAgentElement.setReferenceId(agentElement.getReferenceId());
        currentAgentElement.setParameters(agentElement.getParameters());
        currentAgentElement.setConnectionId(agentElement.getConnectionId());
        currentAgentElement.setPosition(agentElement.getPosition());

        return agentElementRepository.save(currentAgentElement);
    }
}
