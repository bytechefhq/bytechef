/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.metrics;

import com.bytechef.ee.automation.ai.gateway.event.AiGatewayBudgetExceededEvent;
import com.bytechef.ee.automation.ai.gateway.event.AiGatewayTraceCompletedEvent;
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
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayMetrics {

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private final MeterRegistry meterRegistry;

    public AiGatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

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

    @EventListener
    public void onBudgetExceeded(AiGatewayBudgetExceededEvent event) {
        Counter.builder("ai_gateway.budget.exceeded")
            .tag("model", event.model() != null ? event.model() : "unknown")
            .description("Count of requests rejected by pre-request budget enforcement")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Called from {@code AiGatewayRateLimitChecker} when a rate-limit rule rejects a request. Keeping the Micrometer
     * import out of the business-logic class — services call this method instead of building counters themselves.
     */
    public void incrementRateLimitRejection(String rateLimitName) {
        Counter.builder("ai_gateway.rate_limit.rejections")
            .tag("rate_limit", rateLimitName != null ? rateLimitName : "unknown")
            .description("Count of requests rejected by rate-limit rules")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Called from {@code AiObservabilityAlertEvaluator} when a rule threshold is breached and an alert event is
     * created. RESOLVED transitions are not counted — a RESOLVED implies a prior TRIGGERED was already counted.
     */
    public void incrementAlertTriggered(String metric) {
        Counter.builder("ai_gateway.alert.triggered")
            .tag("metric", metric != null ? metric : "unknown")
            .description("Count of alert rules that fired (status=TRIGGERED)")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Called from {@code AiObservabilityExportExecutor} when an export job finishes (or fails). Records wall-clock
     * duration from PENDING-flip to terminal state, tagged by scope and outcome.
     */
    public void recordExportDuration(String scope, String outcome, long durationMs) {
        Timer.builder("ai_gateway.export.duration")
            .tag("scope", scope != null ? scope : "unknown")
            .tag("outcome", outcome != null ? outcome : "unknown")
            .description("Duration of observability export jobs from start to terminal state, milliseconds")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Called from {@code AiObservabilityWebhookDeliveryServiceImpl} after each delivery attempt. Tagged with event type
     * and success flag so operators can alert on webhook-delivery error rate.
     */
    public void incrementWebhookDelivery(String eventType, boolean success) {
        Counter.builder("ai_gateway.webhook.delivery")
            .tag("event_type", eventType != null ? eventType : "unknown")
            .tag("status", success ? "success" : "failure")
            .description("Count of webhook delivery attempts")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Called from {@code AiGatewayFacade.enforcePostRequestBudget} when recording spend-and-enforce fails. Without
     * this, a DB/transport outage silently converts HARD budget mode into SOFT from that moment until cache TTL, and no
     * one is paged. The counter gives operators a metric to alert on.
     */
    public void incrementPostRequestBudgetFailure() {
        Counter.builder("ai_gateway.budget.post_request_failure")
            .description("Count of post-request budget enforcement failures (HARD mode silently degrades on failure)")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Called from {@code AiObservabilityDataCleanupService.cleanup} when per-workspace cleanup throws. Without this, a
     * permanently misconfigured workspace silently fails the 3 AM cron forever; the counter gives operators an
     * alertable signal instead of buried log lines.
     */
    public void incrementCleanupFailure() {
        Counter.builder("ai_gateway.observability.cleanup_failure")
            .description("Count of AI observability per-workspace cleanup failures (3 AM cron)")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Called from {@code AiGatewayFacade} when persisting the per-request cost log fails. Each drop means the request's
     * cost is missing from spend tracking, so budget math silently drifts below reality until the outage ends. Tagged
     * by {@code kind} (chat/streaming/embedding/routing) and {@code outcome} (success/error) so operators can
     * distinguish error-path logging failures (harmless for billing) from success-path logging failures (billing
     * drift).
     */
    public void incrementRequestLogPersistFailure(String kind, String outcome) {
        Counter.builder("ai_gateway.request_log.persist_failure")
            .tag("kind", kind != null ? kind : "unknown")
            .tag("outcome", outcome != null ? outcome : "unknown")
            .description("Count of AiGatewayRequestLog persistence failures (spend tracking silently drifts)")
            .register(meterRegistry)
            .increment();
    }
}
