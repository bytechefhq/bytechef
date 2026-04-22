/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

/**
 * Thrown by rate-limit enforcement paths in the AI gateway. Carries the optional reset-at epoch (millis since Unix
 * epoch) so the REST exception handler can derive a {@code Retry-After} header — without this, a client would have to
 * parse the message string to learn when to retry, which is fragile and locale-dependent.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class RateLimitExceededException extends RuntimeException {

    private final Long resetAtEpochMs;

    public RateLimitExceededException(String message) {
        this(message, null);
    }

    public RateLimitExceededException(String message, Long resetAtEpochMs) {
        super(message);
        this.resetAtEpochMs = resetAtEpochMs;
    }

    /**
     * Epoch millis at which the limiter expects the relevant window to reset. May be {@code null} when the throw site
     * does not have visibility into the underlying limiter's window state.
     */
    public Long getResetAtEpochMs() {
        return resetAtEpochMs;
    }
}
