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
public class WorkflowChatMetrics {

    public static final String TURN_COUNTER = "bytechef_workflow_chat_turn";
    public static final String TURN_BY_WORKSPACE_COUNTER = "bytechef_workflow_chat_turn_by_workspace";
    public static final String RESUME_COUNTER = "bytechef_workflow_chat_resume";
    public static final String UNREACHABLE_COUNTER = "bytechef_workflow_chat_unreachable";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public WorkflowChatMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    /**
     * Records a per-turn invocation. Tag {@code outcome} discriminates the dispatch decision:
     * <ul>
     * <li>{@code sync}, workflow ran synchronously and returned a result.</li>
     * <li>{@code streaming}, workflow ran with the streaming executor.</li>
     * <li>{@code resume}, turn was a resume of a previously paused workflow.</li>
     * <li>{@code rate_limited}, admission rejected because two turns landed within the cooldown window. A spike in this
     * outcome usually points to a buggy client retrying on every keystroke or a user spamming Send.</li>
     * </ul>
     *
     * <p>
     * No workspace tag, workspace cardinality could explode the time-series for large multi-tenant deployments. Use
     * {@link #recordTurnByWorkspace(String, long)} for per-workspace tracking when the deployment has bounded workspace
     * counts (e.g. embedded iPaaS); the dual-counter setup lets ops choose the right granularity.
     * </p>
     */
    public void recordTurn(String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(TURN_COUNTER)
            .tag("outcome", outcome == null ? "unknown" : outcome)
            .register(meterRegistry)
            .increment();
    }

    public void recordTurnByWorkspace(String outcome, long workspaceId) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(TURN_BY_WORKSPACE_COUNTER)
            .tag("outcome", outcome == null ? "unknown" : outcome)
            .tag("workspace", Long.toString(workspaceId))
            .register(meterRegistry)
            .increment();
    }

    public void recordResume(String result) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(RESUME_COUNTER)
            .tag("result", result == null ? "unknown" : result)
            .register(meterRegistry)
            .increment();
    }

    public void recordUnreachable(String reason) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(UNREACHABLE_COUNTER)
            .tag("reason", reason == null ? "unknown" : reason)
            .register(meterRegistry)
            .increment();
    }
}
