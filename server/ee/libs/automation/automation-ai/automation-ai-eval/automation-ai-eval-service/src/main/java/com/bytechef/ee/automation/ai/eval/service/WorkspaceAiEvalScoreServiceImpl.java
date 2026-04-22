/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.automation.ai.eval.domain.WorkspaceAiEvalScore;
import com.bytechef.ee.automation.ai.eval.repository.WorkspaceAiEvalScoreRepository;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint;
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

    private final AiEvalScoreService aiEvalScoreService;
    private final WorkspaceAiEvalScoreConfigService workspaceAiEvalScoreConfigService;
    private final WorkspaceAiEvalScoreRepository workspaceAiEvalScoreRepository;

    WorkspaceAiEvalScoreServiceImpl(
        AiEvalScoreService aiEvalScoreService,
        WorkspaceAiEvalScoreConfigService workspaceAiEvalScoreConfigService,
        WorkspaceAiEvalScoreRepository workspaceAiEvalScoreRepository) {

        this.aiEvalScoreService = aiEvalScoreService;
        this.workspaceAiEvalScoreConfigService = workspaceAiEvalScoreConfigService;
        this.workspaceAiEvalScoreRepository = workspaceAiEvalScoreRepository;
    }

    @Override
    public AiEvalScore createInWorkspace(AiEvalScore score, long workspaceId) {
        Validate.notNull(score, "score must not be null");

        // Resolve the workspace-scoped score-config (if one exists) so the platform layer can validate the score
        // value against minValue/maxValue/categories. Empty Optional → skip validation, score saves as-is.
        Optional<AiEvalScoreConfig> matchingConfig =
            workspaceAiEvalScoreConfigService.fetchScoreConfigByWorkspaceIdAndName(workspaceId, score.getName());

        AiEvalScore saved = aiEvalScoreService.create(score, matchingConfig);

        workspaceAiEvalScoreRepository.save(new WorkspaceAiEvalScore(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    public void deleteInWorkspace(long scoreId) {
        workspaceAiEvalScoreRepository.findByAiEvalScoreId(scoreId)
            .ifPresent(membership -> workspaceAiEvalScoreRepository.deleteById(membership.getId()));

        aiEvalScoreService.delete(scoreId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long scoreId) {
        return workspaceAiEvalScoreRepository.findByAiEvalScoreId(scoreId)
            .map(WorkspaceAiEvalScore::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScore> getScoresByWorkspace(Long workspaceId) {
        return workspaceAiEvalScoreRepository.findAllScoresByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScore> getScoresByWorkspaceAndName(Long workspaceId, String name) {
        return workspaceAiEvalScoreRepository.findAllScoresByWorkspaceIdAndName(workspaceId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScoreTrendPoint> getScoreTrend(Long workspaceId, String name, Instant start, Instant end) {
        return workspaceAiEvalScoreRepository.findTrendByWorkspaceAndName(workspaceId, name, start, end);
    }
}
