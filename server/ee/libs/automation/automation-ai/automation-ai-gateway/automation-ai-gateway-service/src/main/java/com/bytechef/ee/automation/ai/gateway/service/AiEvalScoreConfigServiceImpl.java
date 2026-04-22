/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreConfig;
import com.bytechef.ee.automation.ai.gateway.repository.AiEvalScoreConfigRepository;
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
class AiEvalScoreConfigServiceImpl implements AiEvalScoreConfigService {

    private final AiEvalScoreConfigRepository aiEvalScoreConfigRepository;

    AiEvalScoreConfigServiceImpl(AiEvalScoreConfigRepository aiEvalScoreConfigRepository) {
        this.aiEvalScoreConfigRepository = aiEvalScoreConfigRepository;
    }

    @Override
    public AiEvalScoreConfig create(AiEvalScoreConfig scoreConfig) {
        Validate.notNull(scoreConfig, "scoreConfig must not be null");
        Validate.isTrue(scoreConfig.getId() == null, "scoreConfig id must be null for creation");

        return aiEvalScoreConfigRepository.save(scoreConfig);
    }

    @Override
    public void delete(long id) {
        aiEvalScoreConfigRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiEvalScoreConfig> fetchScoreConfigByWorkspaceIdAndName(Long workspaceId, String name) {
        return aiEvalScoreConfigRepository.findByWorkspaceIdAndName(workspaceId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvalScoreConfig getScoreConfig(long id) {
        return aiEvalScoreConfigRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("AiEvalScoreConfig not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScoreConfig> getScoreConfigsByWorkspace(Long workspaceId) {
        return aiEvalScoreConfigRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public AiEvalScoreConfig update(AiEvalScoreConfig scoreConfig) {
        Validate.notNull(scoreConfig, "scoreConfig must not be null");
        Validate.notNull(scoreConfig.getId(), "scoreConfig id must not be null for update");

        return aiEvalScoreConfigRepository.save(scoreConfig);
    }
}
