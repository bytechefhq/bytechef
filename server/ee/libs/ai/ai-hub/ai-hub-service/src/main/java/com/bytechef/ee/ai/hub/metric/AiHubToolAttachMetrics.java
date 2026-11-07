/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.metric;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubToolAttachMetrics {

    public static final String STATE_VISIBILITY_COUNTER = "bytechef_ai_hub_tool_attach_state_visibility";
    public static final String ATTACH_COUNTER = "bytechef_ai_hub_tool_attach";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public AiHubToolAttachMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    public void recordStateVisibility(String tool, String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(STATE_VISIBILITY_COUNTER)
            .tag("tool", tool == null ? "unknown" : tool)
            .tag("outcome", outcome == null ? "unknown" : outcome)
            .register(meterRegistry)
            .increment();
    }

    public void recordAttach(String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(ATTACH_COUNTER)
            .tag("outcome", outcome == null ? "unknown" : outcome)
            .register(meterRegistry)
            .increment();
    }
}
