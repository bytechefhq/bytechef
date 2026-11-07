/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.component.rule.ComponentRuleSettings;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 * @author Ivica Cardic
 */
class ComponentRuleSettingsServiceTest {

    private static final String PROPERTY_KEY = ComponentRuleSettings.PROPERTY_KEY;

    private final PropertyService propertyService = mock(PropertyService.class);
    private final ComponentRuleSettingsServiceImpl componentRuleSettingsService =
        new ComponentRuleSettingsServiceImpl(propertyService);

    @Test
    void testAWorkspaceOverrideWinsOverTheTenantDefault() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.WORKSPACE, 42L))
            .thenReturn(Optional.of(property(Map.of("observeMode", true, "approvalExpiresInHours", 24))));

        ComponentRuleSettings componentRuleSettings = componentRuleSettingsService.getSettings(42L);

        assertThat(componentRuleSettings.observeMode()).isTrue();
        assertThat(componentRuleSettings.approvalExpiresInHours()).isEqualTo(24);

        verify(propertyService, never()).fetchProperty(PROPERTY_KEY, Scope.PLATFORM, null);
    }

    @Test
    void testAbsentAnOverrideTheTenantDefaultApplies() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.WORKSPACE, 42L)).thenReturn(Optional.empty());
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.PLATFORM, null))
            .thenReturn(Optional.of(property(Map.of("observeMode", true, "approvalExpiresInHours", 12))));

        assertThat(componentRuleSettingsService.getSettings(42L)
            .observeMode()).isTrue();
    }

    @Test
    void testAbsentBothTheDocumentedDefaultApplies() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.WORKSPACE, 42L)).thenReturn(Optional.empty());
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.PLATFORM, null)).thenReturn(Optional.empty());

        assertThat(componentRuleSettingsService.getSettings(42L)).isEqualTo(ComponentRuleSettings.DEFAULT);
    }

    @Test
    void testANullWorkspaceAsksForTheTenantDefaultDirectly() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.PLATFORM, null))
            .thenReturn(Optional.of(property(Map.of("observeMode", true, "approvalExpiresInHours", 24))));

        ComponentRuleSettings componentRuleSettings = componentRuleSettingsService.getSettings(null);

        assertThat(componentRuleSettings.observeMode()).isTrue();
        assertThat(componentRuleSettings.approvalExpiresInHours()).isEqualTo(24);

        verify(propertyService, never()).fetchProperty(eq(PROPERTY_KEY), eq(Scope.WORKSPACE), anyLong());
    }

    @Test
    void testGetSettingsFallsBackToTheDefaultWhenTheStoredMapIsMissingAKey() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.PLATFORM, null))
            .thenReturn(Optional.of(property(Map.of("observeMode", true))));

        ComponentRuleSettings componentRuleSettings = componentRuleSettingsService.getSettings(null);

        assertThat(componentRuleSettings.observeMode()).isTrue();
        assertThat(componentRuleSettings.approvalExpiresInHours())
            .isEqualTo(ComponentRuleSettings.DEFAULT.approvalExpiresInHours());
    }

    @Test
    void testGetSettingsFallsBackToTheDefaultWhenThePropertyServiceThrows() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.WORKSPACE, 42L))
            .thenThrow(new UnsupportedOperationException());

        assertThat(componentRuleSettingsService.getSettings(42L)).isEqualTo(ComponentRuleSettings.DEFAULT);
    }

    @Test
    void testGetSettingsFallsBackToTheDefaultWhenAStoredValueHasTheWrongType() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.PLATFORM, null))
            .thenReturn(
                Optional.of(property(Map.of("observeMode", "yes", "approvalExpiresInHours", "twenty-four"))));

        ComponentRuleSettings componentRuleSettings = componentRuleSettingsService.getSettings(null);

        assertThat(componentRuleSettings).isEqualTo(ComponentRuleSettings.DEFAULT);
    }

    @Test
    void testFetchWorkspaceOverrideReturnsTheRowWhenOneExists() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.WORKSPACE, 42L))
            .thenReturn(Optional.of(property(Map.of("observeMode", true, "approvalExpiresInHours", 24))));

        Optional<ComponentRuleSettings> workspaceOverride = componentRuleSettingsService.fetchWorkspaceOverride(42L);

        assertThat(workspaceOverride).isPresent();
        assertThat(workspaceOverride.get()
            .observeMode()).isTrue();
        assertThat(workspaceOverride.get()
            .approvalExpiresInHours()).isEqualTo(24);
    }

    @Test
    void testFetchWorkspaceOverrideIsEmptyWhenTheWorkspaceHasNoOverrideOfItsOwn() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.WORKSPACE, 42L)).thenReturn(Optional.empty());

        assertThat(componentRuleSettingsService.fetchWorkspaceOverride(42L)).isEmpty();
    }

    @Test
    void testFetchWorkspaceOverrideIsEmptyForANullWorkspace() {
        assertThat(componentRuleSettingsService.fetchWorkspaceOverride(null)).isEmpty();

        verify(propertyService, never()).fetchProperty(eq(PROPERTY_KEY), eq(Scope.WORKSPACE), anyLong());
    }

    @Test
    void testFetchWorkspaceOverrideDegradesToEmptyWhenThePropertyServiceThrows() {
        when(propertyService.fetchProperty(PROPERTY_KEY, Scope.WORKSPACE, 42L))
            .thenThrow(new UnsupportedOperationException());

        assertThat(componentRuleSettingsService.fetchWorkspaceOverride(42L)).isEmpty();
    }

    @Test
    void testSavingAWorkspaceOverrideWritesAWorkspaceScopedRow() {
        componentRuleSettingsService.saveSettings(new ComponentRuleSettings(true, 12), 42L);

        verify(propertyService).save(
            eq(PROPERTY_KEY), eq(Map.of("observeMode", true, "approvalExpiresInHours", 12)),
            eq(Scope.WORKSPACE), eq(42L));
    }

    @Test
    void testSavingTheTenantDefaultWritesAPlatformScopedRowWithANullScopeId() {
        componentRuleSettingsService.saveSettings(new ComponentRuleSettings(true, 12), null);

        verify(propertyService).save(
            eq(PROPERTY_KEY), eq(Map.of("observeMode", true, "approvalExpiresInHours", 12)),
            eq(Scope.PLATFORM), isNull());
    }

    private static Property property(Map<String, ?> value) {
        Property property = new Property();

        property.setValue(value);

        return property;
    }
}
