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

package com.bytechef.ai.agent.skill.service;

import com.bytechef.ai.agent.skill.domain.AgentSkill;
import com.bytechef.ai.agent.skill.repository.AgentSkillRepository;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AgentSkillServiceImpl implements AgentSkillService {

    private final AgentSkillRepository agentSkillRepository;

    AgentSkillServiceImpl(AgentSkillRepository agentSkillRepository) {
        this.agentSkillRepository = agentSkillRepository;
    }

    @Override
    public AgentSkill createAgentSkill(AgentSkill agentSkill) {
        return agentSkillRepository.save(agentSkill);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return agentSkillRepository.existsByName(name);
    }

    @Override
    public void deleteAgentSkill(long id) {
        agentSkillRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentSkill getAgentSkill(long id) {
        return agentSkillRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AgentSkill not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentSkill> getAgentSkills() {
        return agentSkillRepository.findAll();
    }

    @Override
    public AgentSkill updateAgentSkill(long id, String name, @Nullable String description) {
        AgentSkill existingAgentSkill = getAgentSkill(id);

        existingAgentSkill.setName(name);
        existingAgentSkill.setDescription(description);

        return agentSkillRepository.save(existingAgentSkill);
    }
}
