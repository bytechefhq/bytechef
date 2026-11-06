/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayEmbeddedSettings;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @version ee
 */
@SpringBootTest(classes = AiGatewayEmbeddedSettingsIntTestConfiguration.class)
@ActiveProfiles("testint")
@TestPropertySource(properties = "spring.liquibase.contexts=configuration")
class AiGatewayEmbeddedSettingsServiceIntTest {

    @Autowired
    private AiGatewayEmbeddedSettingsService aiGatewayEmbeddedSettingsService;

    @Autowired
    private PropertyService propertyService;

    @Test
    void testUpsertTwiceForSameEnvironmentUpdatesSingleRow() {
        aiGatewayEmbeddedSettingsService.upsert(
            new AiGatewayEmbeddedSettings(101L, 1, 1000, true, 60, 7, 5L, 50, new BigDecimal("25.00")));
        aiGatewayEmbeddedSettingsService.upsert(
            new AiGatewayEmbeddedSettings(101L, 3, 2000, false, 120, 14, 9L, 80, new BigDecimal("40.00")));

        AiGatewayEmbeddedSettings settings = aiGatewayEmbeddedSettingsService.find(101L)
            .orElseThrow();

        assertThat(settings.retryCount()).isEqualTo(3);
        assertThat(settings.timeoutMs()).isEqualTo(2000);
        assertThat(settings.cacheEnabled()).isFalse();
        assertThat(settings.defaultRoutingPolicyId()).isEqualTo(9L);
        assertThat(settings.softBudgetWarningPct()).isEqualTo(80);
        assertThat(settings.defaultConnectedUserBudgetCap()).isEqualByComparingTo(new BigDecimal("40.00"));

        assertThat(propertyService.getProperties(
            List.of(AiGatewayEmbeddedSettings.PROPERTY_KEY), Property.Scope.EMBEDDED, null, 101L)).hasSize(1);
    }

    @Test
    void testUpsertForDifferentEnvironmentsProducesTwoRows() {
        aiGatewayEmbeddedSettingsService.upsert(
            new AiGatewayEmbeddedSettings(102L, 1, 1000, true, 60, 7, 5L, 50, new BigDecimal("25.00")));
        aiGatewayEmbeddedSettingsService.upsert(
            new AiGatewayEmbeddedSettings(103L, 2, 1500, false, 90, 10, 6L, 60, new BigDecimal("30.00")));

        assertThat(aiGatewayEmbeddedSettingsService.find(102L)
            .orElseThrow()
            .retryCount()).isEqualTo(1);
        assertThat(aiGatewayEmbeddedSettingsService.find(103L)
            .orElseThrow()
            .retryCount()).isEqualTo(2);
        assertThat(aiGatewayEmbeddedSettingsService.find(102L)
            .orElseThrow()
            .defaultConnectedUserBudgetCap()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(aiGatewayEmbeddedSettingsService.find(103L)
            .orElseThrow()
            .defaultConnectedUserBudgetCap()).isEqualByComparingTo(new BigDecimal("30.00"));

        assertThat(propertyService.getProperties(
            List.of(AiGatewayEmbeddedSettings.PROPERTY_KEY), Property.Scope.EMBEDDED, null, 102L)).hasSize(1);
        assertThat(propertyService.getProperties(
            List.of(AiGatewayEmbeddedSettings.PROPERTY_KEY), Property.Scope.EMBEDDED, null, 103L)).hasSize(1);
    }
}
