/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ProviderConnectionResult(boolean ok, Long latencyMs, String errorMessage) {

    public static ProviderConnectionResult success(long latencyMs) {
        return new ProviderConnectionResult(true, latencyMs, null);
    }

    public static ProviderConnectionResult failure(String errorMessage) {
        return new ProviderConnectionResult(false, null, errorMessage);
    }
}
