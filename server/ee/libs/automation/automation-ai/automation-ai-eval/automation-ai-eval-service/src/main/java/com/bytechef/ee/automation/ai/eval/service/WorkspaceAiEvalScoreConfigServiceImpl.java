/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.automation.ai.eval.domain.WorkspaceAiEvalScoreConfig;
import com.bytechef.ee.automation.ai.eval.repository.WorkspaceAiEvalScoreConfigRepository;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreConfigService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class WorkspaceAiEvalScoreConfigServiceImpl implements WorkspaceAiEvalScoreConfigService {

    private final AiEvalScoreConfigService aiEvalScoreConfigService;
    private final WorkspaceAiEvalScoreConfigRepository workspaceAiEvalScoreConfigRepository;

    WorkspaceAiEvalScoreConfigServiceImpl(
        AiEvalScoreConfigService aiEvalScoreConfigService,
        WorkspaceAiEvalScoreConfigRepository workspaceAiEvalScoreConfigRepository) {

        this.aiEvalScoreConfigService = aiEvalScoreConfigService;
        this.workspaceAiEvalScoreConfigRepository = workspaceAiEvalScoreConfigRepository;
    }

    @Override
    public AiEvalScoreConfig createInWorkspace(AiEvalScoreConfig scoreConfig, long workspaceId) {
        Validate.notNull(scoreConfig, "scoreConfig must not be null");

        AiEvalScoreConfig saved = aiEvalScoreConfigService.create(scoreConfig);

        workspaceAiEvalScoreConfigRepository.save(new WorkspaceAiEvalScoreConfig(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    public void deleteInWorkspace(long scoreConfigId) {
        workspaceAiEvalScoreConfigRepository.findByAiEvalScoreConfigId(scoreConfigId)
            .ifPresent(membership -> workspaceAiEvalScoreConfigRepository.deleteById(membership.getId()));

        aiEvalScoreConfigService.delete(scoreConfigId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiEvalScoreConfig> fetchScoreConfigByWorkspaceIdAndName(Long workspaceId, String name) {
        return workspaceAiEvalScoreConfigRepository.findConfigByWorkspaceIdAndName(workspaceId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long scoreConfigId) {
        return workspaceAiEvalScoreConfigRepository.findByAiEvalScoreConfigId(scoreConfigId)
            .map(WorkspaceAiEvalScoreConfig::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScoreConfig> getScoreConfigsByWorkspace(Long workspaceId) {
        return workspaceAiEvalScoreConfigRepository.findAllConfigsByWorkspaceId(workspaceId);
    }
}
