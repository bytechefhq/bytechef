/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.constant.PlatformType;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiGuardrailsSettingsTargetTest {

    @Test
    void testWorkspaceCarriesItsId() {
        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.workspace(7L);

        assertThat(target.scope()).isEqualTo(AiGuardrailsSettingsScope.WORKSPACE);
        assertThat(target.workspaceId()).isEqualTo(7L);
    }

    @Test
    void testPlatformAndEmbeddedCarryNoWorkspace() {
        assertThat(AiGuardrailsSettingsTarget.platform()
            .workspaceId()).isNull();
        assertThat(AiGuardrailsSettingsTarget.embedded()
            .workspaceId()).isNull();
    }

    @Test
    void testAWorkspaceScopeWithoutAnIdIsRefused() {
        assertThatThrownBy(() -> new AiGuardrailsSettingsTarget(AiGuardrailsSettingsScope.WORKSPACE, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testANonWorkspaceScopeWithAnIdIsRefused() {
        assertThatThrownBy(() -> new AiGuardrailsSettingsTarget(AiGuardrailsSettingsScope.EMBEDDED, 7L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiGuardrailsSettingsTarget(AiGuardrailsSettingsScope.PLATFORM, 7L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testANullScopeIsRefused() {
        assertThatThrownBy(() -> new AiGuardrailsSettingsTarget(null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testResolveMapsAnEmbeddedRunToTheEmbeddedScope() {
        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(PlatformType.EMBEDDED, null);

        assertThat(target.scope()).isEqualTo(AiGuardrailsSettingsScope.EMBEDDED);
    }

    @Test
    void testResolveIgnoresAWorkspaceIdOnAnEmbeddedRun() {
        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(PlatformType.EMBEDDED, 7L);

        assertThat(target.scope()).isEqualTo(AiGuardrailsSettingsScope.EMBEDDED);
        assertThat(target.workspaceId()).isNull();
    }

    @Test
    void testResolveMapsAResolvedWorkspaceToTheWorkspaceScope() {
        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(PlatformType.AUTOMATION, 7L);

        assertThat(target.scope()).isEqualTo(AiGuardrailsSettingsScope.WORKSPACE);
        assertThat(target.workspaceId()).isEqualTo(7L);
    }

    @Test
    void testResolveMapsAnUnresolvedWorkspaceToThePlatformScope() {
        assertThat(AiGuardrailsSettingsTarget.resolve(PlatformType.AUTOMATION, null)
            .scope())
                .isEqualTo(AiGuardrailsSettingsScope.PLATFORM);
        assertThat(AiGuardrailsSettingsTarget.resolve(null, null)
            .scope())
                .isEqualTo(AiGuardrailsSettingsScope.PLATFORM);
    }
}
