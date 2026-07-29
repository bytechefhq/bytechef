/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.prompt.service;

import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPromptService;
import com.bytechef.ee.platform.ai.prompt.repository.AiPromptRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-aware operations on {@link AiPrompt}. Delegates entity-level CRUD to the platform {@link AiPromptService};
 * owns the workspace binding (the {@code ai_prompt.workspace_id} column) and the workspace-scoped queries that filter
 * it.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class WorkspaceAiPromptServiceImpl implements WorkspaceAiPromptService {

    private final AiPromptRepository aiPromptRepository;
    private final AiPromptService aiPromptService;

    WorkspaceAiPromptServiceImpl(AiPromptRepository aiPromptRepository, AiPromptService aiPromptService) {
        this.aiPromptRepository = aiPromptRepository;
        this.aiPromptService = aiPromptService;
    }

    @Override
    public AiPrompt createInWorkspace(AiPrompt prompt, long workspaceId) {
        Validate.notNull(prompt, "prompt must not be null");
        Validate.isTrue(prompt.getId() == null, "prompt id must be null for creation");

        prompt.setWorkspaceId(workspaceId);

        return aiPromptService.create(prompt);
    }

    @Override
    public void deleteInWorkspace(long promptId) {
        aiPromptService.delete(promptId);
    }

    /**
     * Reads the owning workspace straight off the prompt row. Deliberately goes through the repository rather than
     * {@code aiPromptService.getPrompt}: callers use this as an authorization probe and an unknown id must still answer
     * null (what "no membership row" used to mean), whereas getPrompt throws.
     */
    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long promptId) {
        return aiPromptRepository.findById(promptId)
            .map(AiPrompt::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiPrompt> getPromptsByWorkspace(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return aiPromptRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiPrompt> getPromptByName(Long workspaceId, Long projectId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        return aiPromptRepository.findByWorkspaceIdAndProjectIdAndName(workspaceId, projectId, name);
    }
}
