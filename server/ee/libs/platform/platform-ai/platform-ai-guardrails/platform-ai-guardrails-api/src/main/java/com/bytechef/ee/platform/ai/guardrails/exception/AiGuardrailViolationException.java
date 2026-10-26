/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.exception;

/**
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
