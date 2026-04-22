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
 * Request body for {@code POST /traces/{traceId}/scores} and {@code POST /spans/{spanId}/scores}.
 *
 * <p>
 * {@code value} is kept loose ({@link Object}) so Jackson can deserialize the wire shape {@code {"value": 0.87,
 * "dataType": "NUMERIC"}} without a discriminator. Runtime conformance is enforced at construction by
 * {@link ScoreValue#from(AiEvalScoreDataType, Object)}; mismatched (value, dataType) pairs surface as HTTP 400 via
 * {@link IllegalArgumentException}. The canonicalised value is exposed via {@link #scoreValue()} so the facade
 * dispatches on a sealed type with exhaustive switch instead of re-running runtime type checks.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiExternalScoreRequest(
    String name,
    Object value,
    AiEvalScoreDataType dataType,
    String comment,
    String source,
    Map<String, Object> metadata) {

    public AiExternalScoreRequest {
        // Explicit IllegalArgumentException (not commons-lang3 Validate.notNull, which throws NPE) so an empty
        // request body / {"value": null} surfaces as HTTP 400 via handleBadRequest, not a generic 500.
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (dataType == null) {
            throw new IllegalArgumentException("dataType must not be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        // Run the canonicalisation eagerly at construction so shape mismatches reject at the DTO boundary
        // (HTTP 400) rather than deeper in the facade. The result is recomputed by scoreValue() — cheap
        // because the rules are pure — and the eager call here is the one that fails fast.
        ScoreValue.from(dataType, value);

        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }

    /**
     * Canonicalised, type-safe view of {@code (dataType, value)}. The facade switches on the sealed return type so a
     * future {@link AiEvalScoreDataType} variant added without a corresponding {@link ScoreValue} permit fails at
     * compile time rather than at runtime.
     */
    public ScoreValue scoreValue() {
        return ScoreValue.from(dataType, value);
    }
}
