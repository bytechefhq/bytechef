/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.cost;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW", "EI"
})
public class OtlpCostResolver {

    private static final Logger log = LoggerFactory.getLogger(OtlpCostResolver.class);

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private static final Duration MODEL_NOT_REGISTERED_LOG_DEDUP_WINDOW = Duration.ofMinutes(1);

    private static final String UNRESOLVED_METRIC = "bytechef_ai_otlp_cost_unresolved";

    private static final int MODEL_DEDUP_MAP_SOFT_CAP = 10_000;

    private final AiModelService aiModelService;
    private final MeterRegistry meterRegistry;

    private final Map<String, Instant> modelNotRegisteredLastLoggedAt = new ConcurrentHashMap<>();

    public OtlpCostResolver(
        AiModelService aiModelService, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.aiModelService = aiModelService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    enum Reason {
        MISSING_TOKENS,
        MISSING_INPUT_TOKENS,
        MISSING_OUTPUT_TOKENS,
        MODEL_IDENTIFIER_MISSING,
        MODEL_NOT_REGISTERED,
        RATES_MISSING,
        RATES_INVALID,
        TOKENS_INVALID,
        PARTIAL_FREE_TIER
    }

    public BigDecimal computeCost(String modelIdentifier, Integer inputTokens, Integer outputTokens) {
        if (modelIdentifier == null) {
            if (inputTokens != null || outputTokens != null) {
                log.warn(
                    "OTLP cost lookup found tokens (input={}, output={}) but no model identifier — "
                        + "span attribution lost",
                    inputTokens, outputTokens);

                incrementUnresolvedCounter(Reason.MODEL_IDENTIFIER_MISSING);
            } else {
                incrementUnresolvedCounter(Reason.MISSING_TOKENS);
            }

            return null;
        }

        if (inputTokens == null && outputTokens == null) {
            incrementUnresolvedCounter(Reason.MISSING_TOKENS);

            return null;
        }

        if (inputTokens == null) {
            incrementUnresolvedCounter(Reason.MISSING_INPUT_TOKENS);

            return null;
        }

        if (outputTokens == null) {
            incrementUnresolvedCounter(Reason.MISSING_OUTPUT_TOKENS);

            return null;
        }

        if (inputTokens < 0 || outputTokens < 0) {
            log.warn(
                "OTLP cost lookup received negative token count for model '{}' (input={}, output={}) — refusing "
                    + "to compute attribution",
                modelIdentifier, inputTokens, outputTokens);

            incrementUnresolvedCounter(Reason.TOKENS_INVALID);

            return null;
        }

        Optional<AiModel> modelOptional = aiModelService.findByModelIdentifier(modelIdentifier);

        if (modelOptional.isEmpty()) {
            if (shouldLogModelNotRegistered(modelIdentifier)) {
                log.warn("OTLP cost lookup found no registered model for identifier '{}'", modelIdentifier);
            }

            incrementUnresolvedCounter(Reason.MODEL_NOT_REGISTERED);

            return null;
        }

        AiModel model = modelOptional.get();

        BigDecimal inputRate = model.getInputCostPerMTokens();
        BigDecimal outputRate = model.getOutputCostPerMTokens();

        if (inputRate == null || outputRate == null) {
            log.warn(
                "OTLP cost lookup found model '{}' but cost rates are missing (inputRate={}, outputRate={})",
                modelIdentifier, inputRate, outputRate);

            incrementUnresolvedCounter(Reason.RATES_MISSING);

            return null;
        }

        if (inputRate.signum() < 0 || outputRate.signum() < 0) {
            log.warn(
                "OTLP cost lookup found model '{}' but cost rates are negative (inputRate={}, outputRate={})",
                modelIdentifier, inputRate, outputRate);

            incrementUnresolvedCounter(Reason.RATES_INVALID);

            return null;
        }

        if (inputRate.signum() == 0 && outputRate.signum() == 0) {
            return BigDecimal.ZERO;
        }

        if (inputRate.signum() == 0 || outputRate.signum() == 0) {
            incrementUnresolvedCounter(Reason.PARTIAL_FREE_TIER);
        }

        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
            .multiply(inputRate)
            .divide(ONE_MILLION, MathContext.DECIMAL64);
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
            .multiply(outputRate)
            .divide(ONE_MILLION, MathContext.DECIMAL64);

        return inputCost.add(outputCost);
    }

    private void incrementUnresolvedCounter(Reason reason) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(UNRESOLVED_METRIC)
                .tag("reason", reason.name())
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            log.debug("Unresolved-cost counter increment failed (reason={})", reason, meterFailure);
        }
    }

    private boolean shouldLogModelNotRegistered(String modelIdentifier) {
        Instant now = Instant.now();

        Instant mappedValue = modelNotRegisteredLastLoggedAt.compute(modelIdentifier, (key, lastLoggedAt) -> {
            if (lastLoggedAt == null || Duration.between(lastLoggedAt, now)
                .compareTo(MODEL_NOT_REGISTERED_LOG_DEDUP_WINDOW) >= 0) {

                return now;
            }

            return lastLoggedAt;
        });

        if (modelNotRegisteredLastLoggedAt.size() > MODEL_DEDUP_MAP_SOFT_CAP) {
            pruneDedupMap(now);
        }

        return mappedValue == now;
    }

    private void pruneDedupMap(Instant now) {
        Iterator<Map.Entry<String, Instant>> iterator = modelNotRegisteredLastLoggedAt.entrySet()
            .iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();

            if (Duration.between(entry.getValue(), now)
                .compareTo(MODEL_NOT_REGISTERED_LOG_DEDUP_WINDOW) >= 0) {

                iterator.remove();
            }
        }

        if (modelNotRegisteredLastLoggedAt.size() > MODEL_DEDUP_MAP_SOFT_CAP) {
            modelNotRegisteredLastLoggedAt.clear();
        }
    }
}
