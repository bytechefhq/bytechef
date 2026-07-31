/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGuardrailsWorkspaceSettingsServiceTest {

    @Mock
    private PropertyService propertyService;

    private AiGuardrailsWorkspaceSettingsService service;

    @BeforeEach
    void beforeEach() {
        service = new AiGuardrailsWorkspaceSettingsServiceImpl(propertyService);
    }

    @Test
    void testFetchSettingsReadsWorkspaceScopedProperty() {
        when(propertyService.fetchProperty(
            AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.WORKSPACE, 7L))
                .thenReturn(Optional.of(property(Map.of("redactPii", true, "blockingMode", "REDACT_AND_CONTINUE"))));

        AiGuardrailsWorkspaceSettings settings = service.fetchSettings(7L)
            .orElseThrow();

        assertThat(settings.workspaceId()).isEqualTo(7L);
        assertThat(settings.redactPii()).isTrue();
        assertThat(settings.blockingMode()).isEqualTo(BlockingMode.REDACT_AND_CONTINUE);
    }

    @Test
    void testFetchSettingsWithNullWorkspaceReadsTenantDefault() {
        // null workspaceId = tenant default row; the service stores it under Scope.PLATFORM with a null scopeId
        // (see AiGuardrailsWorkspaceSettingsServiceImpl's class javadoc for the rationale).
        when(propertyService.fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.PLATFORM, null))
            .thenReturn(Optional.of(property(Map.of("redactSecrets", true))));

        AiGuardrailsWorkspaceSettings settings = service.fetchSettings(null)
            .orElseThrow();

        assertThat(settings.workspaceId()).isNull();
        assertThat(settings.redactSecrets()).isTrue();
    }

    @Test
    void testBlockingModeDefaultsToBlockWhenAbsent() {
        when(propertyService.fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.WORKSPACE, 7L))
            .thenReturn(Optional.of(property(Map.of("redactPii", true))));

        AiGuardrailsWorkspaceSettings settings = service.fetchSettings(7L)
            .orElseThrow();

        assertThat(settings.blockingMode()).isEqualTo(BlockingMode.BLOCK);
    }

    @Test
    void testSaveSettingsWritesWorkspaceScopedProperty() {
        AiGuardrailsWorkspaceSettings settings = new AiGuardrailsWorkspaceSettings(
            7L, true, null, "foo,bar", null, null, null, BlockingMode.REDACT_AND_CONTINUE);

        service.saveSettings(settings);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY),
            eq(Map.of("redactPii", true, "blockedTerms", "foo,bar", "blockingMode", "REDACT_AND_CONTINUE")),
            eq(Scope.WORKSPACE), eq(7L));
    }

    @Test
    void testSaveSettingsWithNullWorkspaceWritesPlatformScope() {
        AiGuardrailsWorkspaceSettings settings =
            new AiGuardrailsWorkspaceSettings(null, null, null, null, null, null, null, null);

        service.saveSettings(settings);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), eq(Map.of()), eq(Scope.PLATFORM), isNull());
    }

    private static Property property(Map<String, ?> value) {
        Property property = new Property();

        property.setValue(value);

        return property;
    }
}
