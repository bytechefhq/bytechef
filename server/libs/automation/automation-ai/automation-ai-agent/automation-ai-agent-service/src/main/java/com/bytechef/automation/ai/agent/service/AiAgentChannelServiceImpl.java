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

import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link AiAgentChannelService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiAgentChannelServiceImpl implements AiAgentChannelService {

    private final AiAgentChannelRepository agentChannelRepository;

    public AiAgentChannelServiceImpl(AiAgentChannelRepository agentChannelRepository) {
        this.agentChannelRepository = agentChannelRepository;
    }

    @Override
    public AiAgentChannel create(AiAgentChannel agentChannel) {
        return agentChannelRepository.save(agentChannel);
    }

    @Override
    public void delete(long id) {
        agentChannelRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentChannel getAgentChannel(long id) {
        return agentChannelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiAgentChannel with id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentChannel> getByAgentId(long agentId) {
        return agentChannelRepository.findAllByAgentIdOrderByPositionAsc(agentId);
    }

    @Override
    public AiAgentChannel update(AiAgentChannel agentChannel) {
        AiAgentChannel currentAgentChannel = agentChannelRepository.findById(agentChannel.getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "AiAgentChannel with id " + agentChannel.getId() + " not found"));

        currentAgentChannel.setChannelType(agentChannel.getChannelType());
        currentAgentChannel.setPosition(agentChannel.getPosition());
        currentAgentChannel.setParameters(agentChannel.getParameters());
        currentAgentChannel.setConnectionId(agentChannel.getConnectionId());

        return agentChannelRepository.save(currentAgentChannel);
    }
}
