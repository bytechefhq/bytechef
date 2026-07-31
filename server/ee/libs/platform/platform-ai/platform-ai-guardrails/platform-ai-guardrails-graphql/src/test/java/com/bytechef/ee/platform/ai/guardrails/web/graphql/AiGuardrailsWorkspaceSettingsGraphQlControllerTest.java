/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class AiGuardrailsWorkspaceSettingsGraphQlControllerTest {

    private AiGuardrailsWorkspaceSettingsGraphQlController aiGuardrailsWorkspaceSettingsGraphQlController;
    private AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService;

    @BeforeEach
    void beforeEach() {
        aiGuardrailsWorkspaceSettingsService = mock(AiGuardrailsWorkspaceSettingsService.class);
        aiGuardrailsWorkspaceSettingsGraphQlController =
            new AiGuardrailsWorkspaceSettingsGraphQlController(aiGuardrailsWorkspaceSettingsService);
    }

    @Test
    void testAiGuardrailsWorkspaceSettingsQueryRequiresWorkspaceMembershipOrAdmin() throws NoSuchMethodException {
        Method method = AiGuardrailsWorkspaceSettingsGraphQlController.class.getDeclaredMethod(
            "aiGuardrailsWorkspaceSettings", Long.class);

        assertThat(method.getAnnotation(QueryMapping.class)).isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(
            "hasAuthority('ROLE_ADMIN') or (#workspaceId != null && hasPermission(#workspaceId, 'Workspace', "
                + "'AI_GATEWAY_VIEW'))");
    }

    @Test
    void testUpdateAiGuardrailsWorkspaceSettingsMutationRequiresAdmin() throws NoSuchMethodException {
        Method method = AiGuardrailsWorkspaceSettingsGraphQlController.class.getDeclaredMethod(
            "updateAiGuardrailsWorkspaceSettings",
            AiGuardrailsWorkspaceSettingsGraphQlController.AiGuardrailsWorkspaceSettingsInput.class);

        assertThat(method.getAnnotation(MutationMapping.class)).isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAuthority('ROLE_ADMIN')");
    }

    @Test
    void testAiGuardrailsWorkspaceSettingsWithNullWorkspaceIdFetchesTenantDefault() {
        AiGuardrailsWorkspaceSettings tenantDefault = new AiGuardrailsWorkspaceSettings(
            null, true, true, "secret", false, true, false, BlockingMode.REDACT_AND_CONTINUE);

        when(aiGuardrailsWorkspaceSettingsService.fetchSettings(isNull())).thenReturn(Optional.of(tenantDefault));

        AiGuardrailsWorkspaceSettings result =
            aiGuardrailsWorkspaceSettingsGraphQlController.aiGuardrailsWorkspaceSettings(null);

        assertThat(result).isEqualTo(tenantDefault);

        verify(aiGuardrailsWorkspaceSettingsService).fetchSettings(isNull());
    }

    @Test
    void testAiGuardrailsWorkspaceSettingsReturnsNullWhenNoSettingsRowExists() {
        when(aiGuardrailsWorkspaceSettingsService.fetchSettings(eq(1L))).thenReturn(Optional.empty());

        AiGuardrailsWorkspaceSettings result =
            aiGuardrailsWorkspaceSettingsGraphQlController.aiGuardrailsWorkspaceSettings(1L);

        assertThat(result).isNull();
    }

    @Test
    void testUpdateAiGuardrailsWorkspaceSettingsRoundTripsThroughService() {
        AiGuardrailsWorkspaceSettingsGraphQlController.AiGuardrailsWorkspaceSettingsInput input =
            new AiGuardrailsWorkspaceSettingsGraphQlController.AiGuardrailsWorkspaceSettingsInput(
                1L, true, false, "foo,bar", true, false, true, BlockingMode.BLOCK);

        AiGuardrailsWorkspaceSettings saved = new AiGuardrailsWorkspaceSettings(
            1L, true, false, "foo,bar", true, false, true, BlockingMode.BLOCK);

        when(aiGuardrailsWorkspaceSettingsService.saveSettings(eq(saved))).thenReturn(saved);

        AiGuardrailsWorkspaceSettings result =
            aiGuardrailsWorkspaceSettingsGraphQlController.updateAiGuardrailsWorkspaceSettings(input);

        assertThat(result).isEqualTo(saved);

        verify(aiGuardrailsWorkspaceSettingsService).saveSettings(eq(saved));
    }
}
