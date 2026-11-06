/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.cost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 * @version ee
 */
class OtlpCostResolverTest {

    @Test
    void testComputesCostFromKnownModel() {
        AiModelService modelService = mock(AiModelService.class);

        AiModel model = mock(AiModel.class);

        when(model.getInputCostPerMTokens()).thenReturn(new BigDecimal("2.50"));
        when(model.getOutputCostPerMTokens()).thenReturn(new BigDecimal("10.00"));
        when(modelService.findByModelIdentifier("gpt-4o"))
            .thenReturn(Optional.of(model));

        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(new SimpleMeterRegistry()));

        BigDecimal cost = resolver.computeCost("gpt-4o", 1000, 500);

        assertThat(cost).isEqualByComparingTo("0.0075");
    }

    @Test
    void testReturnsNullWhenModelUnknown() {
        AiModelService modelService = mock(AiModelService.class);

        when(modelService.findByModelIdentifier("unknown-model"))
            .thenReturn(Optional.empty());

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        assertThat(resolver.computeCost("unknown-model", 100, 100)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MODEL_NOT_REGISTERED")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testReturnsNullAndCountsMissingTokensWhenBothMissing() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(mock(AiModelService.class), staticProvider(registry));

        assertThat(resolver.computeCost("gpt-4o", null, null)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MISSING_TOKENS")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testDistinguishesMissingInputFromMissingOutputTokens() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(mock(AiModelService.class), staticProvider(registry));

        assertThat(resolver.computeCost("gpt-4o", null, 100)).isNull();
        assertThat(resolver.computeCost("gpt-4o", 100, null)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MISSING_INPUT_TOKENS")
            .counter()
            .count()).isEqualTo(1.0);
        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MISSING_OUTPUT_TOKENS")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testEmitsWarnLogForRegisteredOperationalReasons() {
        AiModelService modelService = mock(AiModelService.class);

        when(modelService.findByModelIdentifier("rare-unknown-model"))
            .thenReturn(Optional.empty());

        ListAppender<ILoggingEvent> appender = attachAppenderToCostResolverLogger();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        try {
            resolver.computeCost("rare-unknown-model", 100, 100);

            assertThat(appender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                    && event.getFormattedMessage()
                        .contains("rare-unknown-model"));
        } finally {
            detachAppenderFromCostResolverLogger(appender);
        }
    }

    @Test
    void testDedupsModelNotRegisteredWarnLogPerModelInWindow() {
        AiModelService modelService = mock(AiModelService.class);

        when(modelService.findByModelIdentifier("flooded-model"))
            .thenReturn(Optional.empty());

        ListAppender<ILoggingEvent> appender = attachAppenderToCostResolverLogger();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        try {
            for (int callIndex = 0; callIndex < 50; callIndex++) {
                resolver.computeCost("flooded-model", 100, 100);
            }

            long warnCountForFloodedModel = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .filter(event -> event.getFormattedMessage()
                    .contains("flooded-model"))
                .count();

            assertThat(warnCountForFloodedModel).isEqualTo(1L);

            assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
                .tag("reason", "MODEL_NOT_REGISTERED")
                .counter()
                .count()).isEqualTo(50.0);
        } finally {
            detachAppenderFromCostResolverLogger(appender);
        }
    }

    private static ListAppender<ILoggingEvent> attachAppenderToCostResolverLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(OtlpCostResolver.class);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        return appender;
    }

    private static void detachAppenderFromCostResolverLogger(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(OtlpCostResolver.class);

        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void testReturnsNullWhenModelHasNullRates() {
        AiModelService modelService = mock(AiModelService.class);

        AiModel model = mock(AiModel.class);

        when(model.getInputCostPerMTokens()).thenReturn(null);
        when(model.getOutputCostPerMTokens()).thenReturn(null);
        when(modelService.findByModelIdentifier("unpriced-model"))
            .thenReturn(Optional.of(model));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        assertThat(resolver.computeCost("unpriced-model", 100, 100)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "RATES_MISSING")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testReturnsNullAndCountsModelIdentifierMissingWhenIdentifierIsNull() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(mock(AiModelService.class), staticProvider(registry));

        assertThat(resolver.computeCost(null, 100, 100)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MODEL_IDENTIFIER_MISSING")
            .counter()
            .count()).isEqualTo(1.0);

        assertThat(registry.find("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MISSING_TOKENS")
            .counter()).isNull();
    }

    @Test
    void testReturnsNullAndCountsRatesInvalidWhenRatesAreNegative() {
        AiModelService modelService = mock(AiModelService.class);

        AiModel model = mock(AiModel.class);

        when(model.getInputCostPerMTokens()).thenReturn(new BigDecimal("-1.00"));
        when(model.getOutputCostPerMTokens()).thenReturn(new BigDecimal("10.00"));
        when(modelService.findByModelIdentifier("typo-priced-model"))
            .thenReturn(Optional.of(model));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        assertThat(resolver.computeCost("typo-priced-model", 100, 100)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "RATES_INVALID")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testReturnsZeroForFreeTierModelWithRatesExactlyZero() {
        AiModelService modelService = mock(AiModelService.class);

        AiModel model = mock(AiModel.class);

        when(model.getInputCostPerMTokens()).thenReturn(BigDecimal.ZERO);
        when(model.getOutputCostPerMTokens()).thenReturn(BigDecimal.ZERO);
        when(modelService.findByModelIdentifier("free-tier-model"))
            .thenReturn(Optional.of(model));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        BigDecimal cost = resolver.computeCost("free-tier-model", 100, 100);

        assertThat(cost).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(registry.find("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "RATES_INVALID")
            .counter()).isNull();
    }

    @Test
    void testMixedZeroRateFallsThroughToMultiplication() {
        AiModelService modelService = mock(AiModelService.class);

        AiModel model = mock(AiModel.class);

        when(model.getInputCostPerMTokens()).thenReturn(BigDecimal.ZERO);
        when(model.getOutputCostPerMTokens()).thenReturn(new BigDecimal("0.01"));
        when(modelService.findByModelIdentifier("mixed-zero-model"))
            .thenReturn(Optional.of(model));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        BigDecimal cost = resolver.computeCost("mixed-zero-model", 100, 100);

        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.000001"));

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "PARTIAL_FREE_TIER")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testReturnsNullAndCountsTokensInvalidWhenInputTokensNegative() {
        AiModelService modelService = mock(AiModelService.class);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        assertThat(resolver.computeCost("gpt-4o", -100, 200)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "TOKENS_INVALID")
            .counter()
            .count()).isEqualTo(1.0);

        verify(modelService, never()).findByModelIdentifier(any());
    }

    @Test
    void testReturnsNullAndCountsTokensInvalidWhenOutputTokensNegative() {
        AiModelService modelService = mock(AiModelService.class);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        assertThat(resolver.computeCost("gpt-4o", 100, -200)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "TOKENS_INVALID")
            .counter()
            .count()).isEqualTo(1.0);

        verify(modelService, never()).findByModelIdentifier(any());
    }

    @Test
    void testReturnsNullAndCountsTokensInvalidWhenBothTokensNegative() {
        AiModelService modelService = mock(AiModelService.class);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        assertThat(resolver.computeCost("gpt-4o", -100, -200)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "TOKENS_INVALID")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> staticProvider(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(registry);

        return provider;
    }
}
