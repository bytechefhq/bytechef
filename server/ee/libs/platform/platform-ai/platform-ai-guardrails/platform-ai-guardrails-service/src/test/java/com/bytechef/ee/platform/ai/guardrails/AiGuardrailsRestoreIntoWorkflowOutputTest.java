/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiGuardrailsRestoreIntoWorkflowOutputTest {

    @Test
    void testDoesNotRestoreWhenTheSettingIsUnset() {
        AiGuardrails aiGuardrails = aiGuardrailsWith(null);

        assertThat(aiGuardrails.isRestoreIntoWorkflowOutput(AiGuardrailsSettingsTarget.workspace(1L))).isFalse();
    }

    @Test
    void testRestoresWhenTheSettingIsTrue() {
        AiGuardrails aiGuardrails = aiGuardrailsWith(true);

        assertThat(aiGuardrails.isRestoreIntoWorkflowOutput(AiGuardrailsSettingsTarget.workspace(1L))).isTrue();
    }

    @Test
    void testDoesNotRestoreWhenTheSettingIsFalse() {
        AiGuardrails aiGuardrails = aiGuardrailsWith(false);

        assertThat(aiGuardrails.isRestoreIntoWorkflowOutput(AiGuardrailsSettingsTarget.workspace(1L))).isFalse();
    }

    @Test
    void testDoesNotRestoreWhenNoSettingsRowExists() {
        AiGuardrails aiGuardrails = aiGuardrailsWithNoRow();

        assertThat(aiGuardrails.isRestoreIntoWorkflowOutput(AiGuardrailsSettingsTarget.workspace(1L))).isFalse();
    }

    @Test
    void testDoesNotRestoreWhenTheSettingsLookupThrows() {
        AiGuardrails aiGuardrails = aiGuardrailsWithFailingLookup();

        assertThat(aiGuardrails.isRestoreIntoWorkflowOutput(AiGuardrailsSettingsTarget.workspace(1L))).isFalse();
    }

    private AiGuardrails aiGuardrailsWith(Boolean restoreIntoWorkflowOutput) {
        AiGuardrailsWorkspaceSettingsService settingsService = mock(AiGuardrailsWorkspaceSettingsService.class);

        when(settingsService.fetchSettings(1L)).thenReturn(Optional.of(new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, 1L, null, null, null, null, null, null, null, null, null,
            restoreIntoWorkflowOutput)));

        return new AiGuardrails(
            settingsService, null, null, null, false, false, "", false, false, false, false);
    }

    private AiGuardrails aiGuardrailsWithNoRow() {
        AiGuardrailsWorkspaceSettingsService settingsService = mock(AiGuardrailsWorkspaceSettingsService.class);

        when(settingsService.fetchSettings(1L)).thenReturn(Optional.empty());

        return new AiGuardrails(
            settingsService, null, null, null, false, false, "", false, false, false, false);
    }

    private AiGuardrails aiGuardrailsWithFailingLookup() {
        AiGuardrailsWorkspaceSettingsService settingsService = mock(AiGuardrailsWorkspaceSettingsService.class);

        doThrow(new RuntimeException("settings lookup failed")).when(settingsService)
            .fetchSettings(1L);

        return new AiGuardrails(
            settingsService, null, null, null, false, false, "", false, false, false, false);
    }
}
