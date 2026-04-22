/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

/**
 * Result of a "Test connectivity" probe against a configured AI Gateway provider.
 *
 * @param ok           true when the provider responded successfully within the timeout
 * @param latencyMs    wall-clock round trip, or null on failure
 * @param errorMessage short human-readable failure cause, or null on success
 * @version ee
 */
public record ProviderConnectionResult(boolean ok, Long latencyMs, String errorMessage) {

    public static ProviderConnectionResult success(long latencyMs) {
        return new ProviderConnectionResult(true, latencyMs, null);
    }

    public static ProviderConnectionResult failure(String errorMessage) {
        return new ProviderConnectionResult(false, null, errorMessage);
    }
}
