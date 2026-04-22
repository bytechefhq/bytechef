/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiPrompt;
import com.bytechef.ee.automation.ai.gateway.repository.AiPromptRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiPromptServiceImpl implements AiPromptService {

    private final AiPromptRepository aiPromptRepository;

    AiPromptServiceImpl(AiPromptRepository aiPromptRepository) {
        this.aiPromptRepository = aiPromptRepository;
    }

    @Override
    public AiPrompt create(AiPrompt prompt) {
        Validate.notNull(prompt, "prompt must not be null");
        Validate.isTrue(prompt.getId() == null, "prompt id must be null for creation");

        return aiPromptRepository.save(prompt);
    }

    @Override
    public void delete(long id) {
        aiPromptRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiPrompt getPrompt(long id) {
        return aiPromptRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiPrompt not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiPrompt> getPromptsByWorkspace(Long workspaceId) {
        return aiPromptRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiPrompt> getPromptByName(Long workspaceId, Long projectId, String name) {
        return aiPromptRepository.findByWorkspaceIdAndProjectIdAndName(workspaceId, projectId, name);
    }

    @Override
    public AiPrompt update(AiPrompt prompt) {
        Validate.notNull(prompt, "prompt must not be null");
        Validate.notNull(prompt.getId(), "prompt id must not be null for update");

        return aiPromptRepository.save(prompt);
    }
}
