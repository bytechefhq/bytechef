/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.cost;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelService;
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
 * Computes span cost server-side from token counts x per-model pricing. Returns {@code null} (not zero) when the model
 * is not registered with the gateway - prevents silent "free" traces from polluting spend dashboards.
 *
 * <p>
 * Distinct null-returning conditions, each tagged on the {@code bytechef_ai_otlp_cost_unresolved{reason}} counter so
 * operators can wire targeted Grafana alerts:
 * <ul>
 * <li>{@code MISSING_TOKENS} — span had no {@code gen_ai.usage.*} attributes (normal, e.g. embedding-only spans).</li>
 * <li>{@code MISSING_INPUT_TOKENS} / {@code MISSING_OUTPUT_TOKENS} — the model identifier was present but exactly one
 * of the input / output token counts was missing. Distinguished from {@code MISSING_TOKENS} (which is "exporter
 * intentionally emitted no usage at all") because asymmetric loss usually points at a buggy exporter stripping a single
 * attribute — a different operational signal than the all-or-nothing case.</li>
 * <li>{@code MODEL_IDENTIFIER_MISSING} — protobuf-mapper bug: the span lost both {@code gen_ai.response.model} and
 * {@code gen_ai.request.model}. Looks indistinguishable from a missing-tokens span on a $0 dashboard, but is actually a
 * regression that drops cost attribution for an entire workspace; warn-level so it surfaces in operator log
 * streams.</li>
 * <li>{@code MODEL_NOT_REGISTERED} — operational: the model catalog is missing an entry for this id. The warn log is
 * dedup'd per-model-id over a short window so a provider rotation does not flood structured-log pipelines.</li>
 * <li>{@code RATES_MISSING} — data-quality: the model row exists but has null cost rates.</li>
 * <li>{@code RATES_INVALID} — data-quality: the model row's rates are <em>negative</em>. Always indicates a data-entry
 * error. The resolved cost is suppressed (returns null, so the span carries no cost) but the
 * {@code bytechef_ai_otlp_cost_unresolved{reason=RATES_INVALID}} counter still emits — operators want a Grafana alert
 * to fire when this rate climbs, since it points at corrupted catalog data. A rate of exactly zero is treated
 * separately (see below) as a legitimate free-tier or self-hosted-model value.</li>
 * <li>{@code TOKENS_INVALID} — data-quality: an exporter sent a negative {@code gen_ai.usage.input_tokens} or
 * {@code gen_ai.usage.output_tokens}. Always a bug in the exporter, never a meaningful billing event. The resolved cost
 * is suppressed and the counter fires so the regression surfaces on dashboards instead of producing absurd negative
 * spend totals.</li>
 * <li>{@code PARTIAL_FREE_TIER} — observability-only: emitted when exactly one of the two rates is zero and the other
 * is positive (e.g. a hypothetical "output-only billed" provider, or — far more likely — a misconfigured catalog row).
 * The span still receives a non-null computed cost (so dashboards stay accurate when the configuration is intentional),
 * but the counter lets operators alert on the asymmetry, which is almost always a data-entry bug.</li>
 * </ul>
 * Free-tier or self-hosted models with BOTH rates exactly zero are NOT bucketed into {@code RATES_INVALID}: the
 * resolver computes {@code BigDecimal.ZERO} and persists it, so dashboards distinguish "$0 free-tier" from "no cost
 * data".
 *
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

    /**
     * Soft cap on the per-model log-dedup map. A misbehaving exporter rotating UUID-prefixed model identifiers could
     * otherwise grow the map unboundedly; reaching this cap triggers a prune of entries older than the dedup window,
     * and if still over cap clears the whole map. The latter only means a misbehaving model id might re-log once after
     * the prune — strictly preferable to an unbounded leak.
     */
    private static final int MODEL_DEDUP_MAP_SOFT_CAP = 10_000;

    private final AiGatewayModelService aiGatewayModelService;
    private final MeterRegistry meterRegistry;

    // Per-model log-dedup gate: a workspace whose exporter rotates onto a not-yet-catalogued model can fire
    // 1k+ spans/min — warn-logging each one floods structured-log pipelines without adding signal beyond
    // the (already-wired) bytechef_ai_otlp_cost_unresolved{reason=MODEL_NOT_REGISTERED} counter. Capped at
    // one warn line per model id per MODEL_NOT_REGISTERED_LOG_DEDUP_WINDOW. Bounded by MODEL_DEDUP_MAP_SOFT_CAP
    // via prune-on-overflow inside shouldLogModelNotRegistered().
    private final Map<String, Instant> modelNotRegisteredLastLoggedAt = new ConcurrentHashMap<>();

    public OtlpCostResolver(
        AiGatewayModelService aiGatewayModelService, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.aiGatewayModelService = aiGatewayModelService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    /**
     * Reasons the resolver could not produce a non-null cost. Persisted on the
     * {@code bytechef_ai_otlp_cost_unresolved{reason}} counter via {@link Enum#name()}. Centralising the tags as a
     * typed enum prevents miscategorisation across the resolver's call sites — a string-literal misspelling
     * ({@code "MISSINNG_TOKENS"}) would emit a brand-new metric series with zero dashboard hookup and no compile-time
     * signal. Keep names append-only: dashboards branch on the wire-stable tag value, and renaming would silently break
     * existing alert rules.
     */
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
            // Distinguish "regression" (had tokens, lost model id) from "normal" (no tokens, no model id).
            // The MODEL_IDENTIFIER_MISSING warn-level message is operator-actionable only when the span
            // actually carried token counts — otherwise it's an embedding-only/stream-only span and
            // logging an attribution-loss warning would be misleading (and noisy).
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
            // Both missing: span had no gen_ai.usage.* attributes (legitimate embedding-only / stream-only
            // span). Counter-only because warn-logging the legitimate case would flood logs.
            incrementUnresolvedCounter(Reason.MISSING_TOKENS);

            return null;
        }

        if (inputTokens == null) {
            // Asymmetric loss: an exporter that drops one of the two counts is buggy; surface as a distinct
            // bucket so a Grafana alert can target the asymmetric case without firing on the all-or-nothing
            // legitimate path.
            incrementUnresolvedCounter(Reason.MISSING_INPUT_TOKENS);

            return null;
        }

        if (outputTokens == null) {
            incrementUnresolvedCounter(Reason.MISSING_OUTPUT_TOKENS);

            return null;
        }

        // Defense against compromised / buggy exporters: NEGATIVE token counts would multiply against positive
        // rates to produce a negative cost row, silently SUBTRACTING from the trace's total_cost aggregate. A
        // single rogue exporter could effectively zero out a workspace's billed cost. We refuse to compute
        // attribution for negative tokens and surface a TOKENS_INVALID counter so operators can alert before a
        // billing cycle ships corrupted. We do NOT validate negative rates separately — that path lives below
        // and bucket TOKENS_INVALID intentionally so the two distinct corruption vectors stay visible as
        // separate counter labels.
        if (inputTokens < 0 || outputTokens < 0) {
            log.warn(
                "OTLP cost lookup received negative token count for model '{}' (input={}, output={}) — refusing "
                    + "to compute attribution",
                modelIdentifier, inputTokens, outputTokens);

            incrementUnresolvedCounter(Reason.TOKENS_INVALID);

            return null;
        }

        Optional<AiGatewayModel> modelOptional = aiGatewayModelService.findByModelIdentifier(modelIdentifier);

        if (modelOptional.isEmpty()) {
            if (shouldLogModelNotRegistered(modelIdentifier)) {
                log.warn("OTLP cost lookup found no registered model for identifier '{}'", modelIdentifier);
            }

            incrementUnresolvedCounter(Reason.MODEL_NOT_REGISTERED);

            return null;
        }

        AiGatewayModel model = modelOptional.get();

        BigDecimal inputRate = model.getInputCostPerMTokens();
        BigDecimal outputRate = model.getOutputCostPerMTokens();

        if (inputRate == null || outputRate == null) {
            log.warn(
                "OTLP cost lookup found model '{}' but cost rates are missing (inputRate={}, outputRate={})",
                modelIdentifier, inputRate, outputRate);

            incrementUnresolvedCounter(Reason.RATES_MISSING);

            return null;
        }

        // Defense against data-entry errors: NEGATIVE rates would silently produce a negative cost on
        // dashboards. Distinct from RATES_MISSING because the row exists, and explicitly NOT bucketed with
        // zero — a rate of exactly zero is the canonical encoding for free-tier / self-hosted models, and
        // suppressing it would cause those workspaces' dashboards to show "no cost data" instead of "$0".
        // Surface negatives as an explicit reason so a Grafana alert can trigger before a billing cycle
        // ships under-attributed.
        if (inputRate.signum() < 0 || outputRate.signum() < 0) {
            log.warn(
                "OTLP cost lookup found model '{}' but cost rates are negative (inputRate={}, outputRate={})",
                modelIdentifier, inputRate, outputRate);

            incrementUnresolvedCounter(Reason.RATES_INVALID);

            return null;
        }

        // Free-tier / self-hosted: BOTH rates exactly zero. Compute and persist BigDecimal.ZERO so dashboards
        // distinguish "$0 free-tier traffic" from "no cost data" (the null-return paths above).
        if (inputRate.signum() == 0 && outputRate.signum() == 0) {
            return BigDecimal.ZERO;
        }

        // Mixed-zero rates: one zero, one positive. Almost always a data-entry bug — the catalog row was
        // populated with only half its pricing. The computed cost is still real money (so we keep returning
        // it), but the PARTIAL_FREE_TIER counter lets operators alert when the rate of mixed-zero rows climbs
        // before a billing cycle ships under-attributed. The all-zero case above already handled the
        // intentional free-tier path so this counter never fires for legitimate $0 traffic.
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

    /**
     * Increments the unresolved-cost counter with the supplied reason tag. Wrapped in try/catch mirroring the
     * discipline used in the OTLP ingest facade: a meter-registry failure (cardinality cap, shutdown race, Prometheus
     * push-gateway hiccup) escaping out of this helper would propagate up the per-span persistence path and reject
     * otherwise-valid spans purely because the metrics layer is unhealthy. Counter failures are operationally noisy but
     * not data-correctness affecting; degrade gracefully so the cost-resolution hot path keeps running.
     */
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

    /**
     * Returns true when the supplied model id has not been logged in the dedup window. Uses {@link Map#compute} for
     * atomic check-and-set so concurrent ingestion threads do not double-log.
     *
     * <p>
     * The dedup map is bounded: when its size exceeds {@link #MODEL_DEDUP_MAP_SOFT_CAP}, entries older than the dedup
     * window are pruned, and if the map is still over cap after that the whole map is cleared. This prevents an
     * unbounded leak when a misbehaving exporter rotates UUID-prefixed model ids.
     *
     * <p>
     * The identity check {@code mappedValue == now} reads the result of {@link Map#compute}, which returns the NEW
     * mapped value (not the prior one — note the JDK's {@code compute} contract differs from the historical
     * "putIfAbsent returns the old value" pattern). When the lambda returned {@code now}, the identity match tells us
     * THIS thread won the dedup-gate race and is responsible for the warn log. When the lambda returned the stored
     * {@code lastLoggedAt}, identity mismatches and we suppress the log. The variable is named {@code mappedValue} (not
     * {@code previous}, which would read like a bug) so a future maintainer does not "fix" the comparison to
     * {@code Objects.equals(previous, now)} and break the dedup gate.
     */
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

        // Identity check (NOT equals): compute() returns the new mapping; reference-equality with `now` tells
        // us this thread's lambda returned `now` (i.e., we won the race). A different thread's matching
        // Instant value (.equals() true but != identity) would falsely report "we logged" if we used
        // Objects.equals — and the dedup gate would silently double-log.
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
            // Aged-out prune was insufficient (every entry is fresh). Clear the whole map; the worst case
            // is one re-log per misbehaving model after the clear, which is strictly better than a leak.
            modelNotRegisteredLastLoggedAt.clear();
        }
    }
}
