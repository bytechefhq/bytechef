/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.repository.AiEvalScoreConfigRepository;
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

    private final AiEvalScoreConfigRepository aiEvalScoreConfigRepository;
    private final AiEvalScoreConfigService aiEvalScoreConfigService;

    WorkspaceAiEvalScoreConfigServiceImpl(
        AiEvalScoreConfigRepository aiEvalScoreConfigRepository,
        AiEvalScoreConfigService aiEvalScoreConfigService) {

        this.aiEvalScoreConfigRepository = aiEvalScoreConfigRepository;
        this.aiEvalScoreConfigService = aiEvalScoreConfigService;
    }

    @Override
    public AiEvalScoreConfig createInWorkspace(AiEvalScoreConfig scoreConfig, long workspaceId) {
        Validate.notNull(scoreConfig, "scoreConfig must not be null");

        scoreConfig.setWorkspaceId(workspaceId);

        return aiEvalScoreConfigService.create(scoreConfig);
    }

    @Override
    public void deleteInWorkspace(long scoreConfigId) {
        aiEvalScoreConfigService.delete(scoreConfigId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiEvalScoreConfig> fetchScoreConfigByWorkspaceIdAndName(Long workspaceId, String name) {
        return aiEvalScoreConfigRepository.findByWorkspaceIdAndName(workspaceId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long scoreConfigId) {
        // findById rather than the platform service's getScoreConfig: an unknown id must still yield null (the
        // pre-collapse "no membership row" answer) because callers use this as an authorization probe, not a fetch.
        return aiEvalScoreConfigRepository.findById(scoreConfigId)
            .map(AiEvalScoreConfig::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScoreConfig> getScoreConfigsByWorkspace(Long workspaceId) {
        return aiEvalScoreConfigRepository.findAllByWorkspaceId(workspaceId);
    }
}
