/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import java.util.List;

/**
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
