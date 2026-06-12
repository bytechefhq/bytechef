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

package com.bytechef.platform.ai.skill.service;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.repository.AiSkillRepository;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AiSkillServiceImpl implements AiSkillService {

    private final AiSkillRepository aiSkillRepository;

    AiSkillServiceImpl(AiSkillRepository aiSkillRepository) {
        this.aiSkillRepository = aiSkillRepository;
    }

    @Override
    public AiSkill createAiSkill(AiSkill aiSkill) {
        return aiSkillRepository.save(aiSkill);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return aiSkillRepository.existsByName(name);
    }

    @Override
    public void deleteAiSkill(long id) {
        aiSkillRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSkill getAiSkill(long id) {
        return aiSkillRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiSkill not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiSkill> getAiSkills() {
        return aiSkillRepository.findAll();
    }

    @Override
    public AiSkill updateAiSkill(long id, String name, @Nullable String description) {
        AiSkill existingAiSkill = getAiSkill(id);

        existingAiSkill.setName(name);
        existingAiSkill.setDescription(description);

        return aiSkillRepository.save(existingAiSkill);
    }

    @Override
    public AiSkill updateAiSkillFile(long id, FileEntry fileEntry) {
        AiSkill existingAiSkill = getAiSkill(id);

        existingAiSkill.setSkillFile(fileEntry);

        return aiSkillRepository.save(existingAiSkill);
    }
}
