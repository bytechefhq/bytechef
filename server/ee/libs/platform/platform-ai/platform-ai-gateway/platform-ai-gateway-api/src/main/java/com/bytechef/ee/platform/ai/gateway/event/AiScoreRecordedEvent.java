/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.event;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Fired after a score write commits. Consumers include: audit log subscribers, metrics aggregators. Uses Spring's
 * {@code ApplicationEventPublisher} pattern already established in the AI gateway via
 * {@code AiGatewayTraceCompletedEvent} and {@code AiGatewayBudgetExceededEvent}.
 *
 * <p>
 * {@code spanId} and {@code sourceIdentifier} are nullable; everything else is required so subscribers can trust the
 * event payload without nullability dancing.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AiScoreRecordedEvent(
    Long scoreId,
    Long workspaceId,
    Long traceId,
    @Nullable Long spanId,
    String name,
    AiEvalScoreDataType dataType,
    AiEvalScoreSource source,
    @Nullable String sourceIdentifier,
    Instant occurredAt) {

    public AiScoreRecordedEvent {
        Validate.notNull(scoreId, "scoreId must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(traceId, "traceId must not be null");
        Validate.isTrue(StringUtils.isNotBlank(name), "name must not be blank");
        Validate.notNull(dataType, "dataType must not be null");
        Validate.notNull(source, "source must not be null");
        Validate.notNull(occurredAt, "occurredAt must not be null");
    }
}
