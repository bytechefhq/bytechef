/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import java.util.List;

/**
 * Batch body for {@code POST /scores/batch}.
 *
 * <p>
 * Two-tier sizing model: the controller enforces the operator-tunable
 * {@code bytechef.ai.gateway.external-scores.maxBatchSize} (default 1000) and returns HTTP 413 with an actionable
 * message; the record's {@link #MAX_BATCH_SIZE} is an absolute defense-in-depth ceiling so any caller that constructs
 * the DTO directly (tests, other facades, future SDK clients) cannot starve the connection pool inside
 * {@code AiExternalScoreFacadeImpl.recordBatch}, where each row opens its own transaction.
 *
 * <p>
 * The record cap is intentionally set well above the default operator cap so a runtime knob raise (for SDKs batching at
 * scale) does not collide with the record cap and surface as a generic Jackson 400 instead of the friendly 413.
 * Operators tuning the knob beyond {@link #MAX_BATCH_SIZE} should switch to a streaming endpoint.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AiExternalScoreBatchRequest(List<AiExternalScoreBatchItem> scores) {

    public static final int MAX_BATCH_SIZE = 10_000;

    public AiExternalScoreBatchRequest {
        if (scores == null) {
            throw new IllegalArgumentException("scores must not be null");
        }

        if (scores.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                "scores size (" + scores.size() + ") exceeds absolute cap (" + MAX_BATCH_SIZE
                    + "); switch to a streaming endpoint for larger batches");
        }

        scores = List.copyOf(scores);
    }
}
