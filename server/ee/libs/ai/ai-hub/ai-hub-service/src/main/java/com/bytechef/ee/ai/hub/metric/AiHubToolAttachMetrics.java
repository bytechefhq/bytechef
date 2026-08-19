/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.metric;

import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Counters for the autonomous tool-attach flow on the main AI Hub agents. Without these, ops has no visibility into
 * whether the agent is actually exercising the discovery callbacks ({@code searchComponents},
 * {@code listComponentActions}, {@code describeComponentAction}), how often the attach itself fires, how often the
 * agent falls back to {@code askUserQuestion} for disambiguation, or whether truncation in {@code searchComponents} is
 * hitting in practice (a signal the agent's queries are too broad).
 *
 * <p>
 * Wired via {@link ObjectProvider}{@code <MeterRegistry>} so app variants without actuator start cleanly — every record
 * method is a no-op when no {@code MeterRegistry} is available. Mirrors {@link WorkflowChatMetrics}' dual-counter
 * pattern but does NOT include a per-workspace tag: the autonomous-attach flow is bursty (many tool calls per turn) and
 * per-workspace cardinality would dominate the time series. If per-workspace tracking becomes useful later, add a
 * parallel counter as {@code WorkflowChatMetrics} does.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AiHubToolAttachMetrics implements ToolStateVisibilityMetrics {

    public static final String DISCOVERY_COUNTER = "bytechef_ai_hub_tool_attach_discovery";
    public static final String STATE_VISIBILITY_COUNTER = "bytechef_ai_hub_tool_attach_state_visibility";
    public static final String ATTACH_COUNTER = "bytechef_ai_hub_tool_attach";
    public static final String ASK_USER_QUESTION_COUNTER = "bytechef_ai_hub_ask_user_question";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public AiHubToolAttachMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    /**
     * Records a discovery-callback invocation. Tag {@code tool} discriminates between {@code searchComponents},
     * {@code listComponentActions}, {@code describeComponentAction}; tag {@code outcome} discriminates between
     * {@code success}, {@code truncated} (results exceeded the caller's limit — only meaningful for
     * {@code searchComponents}), {@code empty} (no matches), {@code error}.
     */
    public void recordDiscovery(String tool, String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(DISCOVERY_COUNTER)
            .tag("tool", tool == null ? "unknown" : tool)
            .tag("outcome", outcome == null ? "unknown" : outcome)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records a state-visibility callback invocation ({@code listChatTools}, {@code listConnectionsForComponent},
     * {@code lookupActionPropertyOptions}, {@code lookupTriggerPropertyOptions}). Tag {@code tool} discriminates; tag
     * {@code outcome} is one of {@code success}, {@code empty}, {@code error}, plus the lookup-specific outcomes
     * {@code connection_required}, {@code dependency_missing}, {@code no_options_for_property}. {@code empty} on
     * {@code listConnectionsForComponent} is the signal that drives the agent to fall back to {@code createConnection},
     * so a spike there indicates users connecting integrations for the first time. Spikes in
     * {@code connection_required} on the lookup tools indicate agents skipping the listConnections step.
     */
    @Override
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

    /**
     * Records an {@code attachChatTool} invocation. Tag {@code outcome}:
     * <ul>
     * <li>{@code new_component} — fresh attach, no prior binding for this (chat, component, version, env) tuple.</li>
     * <li>{@code rebound_connection} — hardened-attach rebind path: existing binding found, connection updated in
     * place. A spike here means the autonomous flow is doing what it's designed to (null-then-real) — useful signal
     * that the rebind is being exercised vs. dead code.</li>
     * <li>{@code connection_environment_mismatch} — defensive guard fired: the LLM-supplied {@code connectionId}
     * resolved to a connection in a different environment than the chat's chat. Should be near-zero in production
     * (listConnectionsForComponent filters by env); a spike points to LLM hallucinating connection ids from prior
     * cross-env chat memory.</li>
     * <li>{@code error} — facade or context failure.</li>
     * </ul>
     */
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

    /**
     * Records an {@code askUserQuestion} invocation. Tag {@code outcome}: {@code success} (questions captured and
     * envelope emitted), {@code empty} (library accepted input but produced zero questions), {@code error}.
     */
    @Override
    public void recordAskUserQuestion(String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(ASK_USER_QUESTION_COUNTER)
            .tag("outcome", outcome == null ? "unknown" : outcome)
            .register(meterRegistry)
            .increment();
    }
}
