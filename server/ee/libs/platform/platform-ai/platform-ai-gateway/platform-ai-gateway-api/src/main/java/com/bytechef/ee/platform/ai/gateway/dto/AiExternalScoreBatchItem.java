/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * One row of the batch score request. Exactly one of {@code traceId} / {@code spanId} must be non-null.
 *
 * <p>
 * Validation parity with {@link AiExternalScoreRequest}: {@code name} / {@code dataType} / {@code value} are all
 * required and {@link ScoreValue#from(AiEvalScoreDataType, Object)} runs unconditionally at construction. Failing fast
 * at the DTO boundary keeps the batch endpoint's failure shape symmetric with the singleton endpoint — typed 400 naming
 * the offending field rather than a generic 500 deeper in the facade. {@link #scoreValue()} exposes the typed sealed
 * view for downstream dispatch; {@link #target()} returns the typed {@link ScoreTarget} sealed sum so server-side
 * dispatch is exhaustive at compile time.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiExternalScoreBatchItem(
    Long traceId,
    Long spanId,
    String name,
    Object value,
    AiEvalScoreDataType dataType,
    String comment,
    String source,
    Map<String, Object> metadata) {

    public AiExternalScoreBatchItem {
        // Delegate target validation to ScoreTarget.of so the wire-shape XOR check lives in exactly one place.
        // Both-null and both-non-null surface as IllegalArgumentException → HTTP 400 via handleBadRequest.
        ScoreTarget.of(traceId, spanId);

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (dataType == null) {
            throw new IllegalArgumentException("dataType must not be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        // Same eager canonicalisation as AiExternalScoreRequest — fail fast at the DTO boundary.
        ScoreValue.from(dataType, value);

        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }

    /**
     * Canonicalised view of {@code (dataType, value)}. See {@link AiExternalScoreRequest#scoreValue()}.
     */
    public ScoreValue scoreValue() {
        return ScoreValue.from(dataType, value);
    }

    /**
     * Typed sealed view of {@code (traceId, spanId)} for compile-time-exhaustive dispatch. Server code should dispatch
     * on this rather than null-checking the raw fields.
     */
    public ScoreTarget target() {
        return ScoreTarget.of(traceId, spanId);
    }
}
