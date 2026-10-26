/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface PromptComplexityScorer {

    double score(AiGatewayChatCompletionRequest request);
}
