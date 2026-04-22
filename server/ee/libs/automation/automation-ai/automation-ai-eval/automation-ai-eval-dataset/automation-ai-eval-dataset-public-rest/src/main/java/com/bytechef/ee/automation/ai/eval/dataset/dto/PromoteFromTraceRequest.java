/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * Request body for {@code POST /api/ai-gateway/v1/datasets/{id}/items/from-trace}. Promotes an existing observability
 * trace into a dataset item; provenance is preserved via the item's {@code sourceTraceId}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record PromoteFromTraceRequest(
    Long traceId,
    Object expectedOutput,
    Map<String, Object> metadata) {

    public PromoteFromTraceRequest {
        Validate.notNull(traceId, "traceId must not be null");

        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }
}
