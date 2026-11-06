/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiObservabilityOtlpIngestFacade {

    OtlpIngestResult ingest(Long workspaceId, OtelSpanBatch batch);
}
