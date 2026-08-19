/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Counters for workflow-chat bridge events. Without these, ops has no visibility into:
 * <ul>
 * <li>How many turns flow through the bridge vs. the LLM agent (bytechef_workflow_chat_turn).</li>
 * <li>Whether resume-flow is hitting in production at all (bytechef_workflow_chat_resume).</li>
 * <li>Disabled-workflow / deleted-workflow rates per workspace (bytechef_workflow_chat_unreachable).</li>
 * </ul>
 *
 * <p>
 * Wired via {@link ObjectProvider}{@code <MeterRegistry>} so app variants without actuator (the lightweight
 * runtime-job-app pattern in CLAUDE.md) start cleanly — every record method is a no-op when no MeterRegistry is
 * available, mirroring {@code AssetFileMetrics} / {@code bytechef_connection_create}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowChatMetrics {

    public static final String TURN_COUNTER = "bytechef_workflow_chat_turn";
    public static final String TURN_BY_WORKSPACE_COUNTER = "bytechef_workflow_chat_turn_by_workspace";
    public static final String RESUME_COUNTER = "bytechef_workflow_chat_resume";
    public static final String UNREACHABLE_COUNTER = "bytechef_workflow_chat_unreachable";
    public static final String ATTACHMENT_FAILURE_COUNTER = "bytechef_workflow_chat_attachment_failure";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public WorkflowChatMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    /**
     * Records a per-turn bridge invocation. Tag {@code outcome} discriminates the dispatch decision:
     * <ul>
     * <li>{@code sync} — workflow ran synchronously and returned a result.</li>
     * <li>{@code streaming} — workflow ran with the streaming executor.</li>
     * <li>{@code resume} — turn was a resume of a previously paused workflow.</li>
     * <li>{@code rate_limited} — admission rejected because two turns landed within the cooldown window. A spike in
     * this outcome usually points to a buggy client retrying on every keystroke or a user spamming Send.</li>
     * <li>{@code concurrency_blocked} — admission rejected because a previous turn was still in flight. Spikes here
     * suggest workflows whose first step takes long enough that users assume the message was lost and click again.</li>
     * </ul>
     *
     * <p>
     * No workspace tag — workspace cardinality could explode the time-series for large multi-tenant deployments. Use
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

    /**
     * Records a per-turn invocation tagged by workspace id. Use for deployments with bounded workspace counts
     * (single-tenant SaaS, embedded iPaaS) where ops needs the per-workspace breakdown to attribute usage or detect a
     * runaway workspace. For unbounded multi-tenant deployments prefer {@link #recordTurn(String)} alone — the
     * workspace tag's cardinality would otherwise hurt the metrics backend.
     *
     * <p>
     * Counter name {@link #TURN_BY_WORKSPACE_COUNTER} is distinct from {@link #TURN_COUNTER} so the two can be enabled
     * independently. {@code outcome} mirrors the values documented on {@link #recordTurn(String)}.
     * </p>
     */
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

    /**
     * Records whether a resume HTTP POST succeeded. Tag {@code result} is one of {@code success}, {@code http_error}
     * (non-2xx), or {@code transport_error} (timeout / connection-refused / interrupted).
     */
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

    /**
     * Records a turn where the workflow could not be reached. Tag {@code reason} is one of {@code disabled} (the
     * upfront isWorkflowDisabled returned true) or {@code deleted} (the trigger lookup threw, indicating the workflow /
     * project deployment / trigger was deleted out from under the chat). Useful for spotting workspaces with stale
     * workflow chats so the chat can be archived or the workflow restored.
     */
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

    /**
     * Records a workflow-chat attachment that couldn't be promoted to a workspace asset file. Tag {@code reason}
     * categorises the failure: {@code missing_payload} (no base64), {@code too_large} (rejected by the bridge's size
     * cap before upload), {@code malformed} (non-Map shape on the wire), {@code upload_failure} (storage or quota error
     * from AssetFileFacade), {@code decode_failure} (Base64 decoder rejected the string). Lets ops spot a flaky storage
     * backend or a client serializing attachments wrong before users complain.
     */
    public void recordAttachmentFailure(String reason) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(ATTACHMENT_FAILURE_COUNTER)
            .tag("reason", reason == null ? "unknown" : reason)
            .register(meterRegistry)
            .increment();
    }
}
