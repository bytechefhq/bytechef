/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiGatewayGuardrailException extends RuntimeException {

    public AiGatewayGuardrailException(String message) {
        super(message);
    }
}
