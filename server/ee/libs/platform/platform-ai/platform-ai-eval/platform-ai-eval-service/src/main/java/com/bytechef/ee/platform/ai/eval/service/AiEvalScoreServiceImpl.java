/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreValue;
import com.bytechef.ee.platform.ai.eval.repository.AiEvalScoreRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.ArrayList;
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
class AiEvalScoreServiceImpl implements AiEvalScoreService {

    private final AiEvalScoreRepository aiEvalScoreRepository;

    AiEvalScoreServiceImpl(AiEvalScoreRepository aiEvalScoreRepository) {
        this.aiEvalScoreRepository = aiEvalScoreRepository;
    }

    @Override
    public AiEvalScore create(AiEvalScore score, Optional<AiEvalScoreConfig> matchingConfig) {
        Validate.notNull(score, "score must not be null");
        Validate.isTrue(score.getId() == null, "score id must be null for creation");
        Validate.notNull(matchingConfig, "matchingConfig must not be null (Optional.empty() means no validation)");

        matchingConfig.ifPresent(config -> validateAgainstScoreConfig(score, config));

        return aiEvalScoreRepository.save(score);
    }

    @SuppressWarnings("unchecked")
    private static void validateAgainstScoreConfig(AiEvalScore score, AiEvalScoreConfig scoreConfig) {
        AiEvalScoreDataType configDataType = scoreConfig.getDataType();

        if (configDataType == null) {
            return;
        }

        // Read via the typed view: a corrupt row whose dataType disagrees with its column shape now throws
        // IllegalStateException with row id, instead of returning a quiet null that the BOOLEAN branch (for
        // example) would silently re-classify as "not 0.0 or 1.0". Failures here are real-data faults that
        // must surface, not be papered over.
        AiEvalScoreValue typedValue = score.getTypedValue();

        switch (configDataType) {
            case NUMERIC -> {
                if (!(typedValue instanceof AiEvalScoreValue.Numeric numericValue)) {
                    throw new IllegalArgumentException(
                        "Score '" + score.getName() + "' requires a numeric value");
                }

                BigDecimal value = numericValue.value();
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
                if (!(typedValue instanceof AiEvalScoreValue.Bool)) {
                    throw new IllegalArgumentException(
                        "Score '" + score.getName() + "' must be 0.0 or 1.0 for BOOLEAN data type");
                }
            }
            case CATEGORICAL -> {
                if (!(typedValue instanceof AiEvalScoreValue.Categorical categoricalValue)) {
                    throw new IllegalArgumentException(
                        "Score '" + score.getName() + "' requires a categorical value");
                }

                String stringValue = categoricalValue.label();
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
    public List<AiEvalScore> getScores(List<Long> ids) {
        Validate.notNull(ids, "ids must not be null");

        if (ids.isEmpty()) {
            return List.of();
        }

        List<AiEvalScore> scores = new ArrayList<>();

        aiEvalScoreRepository.findAllById(ids)
            .forEach(scores::add);

        return scores;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalScore> getScoresByTrace(Long traceId) {
        return aiEvalScoreRepository.findAllByTraceId(traceId);
    }
}
