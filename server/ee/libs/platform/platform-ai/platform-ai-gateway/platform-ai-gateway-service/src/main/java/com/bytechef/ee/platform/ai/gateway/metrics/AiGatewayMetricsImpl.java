/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.metrics;

import com.bytechef.ee.platform.ai.gateway.event.AiGatewayBudgetExceededEvent;
import com.bytechef.ee.platform.ai.gateway.event.AiGatewayTraceCompletedEvent;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Micrometer counters/timers for AI Gateway traffic. Tags are intentionally low-cardinality (model + status); workspace
 * ID is omitted from metric tags to avoid cardinality explosion — use logs or traces for per-workspace breakdowns.
 *
 * @author Ivica Cardic
 * @version ee
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
    @EventListener
    public void onTraceCompleted(AiGatewayTraceCompletedEvent event) {
        String model = event.modelName() != null ? event.modelName() : "unknown";
        String status = event.success() ? "success" : "error";

        Counter.builder("ai_gateway.trace.completed")
            .tag("model", model)
            .tag("status", status)
            .description("Count of AI Gateway traces reaching a terminal state")
            .register(meterRegistry)
            .increment();

        if (event.totalLatencyMs() != null) {
            Timer.builder("ai_gateway.trace.latency")
                .tag("model", model)
                .tag("status", status)
                .description("End-to-end trace latency, milliseconds")
                .register(meterRegistry)
                .record(event.totalLatencyMs(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    @EventListener
    public void onBudgetExceeded(AiGatewayBudgetExceededEvent event) {
        Counter.builder("ai_gateway.budget.exceeded")
            .tag("model", event.model() != null ? event.model() : "unknown")
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
    public void incrementWebhookDelivery(String eventType, boolean success) {
        Counter.builder("ai_gateway.webhook.delivery")
            .tag("event_type", eventType != null ? eventType : "unknown")
            .tag("status", success ? "success" : "failure")
            .description("Count of webhook delivery attempts")
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void incrementWebhookDispatchFailure(String eventType) {
        Counter.builder("ai_gateway.webhook.dispatch_failure")
            .tag("event_type", eventType != null ? eventType : "unknown")
            .description("Count of webhook event-listener swallow events (subscribers were not notified)")
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void incrementWebhookDeliveryRowDivergence() {
        Counter.builder("ai_gateway.webhook.delivery_row_divergence")
            .description("Count of webhook delivery rows that failed to persist (in-memory status drifts from DB)")
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void incrementWebhookSubscriptionEventsParseFailure() {
        Counter.builder("ai_gateway.webhook.subscription_events_parse_failure")
            .description("Count of subscription rows whose events JSON failed to parse (silently skipped)")
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
    public void incrementCleanupFailure() {
        Counter.builder("ai_gateway.observability.cleanup_failure")
            .description("Count of AI observability per-workspace cleanup failures (3 AM cron)")
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
