/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGuardrailMetrics implements SensitiveDataMetrics {

    public static final String COUNTER_NAME = "bytechef_ai_guardrail";

    private static final String DETECTOR_FAILED_EVENT = "detector_failed";
    private static final String DETECTOR_TIMED_OUT_EVENT = "detector_timed_out";
    private static final String DETECTOR_SKIPPED_OVERSIZE_EVENT = "detector_skipped_oversize";
    private static final String BELOW_CONFIDENCE_THRESHOLD_EVENT = "below_confidence_threshold";
    private static final String TOOL_ARGS_RESTORED_EVENT = "tool_args_restored";
    private static final String TOOL_RESULT_TOKENIZED_EVENT = "tool_result_tokenized";
    private static final String TOKEN_UNRESOLVED_EVENT = "token_unresolved";
    private static final String ASSISTANT_HISTORY_RETOKENIZED_EVENT = "assistant_history_retokenized";
    private static final String RESTORE_SUPPRESSED_EVENT = "restore_suppressed";

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private final @Nullable MeterRegistry meterRegistry;
    private final String surface;

    @Autowired
    public AiGuardrailMetrics(
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        @Value("${bytechef.ai.guardrails.surface:gateway}") String surface) {

        this(meterRegistryProvider.getIfAvailable(), surface);
    }

    public AiGuardrailMetrics(@Nullable MeterRegistry meterRegistry, String surface) {
        this.meterRegistry = meterRegistry;
        this.surface = surface;
    }

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

    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public void recordDetectorFailure(String detectorName) {
        record(DETECTOR_FAILED_EVENT);
    }

    public String getSurface() {
        return surface;
    }

    @Override
    public void recordDetectorTimedOut(String detectorName) {
        record(DETECTOR_TIMED_OUT_EVENT);
    }

    @Override
    public void recordDetectorSkippedOversize(String detectorName) {
        record(DETECTOR_SKIPPED_OVERSIZE_EVENT);
    }

    @Override
    public void recordBelowConfidenceThreshold() {
        record(BELOW_CONFIDENCE_THRESHOLD_EVENT);
    }

    @Override
    public void recordToolArgsRestored() {
        record(TOOL_ARGS_RESTORED_EVENT);
    }

    @Override
    public void recordToolResultTokenized() {
        record(TOOL_RESULT_TOKENIZED_EVENT);
    }

    @Override
    public void recordTokenUnresolved() {
        record(TOKEN_UNRESOLVED_EVENT);
    }

    @Override
    public void recordAssistantHistoryRetokenized() {
        record(ASSISTANT_HISTORY_RETOKENIZED_EVENT);
    }

    @Override
    public void recordRestoreSuppressed() {
        record(RESTORE_SUPPRESSED_EVENT);
    }
}
