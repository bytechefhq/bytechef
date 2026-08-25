/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Counts guardrail activity so operators can see what the DLP layer is catching. Emits a single
 * {@code bytechef_ai_guardrail} counter tagged by {@code event} — one of {@code pii_redacted}, {@code secret_redacted},
 * {@code response_redacted}, {@code blocked_term}, {@code moderation_flagged}, {@code injection_flagged}, or
 * {@code detector_failed} (a {@code SensitiveDataDetector} threw and was skipped for that call) — and by
 * {@code surface}, identifying which caller is applying guardrails (e.g. {@code gateway} for the AI Gateway adapter).
 * Only these two low-cardinality tags are used (no workspace/project dimension) so the meter stays cheap on unbounded
 * multi-tenant deployments. Wired through {@link ObjectProvider} so lightweight app variants without an actuator
 * {@link MeterRegistry} start cleanly and recording is a no-op.
 *
 * <p>
 * The {@code surface} is fixed per bean instance (constructor argument) rather than passed per {@link #record} call.
 * This Spring-wired constructor backs the engine's own internal instance (default surface {@code gateway}, gated on
 * {@code bytechef.ai.gateway.enabled} so it stays a no-op when the gateway is disabled) — used ONLY by
 * {@code AiGuardrails#applyToInputs}, the AI Gateway adapter's throwing entry point, whose request-direction
 * redaction/blocking events therefore stay tagged {@code gateway} (or emit nothing when the gateway is off) regardless
 * of which caller triggered them. Every other caller of the engine constructs its own instance per request via the
 * other constructor, tagged with its own surface (e.g. {@code ai_agent}, {@code ai_hub}): {@code
 * AiGuardrailsAdvisorProviderImpl} and {@code AiHubSpringAIAgent} build the instance that {@code AiGuardrailsAdvisor}
 * then passes into {@code AiGuardrails#checkInputs} for every request-direction event (redaction, blocked terms,
 * injection) as well as the advisor-decided {@code blocking_downgraded} / {@code response_redacted} events — so the
 * advisor path carries an accurate, non-gateway-gated surface tag end to end.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGuardrailMetrics {

    public static final String COUNTER_NAME = "bytechef_ai_guardrail";

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private final @Nullable MeterRegistry meterRegistry;
    private final String surface;

    // Two constructors are declared, so Spring cannot pick an autowire candidate implicitly and would fall back to a
    // (non-existent) default constructor. @Autowired marks this one as the container's entry point.
    @Autowired
    public AiGuardrailMetrics(
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        @Value("${bytechef.ai.guardrails.surface:gateway}") String surface) {

        this(meterRegistryProvider.getIfAvailable(), surface);
    }

    // Public (not package-private like the pre-extraction AiGatewayGuardrailMetrics) so callers outside this module —
    // notably the AI Gateway adapter's own tests, which build this engine's beans directly with a raw MeterRegistry —
    // can construct an instance without going through Spring.
    public AiGuardrailMetrics(@Nullable MeterRegistry meterRegistry, String surface) {
        this.meterRegistry = meterRegistry;
        this.surface = surface;
    }

    /**
     * Increments the guardrail counter for the given {@code event}, tagged with this instance's configured
     * {@code surface}. No-op when no {@link MeterRegistry} is present.
     *
     * @param event one of the documented event names
     */
    public void record(String event) {
        if (meterRegistry == null) {
            return;
        }

        Counter.builder(COUNTER_NAME)
            .description("AI guardrail actions by event type and surface")
            .tag("event", event)
            .tag("surface", surface)
            .register(meterRegistry)
            .increment();
    }
}
