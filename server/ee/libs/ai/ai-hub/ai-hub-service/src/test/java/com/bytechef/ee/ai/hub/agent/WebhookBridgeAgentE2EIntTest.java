/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * WC #13 — End-to-end integration test skeleton for {@link WebhookBridgeAgent}. Disabled by default; the user runs it
 * manually because it requires the full coordinator + worker + webhook stack standing up via Testcontainers, which is
 * far slower than the unit-test surface and has its own CI-only environment.
 *
 * <p>
 * What this test should pin (when enabled):
 * </p>
 * <ol>
 * <li><b>Sync execution path:</b> a workflow with a {@code newChatRequest} trigger and {@code workflowSyncExecution
 * = true} returns its result in one event-batch. The bridge translates the result map's {@code message} field into a
 * single TextMessageContent + RunFinished pair on the AG-UI subscriber. This is the most-trafficked path in production
 * and a regression here would silently break every chat-enabled workflow.</li>
 *
 * <li><b>Streaming execution path:</b> a workflow with {@code workflowSyncExecution = false} streams chunks through the
 * bridge as the workflow runs. The bridge translates each chunk into a TextMessageContent delta and the subscriber
 * receives them in arrival order. Verifies the {@code event=start, payload.jobId=...} branch of
 * {@link com.bytechef.ee.ai.hub.agent.AgUiStreamBridge#onEvent} actually populates
 * {@link com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry} so cancel-turn can resolve back to the job id.</li>
 *
 * <li><b>ask-user-question pause + resume:</b> a workflow that emits {@code ask_user_question} pauses, the bridge
 * registers the resume URL, and the next user turn POSTs the answer to the registered URL rather than starting a fresh
 * execution. Verifies {@link com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry#consume} is atomically
 * remove-and-return so a duplicate delivery of the same turn doesn't fire the resume POST twice.</li>
 *
 * <li><b>WC #5b auto-cancel-on-disconnect:</b> when the AG-UI client disconnects mid-stream, the bridge's
 * {@code onError} hook fires, which now also calls {@code JobFacade.stopJob(jobId)}. Verify by injecting a slow
 * workflow, dropping the subscriber connection, and asserting the job state in the coordinator transitions to STOPPED
 * rather than continuing to RUNNING.</li>
 *
 * <li><b>WC #8 streaming-resume:</b> a workflow that emits {@code text/event-stream} on resume should stream chunks
 * through the bridge as they arrive (rather than buffering). Verify by hitting a resume endpoint that emits one chunk
 * per second over 5 seconds and asserting the bridge emits 5 separate TextMessageContent events with inter-event
 * timing.</li>
 *
 * <li><b>WC #9 + #10 admission gate:</b> with the rate limit + concurrency enabled (the default), a back-to-back second
 * turn arriving within the cooldown window or while the first is still running gets a RUN_ERROR with the guard's
 * user-facing message. Mirrors the existing {@code WorkflowChatGuardTest} but exercises the gate against a real bridge
 * instead of a mock.</li>
 *
 * <li><b>Attachment promotion:</b> a turn carrying a base64 attachment lands as a first-class workspace asset file via
 * {@code AssetFileFacade.createFromUpload}, and the workflow trigger sees it as a {@code FileEntry} in the file-list
 * discriminator branch. Pin the round-trip: the file appears in the workspace's Files panel after the turn finishes,
 * browsable through {@code ListAssetFilesToolCallback}.</li>
 *
 * <li><b>Telemetry:</b> after running the above, the {@code bytechef_workflow_chat_turn} counter has the expected tag
 * combinations ({@code outcome=sync}, {@code streaming}, {@code resume}, {@code rate_limited},
 * {@code concurrency_blocked}). Catches metric-name typos that would otherwise only surface in production
 * dashboards.</li>
 * </ol>
 *
 * <p>
 * <b>Why disabled:</b> the test needs the platform-webhook coordinator + worker stack and a Postgres+Redis + embedded
 * workflow runtime, which spins up in ~2 minutes per fixture. Running it on every CI build would slow the pipeline by
 * an order of magnitude. The user runs it locally before each EE release; the CI gate is the unit-test layer
 * ({@link WebhookBridgeAgentTest}, {@link WebhookBridgeAgentResumeTest}) which already covers the dispatch logic with
 * mocks.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Disabled("WC #13 — manual run only. Requires full coordinator + worker + webhook + Redis stack.")
public class WebhookBridgeAgentE2EIntTest {

    @Test
    @Disabled
    public void testSyncWorkflowReturnsResultMessage() {
        // TODO(WC #13): wire up Testcontainers, register a workflowSyncExecution=true workflow, run a turn,
        // assert the AG-UI subscriber receives TextMessageContent with the workflow's result message.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testStreamingWorkflowEmitsDeltasInOrder() {
        // TODO(WC #13): same setup, workflowSyncExecution=false, assert N deltas arrive in order with sub-second
        // inter-event timing.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testAskUserQuestionRoundTrip() {
        // TODO(WC #13): workflow emits ask_user_question, bridge captures resumeUrl, next turn POSTs answer to
        // resumeUrl, workflow resumes with the answer in scope.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testAutoCancelOnDisconnect() {
        // TODO(WC #5b + #13): start a long-running workflow, drop the subscriber, assert JobFacade.stopJob fires
        // and the coordinator's job state transitions to STOPPED.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testStreamingResumeEmitsChunksAsTheyArrive() {
        // TODO(WC #8 + #13): resume endpoint emits text/event-stream, bridge surfaces each chunk to the subscriber
        // before the response stream closes.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testAdmissionGateRefusesBackToBackTurns() {
        // TODO(WC #9 + #10 + #13): two turns within the cooldown — second gets RUN_ERROR with the rate-limit
        // message; turn arriving while another is in flight gets the concurrency message.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testAttachmentPromotionRoundTrip() {
        // TODO(WC #13): attach a base64 file via forwardedProps, assert the workflow trigger sees it as a
        // FileEntry, AssetFileFacade.createFromUpload was called, file is browsable in the Files panel.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    public void testTelemetryCountersMatchDispatchedTurns() {
        // TODO(WC #13): exercise sync + streaming + resume + rate_limited + concurrency_blocked in sequence,
        // assert the bytechef_workflow_chat_turn counter has exactly the expected tag combinations.
        throw new UnsupportedOperationException("not implemented");
    }
}
