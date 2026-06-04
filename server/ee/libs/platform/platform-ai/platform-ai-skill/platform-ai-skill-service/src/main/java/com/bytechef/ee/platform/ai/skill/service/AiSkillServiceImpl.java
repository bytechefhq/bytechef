/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.service;

import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import com.bytechef.ee.platform.ai.skill.repository.AiSkillRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
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
