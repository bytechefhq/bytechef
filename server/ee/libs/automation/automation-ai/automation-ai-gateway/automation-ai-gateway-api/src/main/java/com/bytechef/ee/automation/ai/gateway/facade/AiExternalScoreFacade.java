/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreResult;

/**
 * Ingests scores from external evaluators (Ragas, DeepEval, TruLens, custom pipelines). Every target trace/span must
 * belong to the caller's workspace — a mismatch raises
 * {@link com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException} (mapped to HTTP 403 by the
 * gateway's exception handler).
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiExternalScoreFacade {

    AiExternalScoreResult recordTraceScore(Long workspaceId, Long traceId, AiExternalScoreRequest request);

    AiExternalScoreResult recordSpanScore(Long workspaceId, Long spanId, AiExternalScoreRequest request);

    AiExternalScoreBatchResult recordBatch(Long workspaceId, AiExternalScoreBatchRequest request);
}
