/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

/**
 * Thrown when an AI Gateway request violates a configured content guardrail — a blocked term, a moderation flag, or a
 * detected prompt injection. Mapped to HTTP 422 (Unprocessable Entity) by the gateway's exception handler. The message
 * names neither the offending prompt content nor the matched term's location, so it is safe to surface on the wire.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiGatewayGuardrailException extends RuntimeException {

    public AiGatewayGuardrailException(String message) {
        super(message);
    }
}
