/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

import com.bytechef.platform.constant.PlatformType;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 */
public record AiGuardrailsSettingsTarget(AiGuardrailsSettingsScope scope, @Nullable Long workspaceId) {

    public AiGuardrailsSettingsTarget {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }

        if ((scope == AiGuardrailsSettingsScope.WORKSPACE) != (workspaceId != null)) {
            throw new IllegalArgumentException(
                "workspaceId must be non-null exactly when scope is WORKSPACE, got scope=" + scope +
                    ", workspaceId=" + workspaceId);
        }
    }

    public static AiGuardrailsSettingsTarget workspace(long workspaceId) {
        return new AiGuardrailsSettingsTarget(AiGuardrailsSettingsScope.WORKSPACE, workspaceId);
    }

    public static AiGuardrailsSettingsTarget platform() {
        return new AiGuardrailsSettingsTarget(AiGuardrailsSettingsScope.PLATFORM, null);
    }

    public static AiGuardrailsSettingsTarget embedded() {
        return new AiGuardrailsSettingsTarget(AiGuardrailsSettingsScope.EMBEDDED, null);
    }

    public static AiGuardrailsSettingsTarget resolve(
        @Nullable PlatformType platformType, @Nullable Long workspaceId) {

        if (platformType == PlatformType.EMBEDDED) {
            return embedded();
        }

        return workspaceId == null ? platform() : workspace(workspaceId);
    }
}
