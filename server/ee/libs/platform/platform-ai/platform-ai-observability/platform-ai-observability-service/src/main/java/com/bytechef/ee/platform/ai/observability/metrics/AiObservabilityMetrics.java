/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.metrics;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Micrometer counters/timers for AI observability subsystems — alerting and export jobs. Sibling to
 * {@code AiGatewayMetrics} (which owns gateway-traffic and budget metrics that listen for gateway events). Tags are
 * intentionally low-cardinality (metric / scope / outcome); workspace ID is omitted to avoid cardinality explosion —
 * use logs or traces for per-workspace breakdowns.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class AiObservabilityMetrics {

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private final MeterRegistry meterRegistry;

    public AiObservabilityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Increment when an alert rule fires (status=TRIGGERED). Called from {@code AiObservabilityAlertEvaluator}'s
     * trigger path so dashboards can show alert volume by metric type without scanning the alert_event table.
     */
    public void incrementAlertTriggered(String metric) {
        Counter.builder("ai_observability.alert.triggered")
            .tag("metric", metric != null ? metric : "unknown")
            .description("Count of alert rules that fired (status=TRIGGERED)")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record an export job's wall-clock duration from start to terminal state. {@code outcome} should be {@code
     * "success"} or {@code "failed"} (or another short label) — operators distinguish slow-but-completing exports from
     * fast-failing ones via the outcome tag.
     */
    public void recordExportDuration(String scope, String outcome, long durationMs) {
        Timer.builder("ai_observability.export.duration")
            .tag("scope", scope != null ? scope : "unknown")
            .tag("outcome", outcome != null ? outcome : "unknown")
            .description("Duration of observability export jobs from start to terminal state, milliseconds")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
