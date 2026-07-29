/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint;
import com.bytechef.ee.platform.ai.eval.repository.AiEvalScoreRepository;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
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
class WorkspaceAiEvalScoreServiceImpl implements WorkspaceAiEvalScoreService {

    private final AiEvalScoreRepository aiEvalScoreRepository;
    private final AiEvalScoreService aiEvalScoreService;
    private final WorkspaceAiEvalScoreConfigService workspaceAiEvalScoreConfigService;

    WorkspaceAiEvalScoreServiceImpl(
        AiEvalScoreRepository aiEvalScoreRepository, AiEvalScoreService aiEvalScoreService,
        WorkspaceAiEvalScoreConfigService workspaceAiEvalScoreConfigService) {

        this.aiEvalScoreRepository = aiEvalScoreRepository;
        this.aiEvalScoreService = aiEvalScoreService;
        this.workspaceAiEvalScoreConfigService = workspaceAiEvalScoreConfigService;
    }

    @Override
    public AiEvalScore createInWorkspace(AiEvalScore score, long workspaceId) {
        Validate.notNull(score, "score must not be null");

        // Resolve the workspace-scoped score-config (if one exists) so the platform layer can validate the score
        // value against minValue/maxValue/categories. Empty Optional → skip validation, score saves as-is.
        Optional<AiEvalScoreConfig> matchingConfig =
            workspaceAiEvalScoreConfigService.fetchScoreConfigByWorkspaceIdAndName(workspaceId, score.getName());

        score.setWorkspaceId(workspaceId);

        return aiEvalScoreService.create(score, matchingConfig);
    }

    @Override
    public void deleteInWorkspace(long scoreId) {
        aiEvalScoreService.delete(scoreId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long scoreId) {
        // findById rather than the platform service's getScore: an unknown id must still yield null (the pre-collapse
        // "no membership row" answer) because callers use this as an authorization probe, not a fetch.
        return aiEvalScoreRepository.findById(scoreId)
            .map(AiEvalScore::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScore> getScoresByWorkspace(Long workspaceId) {
        return aiEvalScoreRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScore> getScoresByWorkspaceAndName(Long workspaceId, String name) {
        return aiEvalScoreRepository.findAllByWorkspaceIdAndName(workspaceId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScoreTrendPoint> getScoreTrend(Long workspaceId, String name, Instant start, Instant end) {
        return aiEvalScoreRepository.findTrendByWorkspaceIdAndName(workspaceId, name, start, end);
    }
}
