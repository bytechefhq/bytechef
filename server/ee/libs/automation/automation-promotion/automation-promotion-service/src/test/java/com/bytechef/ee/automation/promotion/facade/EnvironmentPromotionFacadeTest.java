/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnvironmentPromotionFacadeTest {

    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final EnvironmentPromotionHandler handler = mock(EnvironmentPromotionHandler.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private EnvironmentPromotionFacadeImpl facade;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(handler.getResourceType()).thenReturn(PromotionResourceType.MCP_SERVER);
        when(environmentService.getEnvironments())
            .thenReturn(List.of(Environment.DEVELOPMENT, Environment.STAGING, Environment.PRODUCTION));
        when(environmentService.getEnvironment(Environment.STAGING.ordinal())).thenReturn(Environment.STAGING);

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        facade = new EnvironmentPromotionFacadeImpl(environmentService, List.of(handler), meterRegistryProvider);
    }

    @Test
    void testPreviewDispatchesToHandlerByResourceType() {
        EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
            PromotionResourceType.MCP_SERVER, 9L, Environment.DEVELOPMENT, Environment.STAGING, null, null,
            List.of(), List.of(), List.of());

        when(handler.preview(9L, Environment.STAGING)).thenReturn(preview);

        assertThat(facade.preview(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal()))
            .isSameAs(preview);
    }

    @Test
    void testUnavailableTargetEnvironmentIsRejected() {
        when(environmentService.getEnvironments()).thenReturn(List.of(Environment.DEVELOPMENT));

        assertThatThrownBy(
            () -> facade.preview(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal()))
                .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testUnknownResourceTypeIsRejected() {
        assertThatThrownBy(
            () -> facade.preview(PromotionResourceType.A2A_SERVER, 9L, Environment.STAGING.ordinal()))
                .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testPromoteRecordsCreatedOutcome() {
        when(handler.promote(9L, Environment.STAGING, Map.of()))
            .thenReturn(new EnvironmentPromotionResult(77L, true, null, List.of()));

        facade.promote(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal(), Map.of());

        assertThat(
            meterRegistry.counter("bytechef_environment_promotion", "resource", "mcp_server", "outcome", "created")
                .count()).isEqualTo(1.0);
    }

    @Test
    void testPromoteRecordsUpdatedOutcome() {
        when(handler.promote(9L, Environment.STAGING, Map.of()))
            .thenReturn(new EnvironmentPromotionResult(77L, false, null, List.of()));

        facade.promote(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal(), Map.of());

        assertThat(
            meterRegistry.counter("bytechef_environment_promotion", "resource", "mcp_server", "outcome", "updated")
                .count()).isEqualTo(1.0);
    }

    @Test
    void testPromoteRecordsFailedOutcomeAndRethrows() {
        when(handler.promote(9L, Environment.STAGING, Map.of())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(
            () -> facade.promote(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal(), Map.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
            meterRegistry.counter("bytechef_environment_promotion", "resource", "mcp_server", "outcome", "failed")
                .count()).isEqualTo(1.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDuplicateResourceTypeHandlersFailFast() {
        EnvironmentPromotionHandler duplicateHandler = mock(EnvironmentPromotionHandler.class);

        when(duplicateHandler.getResourceType()).thenReturn(PromotionResourceType.MCP_SERVER);

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        assertThatThrownBy(() -> new EnvironmentPromotionFacadeImpl(
            environmentService, List.of(handler, duplicateHandler), meterRegistryProvider))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testNoRegisteredHandlersRejectsEveryResourceType() {
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        EnvironmentPromotionFacadeImpl emptyFacade =
            new EnvironmentPromotionFacadeImpl(environmentService, List.of(), meterRegistryProvider);

        assertThatThrownBy(
            () -> emptyFacade.preview(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal()))
                .isInstanceOf(ConfigurationException.class);
    }
}
