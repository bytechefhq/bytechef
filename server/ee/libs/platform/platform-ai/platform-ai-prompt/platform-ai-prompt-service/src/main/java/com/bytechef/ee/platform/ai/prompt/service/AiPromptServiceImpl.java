/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.prompt.service;

import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPromptService;
import com.bytechef.ee.platform.ai.prompt.repository.AiPromptRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-agnostic CRUD on {@link AiPrompt}. The workspace membership row in {@code workspace_ai_prompt} and any
 * workspace-scoped query is owned by {@code com.bytechef.ee.automation.ai.prompt.service.WorkspaceAiPromptServiceImpl}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
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
    public AiPrompt update(AiPrompt prompt) {
        Validate.notNull(prompt, "prompt must not be null");
        Validate.notNull(prompt.getId(), "prompt id must not be null for update");

        return aiPromptRepository.save(prompt);
    }
}
