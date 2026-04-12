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

import com.bytechef.ai.agent.skill.domain.AiAgentSkill;
import com.bytechef.ai.agent.skill.repository.AiAgentSkillRepository;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AiAgentSkillServiceImpl implements AiAgentSkillService {

    private final AiAgentSkillRepository aiAgentSkillRepository;

    AiAgentSkillServiceImpl(AiAgentSkillRepository aiAgentSkillRepository) {
        this.aiAgentSkillRepository = aiAgentSkillRepository;
    }

    @Override
    public AiAgentSkill createAiAgentSkill(AiAgentSkill aiAgentSkill) {
        return aiAgentSkillRepository.save(aiAgentSkill);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return aiAgentSkillRepository.existsByName(name);
    }

    @Override
    public void deleteAiAgentSkill(long id) {
        aiAgentSkillRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentSkill getAiAgentSkill(long id) {
        return aiAgentSkillRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiAgentSkill not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentSkill> getAiAgentSkills() {
        return aiAgentSkillRepository.findAll();
    }

    @Override
    public AiAgentSkill updateAiAgentSkill(long id, String name, @Nullable String description) {
        AiAgentSkill existingAiAgentSkill = getAiAgentSkill(id);

        existingAiAgentSkill.setName(name);
        existingAiAgentSkill.setDescription(description);

        return aiAgentSkillRepository.save(existingAiAgentSkill);
    }
}
