/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
            AiGuardrailsSettingsScope.WORKSPACE, 7L, true, null, "foo,bar", null, null, null,
            BlockingMode.REDACT_AND_CONTINUE, null, null,
            null);

        service.saveSettings(settings);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY),
            eq(Map.of("redactPii", true, "blockedTerms", "foo,bar", "blockingMode", "REDACT_AND_CONTINUE")),
            eq(Scope.WORKSPACE), eq(7L));
    }

    @Test
    void testSaveSettingsWithNullWorkspaceWritesPlatformScope() {
        AiGuardrailsWorkspaceSettings settings = new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.PLATFORM, null, null, null, null, null, null, null, null, null, null,
            null);

        service.saveSettings(settings);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), eq(Map.of()), eq(Scope.PLATFORM), isNull());
    }

    @Test
    void testSaveSettingsWritesMinConfidenceWhenSet() {
        AiGuardrailsWorkspaceSettings settings = new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, 7L, null, null, null, null, null, null, null, 0.95, null,
            null);

        service.saveSettings(settings);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), eq(Map.of("minConfidence", 0.95)), eq(Scope.WORKSPACE),
            eq(7L));
    }

    @Test
    void testFetchSettingsReadsMinConfidenceWhenPresent() {
        when(propertyService.fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.WORKSPACE, 7L))
            .thenReturn(Optional.of(property(Map.of("minConfidence", 0.95))));

        AiGuardrailsWorkspaceSettings settings = service.fetchSettings(7L)
            .orElseThrow();

        assertThat(settings.minConfidence()).isEqualTo(0.95);
    }

    @Test
    void testFetchSettingsMinConfidenceIsNullWhenAbsent() {
        when(propertyService.fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.WORKSPACE, 7L))
            .thenReturn(Optional.of(property(Map.of("redactPii", true))));

        AiGuardrailsWorkspaceSettings settings = service.fetchSettings(7L)
            .orElseThrow();

        assertThat(settings.minConfidence()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRedactMcpResultsRoundTripsAndIsAbsentFromOldRows() {
        AiGuardrailsWorkspaceSettings saved = new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, 1L, null, null, null, null, null, null, null, null, true,
            null);

        service.saveSettings(saved);

        ArgumentCaptor<Map<String, Object>> valueCaptor = ArgumentCaptor.forClass(Map.class);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), valueCaptor.capture(), eq(Scope.WORKSPACE), eq(1L));

        when(propertyService.fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.WORKSPACE, 1L))
            .thenReturn(Optional.of(property(valueCaptor.getValue())));

        Optional<AiGuardrailsWorkspaceSettings> fetched = service.fetchSettings(1L);

        assertThat(fetched).isPresent();
        assertThat(fetched.get()
            .redactMcpResults()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRestoreIntoWorkflowOutputRoundTripsAndIsAbsentFromOldRows() {
        AiGuardrailsWorkspaceSettings saved = new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, 1L, null, null, null, null, null, null, null, null, null,
            false);

        service.saveSettings(saved);

        ArgumentCaptor<Map<String, Object>> valueCaptor = ArgumentCaptor.forClass(Map.class);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), valueCaptor.capture(), eq(Scope.WORKSPACE), eq(1L));

        when(propertyService.fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.WORKSPACE, 1L))
            .thenReturn(Optional.of(property(valueCaptor.getValue())));

        Optional<AiGuardrailsWorkspaceSettings> fetched = service.fetchSettings(1L);

        assertThat(fetched).isPresent();
        assertThat(fetched.get()
            .restoreIntoWorkflowOutput()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testARowStoredBeforeThisFieldExistedReadsAsNull() {
        AiGuardrailsWorkspaceSettings settings = new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, 1L, true, null, null, null, null, null, null, null, null,
            null);

        service.saveSettings(settings);

        ArgumentCaptor<Map<String, Object>> valueCaptor = ArgumentCaptor.forClass(Map.class);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), valueCaptor.capture(), eq(Scope.WORKSPACE), eq(1L));

        when(propertyService.fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.WORKSPACE, 1L))
            .thenReturn(Optional.of(property(valueCaptor.getValue())));

        Optional<AiGuardrailsWorkspaceSettings> fetched = service.fetchSettings(1L);

        assertThat(fetched).isPresent();
        assertThat(fetched.get()
            .redactMcpResults())
                .as("absent key must read as null, not false, so it unions as 'not set at this level'")
                .isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEmbeddedSettingsRoundTripIndependentlyOfAnyWorkspaceRow() {
        service.saveSettings(new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.EMBEDDED, null, null, null, null, null, null, null, null, null, true,
            null));

        ArgumentCaptor<Map<String, Object>> valueCaptor = ArgumentCaptor.forClass(Map.class);

        verify(propertyService).save(
            eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), valueCaptor.capture(), eq(Scope.EMBEDDED), isNull());

        when(propertyService.fetchProperty(AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Scope.EMBEDDED, null))
            .thenReturn(Optional.of(property(valueCaptor.getValue())));

        Optional<AiGuardrailsWorkspaceSettings> fetched = service.fetchEmbeddedSettings();

        assertThat(fetched).isPresent();
        assertThat(fetched.get()
            .redactMcpResults()).isTrue();
    }

    @Test
    void testEmbeddedAndPlatformScopesAreDistinctRows() {
        Map<String, Property> propertiesByScopeKey = new HashMap<>();

        doAnswer(invocation -> {
            Scope scope = invocation.getArgument(2);
            Long scopeId = invocation.getArgument(3);

            propertiesByScopeKey.put(scopeKey(scope, scopeId), property(invocation.getArgument(1)));

            return null;
        }).when(propertyService)
            .save(eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), any(), any(), any());

        when(propertyService.fetchProperty(eq(AiGuardrailsWorkspaceSettings.PROPERTY_KEY), any(), any()))
            .thenAnswer(invocation -> {
                Scope scope = invocation.getArgument(1);
                Long scopeId = invocation.getArgument(2);

                return Optional.ofNullable(propertiesByScopeKey.get(scopeKey(scope, scopeId)));
            });

        service.saveSettings(new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.EMBEDDED, null, null, null, null, null, null, null, null, null, true,
            null));

        assertThat(service.fetchSettings(null))
            .as("an embedded row must not be readable as the tenant default, or the two scopes collapse "
                + "into the ambiguity this change exists to remove")
            .isEmpty();

        assertThat(service.fetchEmbeddedSettings())
            .as("the row saved under EMBEDDED must still be readable through the EMBEDDED-scoped fetch, "
                + "so an empty PLATFORM read above is scope isolation and not just an empty store")
            .isPresent();
    }

    private static String scopeKey(Scope scope, Long scopeId) {
        return scope + ":" + scopeId;
    }

    private static Property property(Map<String, ?> value) {
        Property property = new Property();

        property.setValue(value);

        return property;
    }
}
