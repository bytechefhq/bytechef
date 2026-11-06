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

    public void incrementAlertTriggered(String metric) {
        Counter.builder("ai_observability.alert.triggered")
            .tag("metric", metric != null ? metric : "unknown")
            .description("Count of alert rules that fired (status=TRIGGERED)")
            .register(meterRegistry)
            .increment();
    }

    public void recordExportDuration(String scope, String outcome, long durationMs) {
        Timer.builder("ai_observability.export.duration")
            .tag("scope", scope != null ? scope : "unknown")
            .tag("outcome", outcome != null ? outcome : "unknown")
            .description("Duration of observability export jobs from start to terminal state, milliseconds")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
