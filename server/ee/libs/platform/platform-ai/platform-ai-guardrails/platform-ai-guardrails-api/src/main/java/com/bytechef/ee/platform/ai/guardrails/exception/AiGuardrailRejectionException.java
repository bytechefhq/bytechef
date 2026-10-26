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
public final class AiGuardrailRejectionException extends RuntimeException {

    public AiGuardrailRejectionException(String message) {
        super(message);
    }
}
