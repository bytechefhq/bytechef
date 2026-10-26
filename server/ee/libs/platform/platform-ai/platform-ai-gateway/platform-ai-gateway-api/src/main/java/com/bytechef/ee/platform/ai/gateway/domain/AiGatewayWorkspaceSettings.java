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
public record AiGatewayWorkspaceSettings(
    Long workspaceId,
    Integer retryCount,
    Integer timeoutMs,
    Boolean cacheEnabled,
    Integer cacheTtlSeconds,
    Integer logRetentionDays,
    Long defaultRoutingPolicyId,
    Integer softBudgetWarningPct) {
    public static final String PROPERTY_KEY = "ai_gateway_workspace_settings";

    public AiGatewayWorkspaceSettings {
        if (softBudgetWarningPct != null && (softBudgetWarningPct < 0 || softBudgetWarningPct > 100)) {
            throw new IllegalArgumentException(
                "softBudgetWarningPct must be between 0 and 100: " + softBudgetWarningPct);
        }
    }
}
