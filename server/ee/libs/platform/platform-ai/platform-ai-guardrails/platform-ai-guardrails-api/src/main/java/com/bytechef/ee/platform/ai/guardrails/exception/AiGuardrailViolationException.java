/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.exception;

/**
 * Thrown by {@code AiGuardrailsAdvisor} when a request-direction guardrail check reports a blocking violation (a
 * blocked-term match or a flagged prompt injection) and the workspace's {@code BlockingMode} is {@code BLOCK}. The
 * message carries only the violation's {@link #getCategory() category} — never the offending content — so it is safe to
 * surface to a caller or log at any level.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiGuardrailViolationException extends RuntimeException {

    private final String category;

    public AiGuardrailViolationException(String category) {
        super("Blocked by AI guardrail: " + category);

        this.category = category;
    }

    public String getCategory() {
        return category;
    }
}
