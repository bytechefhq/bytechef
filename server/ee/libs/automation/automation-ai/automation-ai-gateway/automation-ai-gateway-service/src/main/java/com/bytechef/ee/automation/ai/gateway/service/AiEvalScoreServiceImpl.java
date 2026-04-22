/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreConfig;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.repository.AiEvalScoreRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
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
class AiEvalScoreServiceImpl implements AiEvalScoreService {

    private final AiEvalScoreConfigService aiEvalScoreConfigService;
    private final AiEvalScoreRepository aiEvalScoreRepository;

    AiEvalScoreServiceImpl(
        AiEvalScoreConfigService aiEvalScoreConfigService, AiEvalScoreRepository aiEvalScoreRepository) {

        this.aiEvalScoreConfigService = aiEvalScoreConfigService;
        this.aiEvalScoreRepository = aiEvalScoreRepository;
    }

    @Override
    public AiEvalScore create(AiEvalScore score) {
        Validate.notNull(score, "score must not be null");
        Validate.isTrue(score.getId() == null, "score id must be null for creation");

        validateAgainstScoreConfig(score);

        return aiEvalScoreRepository.save(score);
    }

    @SuppressWarnings("unchecked")
    private void validateAgainstScoreConfig(AiEvalScore score) {
        Optional<AiEvalScoreConfig> scoreConfigOptional =
            aiEvalScoreConfigService.fetchScoreConfigByWorkspaceIdAndName(score.getWorkspaceId(), score.getName());

        if (scoreConfigOptional.isEmpty()) {
            return;
        }

        AiEvalScoreConfig scoreConfig = scoreConfigOptional.get();
        AiEvalScoreDataType configDataType = scoreConfig.getDataType();

        if (configDataType == null) {
            return;
        }

        switch (configDataType) {
            case NUMERIC -> {
                BigDecimal value = score.getValue();

                if (value == null) {
                    throw new IllegalArgumentException(
                        "Score '" + score.getName() + "' requires a numeric value");
                }

                BigDecimal minValue = scoreConfig.getMinValue();

                if (minValue != null && value.compareTo(minValue) < 0) {
                    throw new IllegalArgumentException(
                        "Score '" + score.getName() + "' value " + value + " is below minimum " + minValue);
                }

                BigDecimal maxValue = scoreConfig.getMaxValue();

                if (maxValue != null && value.compareTo(maxValue) > 0) {
                    throw new IllegalArgumentException(
                        "Score '" + score.getName() + "' value " + value + " is above maximum " + maxValue);
                }
            }
            case BOOLEAN -> {
                BigDecimal value = score.getValue();

                if (value == null
                    || (value.compareTo(BigDecimal.ZERO) != 0 && value.compareTo(BigDecimal.ONE) != 0)) {

                    throw new IllegalArgumentException(
                        "Score '" + score.getName() + "' must be 0.0 or 1.0 for BOOLEAN data type");
                }
            }
            case CATEGORICAL -> {
                String stringValue = score.getStringValue();
                String categoriesJson = scoreConfig.getCategories();

                if (categoriesJson == null || categoriesJson.isBlank()) {
                    return;
                }

                List<Object> categories = JsonUtils.read(categoriesJson, List.class);

                boolean matches = categories.stream()
                    .map(String::valueOf)
                    .anyMatch(category -> category.equals(stringValue));

                if (!matches) {
                    throw new IllegalArgumentException(
                        "Score '" + score.getName() + "' value '" + stringValue
                            + "' is not one of allowed categories: " + categoriesJson);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported score data type: " + configDataType);
        }
    }

    @Override
    public void delete(long id) {
        aiEvalScoreRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvalScore getScore(long id) {
        return aiEvalScoreRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiEvalScore not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScore> getScoresByTrace(Long traceId) {
        return aiEvalScoreRepository.findAllByTraceId(traceId);
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
    public List<com.bytechef.ee.automation.ai.gateway.dto.AiEvalScoreTrendPoint> getScoreTrend(
        Long workspaceId, String name, java.time.Instant start, java.time.Instant end) {

        return aiEvalScoreRepository.findTrendByWorkspaceAndName(workspaceId, name, start, end);
    }
}
