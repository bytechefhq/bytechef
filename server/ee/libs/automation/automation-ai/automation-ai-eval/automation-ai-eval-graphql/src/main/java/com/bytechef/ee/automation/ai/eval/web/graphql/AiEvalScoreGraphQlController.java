/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.eval.facade.AiEvalScoreFacade;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreValue;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * Authorization is enforced on {@link AiEvalScoreFacade}, not here.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalScoreGraphQlController {

    private final AiEvalScoreFacade aiEvalScoreFacade;

    @SuppressFBWarnings("EI")
    AiEvalScoreGraphQlController(AiEvalScoreFacade aiEvalScoreFacade) {
        this.aiEvalScoreFacade = aiEvalScoreFacade;
    }

    @QueryMapping
    public List<AiEvalScore> aiEvalScores(@Argument Long workspaceId) {
        return aiEvalScoreFacade.getScoresByWorkspace(workspaceId);
    }

    @QueryMapping
    public List<AiEvalScore> aiEvalScoresByTrace(@Argument Long traceId) {
        return aiEvalScoreFacade.getScoresByTrace(traceId);
    }

    @QueryMapping
    public List<com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint> aiEvalScoreTrend(
        @Argument Long workspaceId, @Argument String name,
        @Argument Long startDate, @Argument Long endDate) {

        return aiEvalScoreFacade.getScoreTrend(
            workspaceId, name, Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));
    }

    @QueryMapping
    public List<AiEvalScoreAnalytics> aiEvalScoreAnalytics(
        @Argument Long workspaceId, @Argument Long startDate, @Argument Long endDate) {

        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        List<AiEvalScore> scores = aiEvalScoreFacade.getScoresByWorkspaceForAnalytics(workspaceId);

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
                    // Read via the typed view: a corrupt NUMERIC row throws IllegalStateException with row id,
                    // surfacing the bad row immediately instead of silently dropping it from the average.
                    if (!(score.getTypedValue() instanceof AiEvalScoreValue.Numeric numeric)) {
                        continue;
                    }

                    double doubleValue = numeric.value()
                        .doubleValue();

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
                    AiEvalScoreValue typedValue = score.getTypedValue();

                    String key;

                    if (typedValue instanceof AiEvalScoreValue.Bool bool) {
                        key = bool.asString();
                    } else if (typedValue instanceof AiEvalScoreValue.Categorical categorical) {
                        key = categorical.label();
                    } else {
                        // Defensive: a NUMERIC row landing here means dataType+rows agreed on a NUMERIC group
                        // but the row's typed view is Numeric; bucket as the stringified BigDecimal so the
                        // distribution doesn't silently drop it.
                        key = typedValue instanceof AiEvalScoreValue.Numeric numeric
                            ? numeric.value()
                                .toPlainString()
                            : "unknown";
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
        Long traceId, String name, AiEvalScoreDataType dataType,
        AiEvalScoreSource source, Double value, String stringValue) {

        return switch (dataType) {
            case NUMERIC -> {
                if (value == null) {
                    throw new IllegalArgumentException("NUMERIC score '" + name + "' requires a value");
                }

                yield AiEvalScore.numeric(traceId, name, source, BigDecimal.valueOf(value));
            }
            case BOOLEAN -> {
                if (value == null) {
                    throw new IllegalArgumentException(
                        "BOOLEAN score '" + name + "' requires a value (0 or 1)");
                }

                yield AiEvalScore.bool(traceId, name, source, value != 0.0);
            }
            case CATEGORICAL -> {
                if (stringValue == null || stringValue.isBlank()) {
                    throw new IllegalArgumentException(
                        "CATEGORICAL score '" + name + "' requires a non-blank stringValue");
                }

                yield AiEvalScore.categorical(traceId, name, source, stringValue);
            }
        };
    }

    @MutationMapping
    public AiEvalScore createAiEvalScore(
        @Argument Long workspaceId, @Argument Long traceId, @Argument Long spanId,
        @Argument String name, @Argument Double value, @Argument String stringValue,
        @Argument AiEvalScoreDataType dataType, @Argument AiEvalScoreSource source,
        @Argument String comment) {

        AiEvalScore score = buildScore(traceId, name, dataType, source, value, stringValue);

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

        return aiEvalScoreFacade.createInWorkspace(score, workspaceId);
    }

    @MutationMapping
    public boolean deleteAiEvalScore(@Argument long id) {
        aiEvalScoreFacade.deleteInWorkspace(id);

        return true;
    }
}
