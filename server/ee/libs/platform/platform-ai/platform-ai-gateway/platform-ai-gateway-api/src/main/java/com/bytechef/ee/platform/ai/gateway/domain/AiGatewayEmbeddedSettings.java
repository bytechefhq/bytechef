/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import java.math.BigDecimal;

/**
 * @version ee
 */
public record AiGatewayEmbeddedSettings(
    Long environmentId,
    Integer retryCount,
    Integer timeoutMs,
    Boolean cacheEnabled,
    Integer cacheTtlSeconds,
    Integer logRetentionDays,
    Long defaultRoutingPolicyId,
    Integer softBudgetWarningPct,
    BigDecimal defaultConnectedUserBudgetCap) {

    public static final String PROPERTY_KEY = "ai_gateway_embedded_settings";

    public AiGatewayEmbeddedSettings {
        if (softBudgetWarningPct != null && (softBudgetWarningPct < 0 || softBudgetWarningPct > 100)) {
            throw new IllegalArgumentException(
                "softBudgetWarningPct must be between 0 and 100: " + softBudgetWarningPct);
        }

        if (defaultConnectedUserBudgetCap != null && defaultConnectedUserBudgetCap.signum() <= 0) {
            throw new IllegalArgumentException(
                "defaultConnectedUserBudgetCap must be positive: " + defaultConnectedUserBudgetCap);
        }
    }
}
