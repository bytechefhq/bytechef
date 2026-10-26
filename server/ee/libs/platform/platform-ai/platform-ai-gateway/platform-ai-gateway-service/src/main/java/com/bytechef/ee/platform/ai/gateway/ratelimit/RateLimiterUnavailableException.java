/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.ratelimit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RateLimiterUnavailableException extends RuntimeException {

    public RateLimiterUnavailableException(String message) {
        super(message);
    }

    public RateLimiterUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
