/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface LlmUsageRecorder {

    void recordLlm(LlmUsageContext context, String model, int inputTokens, int outputTokens, long durationMs);
}
