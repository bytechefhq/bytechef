/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreSource;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalScoreService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalScoreGraphQlController {

    private final AiEvalScoreService aiEvalScoreService;

    @SuppressFBWarnings("EI")
    AiEvalScoreGraphQlController(AiEvalScoreService aiEvalScoreService) {
        this.aiEvalScoreService = aiEvalScoreService;
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiEvalScore> aiEvalScores(@Argument Long workspaceId) {
        return aiEvalScoreService.getScoresByWorkspace(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalScore> aiEvalScoresByTrace(@Argument Long traceId) {
        return aiEvalScoreService.getScoresByTrace(traceId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<com.bytechef.ee.automation.ai.gateway.dto.AiEvalScoreTrendPoint> aiEvalScoreTrend(
        @Argument Long workspaceId, @Argument String name,
        @Argument Long startDate, @Argument Long endDate) {

        return aiEvalScoreService.getScoreTrend(
            workspaceId, name, Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalScoreAnalytics> aiEvalScoreAnalytics(
        @Argument Long workspaceId, @Argument Long startDate, @Argument Long endDate) {

        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        List<AiEvalScore> scores = aiEvalScoreService.getScoresByWorkspace(workspaceId);

        Map<String, List<AiEvalScore>> scoresByName = new LinkedHashMap<>();

        for (AiEvalScore score : scores) {
            Instant createdDate = score.getCreatedDate();

            if (createdDate == null || createdDate.isBefore(start) || createdDate.isAfter(end)) {
                continue;
            }

            scoresByName.computeIfAbsent(score.getName(), name -> new ArrayList<>())
                .add(score);
        }

        List<AiEvalScoreAnalytics> analytics = new ArrayList<>();

        for (Map.Entry<String, List<AiEvalScore>> entry : scoresByName.entrySet()) {
            String name = entry.getKey();
            List<AiEvalScore> group = entry.getValue();

            AiEvalScoreDataType dataType = group.getFirst()
                .getDataType();

            int count = group.size();
            Double average = null;
            Double min = null;
            Double max = null;
            List<AiEvalScoreDistributionEntry> distribution = new ArrayList<>();

            if (dataType == AiEvalScoreDataType.NUMERIC) {
                double sum = 0.0;
                double minValue = Double.MAX_VALUE;
                double maxValue = -Double.MAX_VALUE;
                int numericCount = 0;

                for (AiEvalScore score : group) {
                    BigDecimal value = score.getValue();

                    if (value == null) {
                        continue;
                    }

                    double doubleValue = value.doubleValue();

                    sum += doubleValue;

                    if (doubleValue < minValue) {
                        minValue = doubleValue;
                    }

                    if (doubleValue > maxValue) {
                        maxValue = doubleValue;
                    }

                    numericCount++;
                }

                if (numericCount > 0) {
                    average = sum / numericCount;
                    min = minValue;
                    max = maxValue;
                }
            } else {
                Map<String, Integer> counts = new LinkedHashMap<>();

                for (AiEvalScore score : group) {
                    String key;

                    if (dataType == AiEvalScoreDataType.BOOLEAN) {
                        BigDecimal value = score.getValue();

                        if (value != null) {
                            key = value.signum() == 0 ? "false" : "true";
                        } else if (score.getStringValue() != null) {
                            key = score.getStringValue();
                        } else {
                            key = "unknown";
                        }
                    } else {
                        key = score.getStringValue() != null ? score.getStringValue() : "unknown";
                    }

                    counts.merge(key, 1, Integer::sum);
                }

                for (Map.Entry<String, Integer> countEntry : counts.entrySet()) {
                    distribution.add(new AiEvalScoreDistributionEntry(countEntry.getKey(), countEntry.getValue()));
                }
            }

            analytics.add(new AiEvalScoreAnalytics(name, dataType, count, average, min, max, distribution));
        }

        return analytics;
    }

    @SuppressFBWarnings("EI")
    public record AiEvalScoreAnalytics(
        String name, AiEvalScoreDataType dataType, int count, Double average, Double min, Double max,
        List<AiEvalScoreDistributionEntry> distribution) {
    }

    public record AiEvalScoreDistributionEntry(String value, int count) {
    }

    /**
     * Routes through the typed factories so the flat {@code (value, stringValue)} pair can't drift from
     * {@code dataType}. NUMERIC and BOOLEAN require {@code value}; CATEGORICAL requires {@code stringValue}.
     */
    private static AiEvalScore buildScore(
        Long workspaceId, Long traceId, String name, AiEvalScoreDataType dataType,
        AiEvalScoreSource source, Double value, String stringValue) {

        return switch (dataType) {
            case NUMERIC -> {
                if (value == null) {
                    throw new IllegalArgumentException("NUMERIC score '" + name + "' requires a value");
                }

                yield AiEvalScore.numeric(workspaceId, traceId, name, source, BigDecimal.valueOf(value));
            }
            case BOOLEAN -> {
                if (value == null) {
                    throw new IllegalArgumentException(
                        "BOOLEAN score '" + name + "' requires a value (0 or 1)");
                }

                yield AiEvalScore.bool(workspaceId, traceId, name, source, value != 0.0);
            }
            case CATEGORICAL -> {
                if (stringValue == null || stringValue.isBlank()) {
                    throw new IllegalArgumentException(
                        "CATEGORICAL score '" + name + "' requires a non-blank stringValue");
                }

                yield AiEvalScore.categorical(workspaceId, traceId, name, source, stringValue);
            }
        };
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalScore createAiEvalScore(
        @Argument Long workspaceId, @Argument Long traceId, @Argument Long spanId,
        @Argument String name, @Argument Double value, @Argument String stringValue,
        @Argument AiEvalScoreDataType dataType, @Argument AiEvalScoreSource source,
        @Argument String comment) {

        AiEvalScore score = buildScore(workspaceId, traceId, name, dataType, source, value, stringValue);

        if (spanId != null) {
            score.setSpanId(spanId);
        }

        if (comment != null) {
            score.setComment(comment);
        }

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication != null) {
            score.setCreatedBy(authentication.getName());
        }

        return aiEvalScoreService.create(score);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiEvalScore(@Argument long id) {
        aiEvalScoreService.delete(id);

        return true;
    }
}
