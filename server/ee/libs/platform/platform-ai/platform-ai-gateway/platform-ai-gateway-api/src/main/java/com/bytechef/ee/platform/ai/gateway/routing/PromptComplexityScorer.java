/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;

/**
 * Produces a 0.0 (simple) to 1.0 (complex) complexity score for a chat completion request, used by the intelligent
 * routing strategies to map a request onto a model cost tier. The default implementation is deterministic and
 * model-free; this interface is the seam for a future embedding-based scorer.
 *
 * @version ee
 */
public interface PromptComplexityScorer {

    double score(AiGatewayChatCompletionRequest request);
}
