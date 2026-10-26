/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

/**
 * @version ee
 */
public record AiGuardrailsWorkspaceSettings(
    AiGuardrailsSettingsScope scope,
    Long workspaceId,
    Boolean redactPii,
    Boolean redactSecrets,
    String blockedTerms,
    Boolean moderationEnabled,
    Boolean injectionDetectionEnabled,
    Boolean scanResponses,
    BlockingMode blockingMode,
    Double minConfidence,
    Boolean redactMcpResults,
    Boolean restoreIntoWorkflowOutput) {

    public static final String PROPERTY_KEY = "ai_guardrails_workspace_settings";

    public AiGuardrailsWorkspaceSettings {
        if (minConfidence != null && (!Double.isFinite(minConfidence) || minConfidence < 0.0 || minConfidence > 1.0)) {
            throw new IllegalArgumentException("minConfidence must be null or between 0.0 and 1.0, got: " +
                minConfidence);
        }

        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }

        if ((scope == AiGuardrailsSettingsScope.WORKSPACE) != (workspaceId != null)) {
            throw new IllegalArgumentException(
                "workspaceId must be non-null exactly when scope is WORKSPACE, got scope=" + scope +
                    ", workspaceId=" + workspaceId);
        }
    }

    /**
     * @version ee
     */
    public enum BlockingMode {

        BLOCK,
        REDACT_AND_CONTINUE,

        ALLOW
    }
}
