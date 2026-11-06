/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayEmbeddedSettings;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.math.BigDecimal;
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
class AiGatewayEmbeddedSettingsServiceTest {

    private static final long ENVIRONMENT_ID = 1L;

    @Mock
    private PropertyService propertyService;

    private AiGatewayEmbeddedSettingsService aiGatewayEmbeddedSettingsService;

    @BeforeEach
    void setUp() {
        aiGatewayEmbeddedSettingsService = new AiGatewayEmbeddedSettingsServiceImpl(propertyService);
    }

    @Test
    void testFindReturnsEmptyWhenNoPropertyRowExists() {
        when(propertyService.fetchProperty(
            AiGatewayEmbeddedSettings.PROPERTY_KEY, Property.Scope.EMBEDDED, null, ENVIRONMENT_ID))
                .thenReturn(Optional.empty());

        assertThat(aiGatewayEmbeddedSettingsService.find(ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testFindMapsStoredValues() {
        Property property = mock(Property.class);

        doReturn(Map.of("defaultRoutingPolicyId", 7, "retryCount", 3)).when(property)
            .getValue();
        when(propertyService.fetchProperty(
            AiGatewayEmbeddedSettings.PROPERTY_KEY, Property.Scope.EMBEDDED, null, ENVIRONMENT_ID))
                .thenReturn(Optional.of(property));

        AiGatewayEmbeddedSettings settings = aiGatewayEmbeddedSettingsService.find(ENVIRONMENT_ID)
            .orElseThrow();

        assertThat(settings.defaultRoutingPolicyId()).isEqualTo(7L);
        assertThat(settings.retryCount()).isEqualTo(3);
        assertThat(settings.timeoutMs()).isNull();
        assertThat(settings.defaultConnectedUserBudgetCap()).isNull();
    }

    @Test
    void testFindMapsConnectedUserBudgetCapFromANonBigDecimalNumber() {
        Property property = mock(Property.class);

        doReturn(Map.of("defaultConnectedUserBudgetCap", 50.0)).when(property)
            .getValue();
        when(propertyService.fetchProperty(
            AiGatewayEmbeddedSettings.PROPERTY_KEY, Property.Scope.EMBEDDED, null, ENVIRONMENT_ID))
                .thenReturn(Optional.of(property));

        AiGatewayEmbeddedSettings settings = aiGatewayEmbeddedSettingsService.find(ENVIRONMENT_ID)
            .orElseThrow();

        assertThat(settings.defaultConnectedUserBudgetCap()).isEqualByComparingTo(new BigDecimal("50.0"));
    }

    @Test
    void testUpsertSavesAtEmbeddedScopeWithNullScopeIdAndAnEnvironment() {
        AiGatewayEmbeddedSettings settings =
            new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, 9L, null, null);

        aiGatewayEmbeddedSettingsService.upsert(settings);

        verify(propertyService).save(
            eq(AiGatewayEmbeddedSettings.PROPERTY_KEY), anyMap(), eq(Property.Scope.EMBEDDED), isNull(),
            eq(ENVIRONMENT_ID));
    }

    @Test
    void testRejectsOutOfRangeSoftBudgetWarningPct() {
        assertThatThrownBy(
            () -> new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, null, 101, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsNonPositiveConnectedUserBudgetCap() {
        assertThatThrownBy(
            () -> new AiGatewayEmbeddedSettings(
                ENVIRONMENT_ID, null, null, null, null, null, null, null, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
    }
}
