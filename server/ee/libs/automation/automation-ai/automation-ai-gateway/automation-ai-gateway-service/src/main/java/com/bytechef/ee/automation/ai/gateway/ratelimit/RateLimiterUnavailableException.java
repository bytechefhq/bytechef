/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

/**
 * Thrown when the rate limiter backend (e.g. Redis) is unavailable or returns an invalid response. Callers must decide
 * whether to fail-closed (reject the request) or fail-open (allow) — this exception prevents the rate limiter from
 * silently allowing every request when its backend is down.
 *
 * @version ee
 */
public class RateLimiterUnavailableException extends RuntimeException {

    public RateLimiterUnavailableException(String message) {
        super(message);
    }

    public RateLimiterUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
