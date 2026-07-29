/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.guardrail;

/**
 * SPI for prompt-injection / jailbreak detection of AI Gateway prompts. When injection detection is enabled
 * (per-workspace or globally), the gateway guardrails ask the classifier whether a message's content is an attempt to
 * override system instructions, exfiltrate data, or otherwise hijack the model before routing it upstream; a flagged
 * message is rejected. Implementations typically call a classification model. Absent a bean, injection detection is a
 * no-op.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayInjectionClassifier {

    /**
     * Returns whether {@code content} is a prompt-injection / jailbreak attempt and should be blocked. Implementations
     * should fail open (return {@code false}) on classification errors so a transient classifier failure does not
     * hard-block all traffic.
     *
     * @param content the message content to classify
     * @return {@code true} if the content should be blocked
     */
    boolean isInjection(String content);
}
