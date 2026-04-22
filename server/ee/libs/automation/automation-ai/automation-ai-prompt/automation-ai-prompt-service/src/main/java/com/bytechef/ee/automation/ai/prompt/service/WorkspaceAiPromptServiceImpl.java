/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.prompt.service;

import com.bytechef.ee.automation.ai.prompt.domain.WorkspaceAiPrompt;
import com.bytechef.ee.automation.ai.prompt.repository.WorkspaceAiPromptRepository;
import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPromptService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-aware operations on {@link AiPrompt}. Delegates entity-level CRUD to the platform {@link AiPromptService};
 * owns the workspace membership row in {@code workspace_ai_prompt} and workspace-scoped queries that join through it.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class WorkspaceAiPromptServiceImpl implements WorkspaceAiPromptService {

    private final AiPromptService aiPromptService;
    private final WorkspaceAiPromptRepository workspaceAiPromptRepository;

    WorkspaceAiPromptServiceImpl(
        AiPromptService aiPromptService, WorkspaceAiPromptRepository workspaceAiPromptRepository) {

        this.aiPromptService = aiPromptService;
        this.workspaceAiPromptRepository = workspaceAiPromptRepository;
    }

    @Override
    public AiPrompt createInWorkspace(AiPrompt prompt, long workspaceId) {
        Validate.notNull(prompt, "prompt must not be null");
        Validate.isTrue(prompt.getId() == null, "prompt id must be null for creation");

        AiPrompt saved = aiPromptService.create(prompt);

        workspaceAiPromptRepository.save(new WorkspaceAiPrompt(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    public void deleteInWorkspace(long promptId) {
        workspaceAiPromptRepository.findByAiPromptId(promptId)
            .ifPresent(membership -> workspaceAiPromptRepository.deleteById(membership.getId()));

        aiPromptService.delete(promptId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long promptId) {
        return workspaceAiPromptRepository.findByAiPromptId(promptId)
            .map(WorkspaceAiPrompt::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiPrompt> getPromptsByWorkspace(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return workspaceAiPromptRepository.findAllPromptsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiPrompt> getPromptByName(Long workspaceId, Long projectId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        return workspaceAiPromptRepository.findPromptByWorkspaceIdAndProjectIdAndName(workspaceId, projectId, name);
    }
}
