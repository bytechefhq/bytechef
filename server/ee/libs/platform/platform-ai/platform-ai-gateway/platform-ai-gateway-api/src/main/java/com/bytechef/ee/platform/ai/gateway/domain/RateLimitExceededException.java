/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

/**
 * @version ee
 *
 * @author Ivica Cardic
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

    public Long getResetAtEpochMs() {
        return resetAtEpochMs;
    }
}
