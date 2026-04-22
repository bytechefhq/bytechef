/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;

/**
 * Converts a batch of OTel GenAI spans (produced by the sibling OTLP module's mapper) into
 * {@code ai_observability_trace} and {@code ai_observability_span} rows. Owns the gateway-side concerns: workspace
 * scoping, cost computation, and trace de-duplication.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiObservabilityOtlpIngestFacade {

    OtlpIngestResult ingest(Long workspaceId, OtelSpanBatch batch);
}
