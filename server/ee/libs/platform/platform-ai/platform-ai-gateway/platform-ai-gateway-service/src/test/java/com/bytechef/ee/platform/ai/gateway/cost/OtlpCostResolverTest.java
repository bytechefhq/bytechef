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

        // The MODEL_NOT_REGISTERED reason tag is the operational signal — confirm the counter is wired so a
        // future regression that silently drops the increment is caught.
        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MODEL_NOT_REGISTERED")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testReturnsNullAndCountsMissingTokensWhenBothMissing() {
        // Both token attributes absent — legitimate embedding-only / stream-only span. Counter-only signal;
        // any warn-level emission would flood logs for normal traffic.
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
        // Asymmetric loss surfaces a buggy exporter dropping a single attribute. Without this distinction
        // both cases bucket into MISSING_TOKENS, masking exporter bugs as legitimate embedding-only spans
        // on the dashboard.
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
        // The MODEL_NOT_REGISTERED / RATES_MISSING / RATES_INVALID counter taps need to be paired with a warn
        // log so an operator immediately sees what's happening — counters alone require a Grafana dashboard
        // to interpret. A regression dropping the warn would still pass counter assertions in other tests
        // but leave the operational signal hidden.
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
        // Provider-rotation onto a not-yet-catalogued model can fire 1k+ spans/min — warn-logging each line
        // would flood structured-log pipelines without adding signal beyond the counter (already wired).
        // Dedup gate must emit at most once per model id per window.
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

            // Counter must still increment for every span — dashboards see true volume independent of log
            // dedup.
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
        // Pins the MODEL_IDENTIFIER_MISSING reason tag — distinct from MISSING_TOKENS. A protobuf-mapper
        // regression that drops gen_ai.*.model attributes from spans must surface here, not collapse into a
        // legitimate-looking $0 dashboard reading. A regression returning the same null without the
        // distinct counter tag would silently bypass operator alarms.
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(mock(AiModelService.class), staticProvider(registry));

        assertThat(resolver.computeCost(null, 100, 100)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MODEL_IDENTIFIER_MISSING")
            .counter()
            .count()).isEqualTo(1.0);

        // And it must NOT collapse into MISSING_TOKENS — that would silently mask the regression on dashboards.
        assertThat(registry.find("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "MISSING_TOKENS")
            .counter()).isNull();
    }

    @Test
    void testReturnsNullAndCountsRatesInvalidWhenRatesAreNegative() {
        // Pins the RATES_INVALID reason tag for negative rates. A negative rate is a data-entry typo and must
        // be suppressed from dashboards — but counted with its distinct reason so a Grafana alert can fire
        // before a billing cycle ships under-attributed.
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
        // Free-tier / self-hosted models legitimately have rate exactly 0. The resolver must compute and
        // return BigDecimal.ZERO so dashboards distinguish "$0 free-tier traffic" from "no cost data" (the
        // null-return paths). A regression bucketing zero into RATES_INVALID would hide free-tier workspaces
        // from spend dashboards.
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

        // Must NOT increment the unresolved counter — free-tier is a resolved-but-zero outcome, not unresolved.
        assertThat(registry.find("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "RATES_INVALID")
            .counter()).isNull();
    }

    @Test
    void testMixedZeroRateFallsThroughToMultiplication() {
        // Asymmetric (input=0, output=non-zero) is a real-money path: the model is free for prompts but
        // billed for completions (some embedding+chat hybrids advertise this). The resolver MUST NOT
        // short-circuit on either-zero — only both-zero qualifies for the free-tier fast-path. A regression
        // returning ZERO for any-zero would attribute output cost as $0 and silently understate spend.
        AiModelService modelService = mock(AiModelService.class);

        AiModel model = mock(AiModel.class);

        when(model.getInputCostPerMTokens()).thenReturn(BigDecimal.ZERO);
        when(model.getOutputCostPerMTokens()).thenReturn(new BigDecimal("0.01"));
        when(modelService.findByModelIdentifier("mixed-zero-model"))
            .thenReturn(Optional.of(model));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        BigDecimal cost = resolver.computeCost("mixed-zero-model", 100, 100);

        // Expected: (100 * 0 + 100 * 0.01) / 1_000_000 = 0.000001
        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.000001"));

        // PARTIAL_FREE_TIER counter MUST fire: a misconfigured catalog row producing half-attributed cost
        // across the board is operator-actionable. A regression dropping the counter (e.g., during a refactor
        // that consolidates "any-zero" into the free-tier short-circuit) would silently kill the alarm.
        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "PARTIAL_FREE_TIER")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testReturnsNullAndCountsTokensInvalidWhenInputTokensNegative() {
        // Defense against compromised / buggy exporters: a negative input-token count would multiply against a
        // positive rate to produce a negative cost row, silently SUBTRACTING from the trace's total_cost
        // aggregate. A regression that allowed the multiplication to proceed would let a single rogue exporter
        // effectively zero-out a workspace's billed cost. The TOKENS_INVALID counter MUST fire so a Grafana
        // alert can trigger before a billing cycle ships corrupted.
        AiModelService modelService = mock(AiModelService.class);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OtlpCostResolver resolver = new OtlpCostResolver(modelService, staticProvider(registry));

        assertThat(resolver.computeCost("gpt-4o", -100, 200)).isNull();

        assertThat(registry.get("bytechef_ai_otlp_cost_unresolved")
            .tag("reason", "TOKENS_INVALID")
            .counter()
            .count()).isEqualTo(1.0);

        // Critical: model lookup MUST NOT happen for invalid token counts — the validation is upstream of the
        // catalog hit, so a regression that moved the check below the model lookup would still pass this test
        // unless we pin the absence here.
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

        // Single counter increment, not double — both-negative is one corruption event, not two.
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
