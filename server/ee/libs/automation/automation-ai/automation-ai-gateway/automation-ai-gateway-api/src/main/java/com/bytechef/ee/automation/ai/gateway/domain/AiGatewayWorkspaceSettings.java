/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

/**
 * Workspace-scoped AI Gateway overrides. Persisted as a single
 * {@link com.bytechef.platform.configuration.domain.Property} row (scope={@code WORKSPACE}, key={@value #PROPERTY_KEY})
 * rather than a dedicated table — the platform property store already handles scope/audit/versioning/encryption and
 * this is plain config data. All fields are nullable; a null value means "inherit from the system default".
 *
 * @version ee
 */
public record AiGatewayWorkspaceSettings(
    Long workspaceId,
    Integer retryCount,
    Integer timeoutMs,
    Boolean cacheEnabled,
    Integer cacheTtlSeconds,
    Integer logRetentionDays,
    Long defaultRoutingPolicyId,
    Integer softBudgetWarningPct,
    Boolean redactPii) {

    public static final String PROPERTY_KEY = "ai_gateway_workspace_settings";

    public AiGatewayWorkspaceSettings {
        if (softBudgetWarningPct != null && (softBudgetWarningPct < 0 || softBudgetWarningPct > 100)) {
            throw new IllegalArgumentException(
                "softBudgetWarningPct must be between 0 and 100: " + softBudgetWarningPct);
        }
    }
}
