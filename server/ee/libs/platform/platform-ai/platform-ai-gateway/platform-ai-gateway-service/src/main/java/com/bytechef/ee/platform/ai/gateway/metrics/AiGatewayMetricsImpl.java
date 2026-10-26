/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.metrics;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayMetricsImpl implements AiGatewayMetrics {

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private final MeterRegistry meterRegistry;

    public AiGatewayMetricsImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void incrementBudgetExceeded(String model) {
        Counter.builder("ai_gateway.budget.exceeded")
            .tag("model", model != null ? model : "unknown")
            .description("Count of requests rejected by pre-request budget enforcement")
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void incrementRateLimitRejection(String rateLimitName) {
        Counter.builder("ai_gateway.rate_limit.rejections")
            .tag("rate_limit", rateLimitName != null ? rateLimitName : "unknown")
            .description("Count of requests rejected by rate-limit rules")
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void incrementPostRequestBudgetFailure() {
        Counter.builder("ai_gateway.budget.post_request_failure")
            .description("Count of post-request budget enforcement failures (HARD mode silently degrades on failure)")
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void incrementRequestLogPersistFailure(String kind, String outcome) {
        Counter.builder("ai_gateway.request_log.persist_failure")
            .tag("kind", kind != null ? kind : "unknown")
            .tag("outcome", outcome != null ? outcome : "unknown")
            .description("Count of AiLlmUsage persistence failures (spend tracking silently drifts)")
            .register(meterRegistry)
            .increment();
    }
}
