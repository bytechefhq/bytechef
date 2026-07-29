/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.guardrail;

/**
 * SPI for model-based content moderation of AI Gateway prompts. When moderation is enabled (per-workspace or globally),
 * the gateway guardrails ask the classifier whether a message's content is unsafe before routing it upstream; a flagged
 * message is rejected. Implementations typically call a moderation/classification model. Absent a bean, moderation is a
 * no-op.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayModerationClassifier {

    /**
     * Returns whether {@code content} is unsafe and should be blocked. Implementations should fail open (return
     * {@code false}) on classification errors so a transient moderation-model failure does not hard-block all traffic.
     *
     * @param content the message content to classify
     * @return {@code true} if the content should be blocked
     */
    boolean isFlagged(String content);
}
