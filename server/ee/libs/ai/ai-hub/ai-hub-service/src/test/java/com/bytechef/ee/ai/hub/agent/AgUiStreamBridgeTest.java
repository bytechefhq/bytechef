/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.CustomEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunFinishedEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.event.TextMessageEndEvent;
import com.agui.core.event.TextMessageStartEvent;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Unit tests for {@link AgUiStreamBridge} focused on the protocol translation contract: webhook stream chunks /
 * ask_user_question events / completion / error get translated into the right AG-UI events, and the bridge captures
 * resume URLs into the registry on {@code ask_user_question} payloads.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AgUiStreamBridgeTest {

    private static final String MESSAGE_ID = "msg-1";
    private static final String THREAD_ID = "thread-1";
    private static final String RUN_ID = "run-1";
    private static final long TASK_ID = 42L;

    private AgentSubscriber subscriber;
    private WebhookResumeRegistry registry;
    private AgUiStreamBridge bridge;

    @BeforeEach
    void setUp() {
        subscriber = mock(AgentSubscriber.class);
        registry = new WebhookResumeRegistry(
            new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        WorkflowChatJobRegistry jobRegistry = new WorkflowChatJobRegistry(
            new ConcurrentMapCacheManager(WorkflowChatJobRegistry.CACHE_NAME));
        // Real guard backed by the in-memory cache manager. The bridge only invokes guard.release() on terminal
        // events — a mock would work here but the real guard exercises the same release-after-clear ordering the
        // dedicated WorkflowChatGuardTest verifies, giving these tests an integration-style check at no extra cost.
        WorkflowChatGuard guard = new WorkflowChatGuard(
            new ConcurrentMapCacheManager(
                WorkflowChatGuard.LAST_TURN_CACHE_NAME, WorkflowChatGuard.IN_FLIGHT_CACHE_NAME));

        bridge = new AgUiStreamBridge(
            subscriber, MESSAGE_ID, THREAD_ID, RUN_ID, TASK_ID, registry, jobRegistry, guard, null,
            () -> {});
    }

    @Test
    void testTextChunkEmitsStartAndContent() {
        bridge.onEvent("hello");

        verify(subscriber, times(1)).onTextMessageStartEvent(any(TextMessageStartEvent.class));

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber).onTextMessageContentEvent(contentCaptor.capture());

        assertThat(contentCaptor.getValue()
            .getDelta()).isEqualTo("hello");
    }

    @Test
    void testMultipleChunksFireStartOnlyOnce() {
        bridge.onEvent("hello ");
        bridge.onEvent("world");

        // The TextMessageStart event marks the beginning of a message — it must fire exactly once even
        // across many chunks. Otherwise the client renders multiple half-formed assistant messages.
        verify(subscriber, times(1)).onTextMessageStartEvent(any());
        verify(subscriber, times(2)).onTextMessageContentEvent(any());
    }

    @Test
    void testAskUserQuestionRegistersResumeUrlAndEmitsCustomEvent() {
        Map<String, Object> payload = Map.of(
            "ask_user_question", true,
            "questions", java.util.List.of("Which option?"),
            "resumeUrl", "https://example.com/resume/abc");

        bridge.onEvent(payload);

        // Resume URL must land in the registry so the next user turn can POST to it.
        assertThat(registry.consume(TASK_ID)).isEqualTo("https://example.com/resume/abc");

        ArgumentCaptor<CustomEvent> eventCaptor = ArgumentCaptor.forClass(CustomEvent.class);

        verify(subscriber).onCustomEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue()
            .getName()).isEqualTo(AgUiStreamBridge.ASK_WORKFLOW_QUESTION_EVENT_NAME);
        verify(subscriber, never()).onTextMessageStartEvent(any());
    }

    @Test
    void testAskUserQuestionWithoutResumeUrlStillEmitsCustomEvent() {
        Map<String, Object> payload = Map.of(
            "ask_user_question", true,
            "questions", java.util.List.of("Which option?"));

        bridge.onEvent(payload);

        // Without a resumeUrl the question still needs to render client-side — the user's answer just
        // starts a fresh execution rather than resuming. Don't drop the event silently.
        assertThat(registry.consume(TASK_ID)).isNull();

        verify(subscriber).onCustomEvent(any());
    }

    @Test
    void testUnrecognisedMapEmitsGenericCustomEvent() {
        Map<String, Object> payload = Map.of("some_other_key", "value");

        bridge.onEvent(payload);

        ArgumentCaptor<CustomEvent> eventCaptor = ArgumentCaptor.forClass(CustomEvent.class);

        verify(subscriber).onCustomEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue()
            .getName()).isEqualTo(AgUiStreamBridge.WORKFLOW_EVENT_NAME);
    }

    @Test
    void testOnCompleteAfterTextEmitsEndAndFinished() {
        bridge.onEvent("hi");
        bridge.onComplete();

        verify(subscriber).onTextMessageEndEvent(any(TextMessageEndEvent.class));
        verify(subscriber).onRunFinishedEvent(any(RunFinishedEvent.class));
    }

    @Test
    void testOnCompleteWithoutPriorTextSkipsTextEnd() {
        bridge.onComplete();

        // No text message was started, so emitting an end event would create a phantom message.
        verify(subscriber, never()).onTextMessageEndEvent(any());
        verify(subscriber).onRunFinishedEvent(any());
    }

    @Test
    void testCompletedFlagPreventsDuplicateFinish() {
        bridge.onComplete();
        bridge.onComplete();

        // Idempotency: the underlying executor's whenComplete chain can deliver completion twice
        // (stream-level + future-level). Without the guard, the client would see RUN_FINISHED twice.
        verify(subscriber, times(1)).onRunFinishedEvent(any());
    }

    @Test
    void testOnErrorEmitsRunErrorEventOnce() {
        bridge.onError(new IllegalStateException("workflow failed"));
        bridge.onError(new IllegalStateException("again"));

        // Idempotency on the error path mirrors complete — the executor can deliver both stream-level
        // and future-level errors.
        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, times(1)).onRunErrorEvent(errorCaptor.capture());

        assertThat(errorCaptor.getValue()
            .getError()).isEqualTo("workflow failed");
    }

    @Test
    void testEventsAfterCompleteAreDropped() {
        bridge.onComplete();
        bridge.onEvent("late chunk");

        verify(subscriber, never()).onTextMessageContentEvent(any());
        verify(subscriber, never()).onTextMessageStartEvent(any());
    }

    @Test
    void testOnErrorFiresJobFacadeStopJobWhenJobIdRegistered() {
        // WC #5b auto-cancel-on-disconnect: when a streaming workflow's run begins, the executor emits a
        // {event=start, payload.jobId=...} event the bridge captures into the job registry. If the bridge
        // later sees onError (workflow crashed mid-run, or the client disconnected and the SSE write failed),
        // the auto-cancel logic must fire JobFacade.stopJob(jobId) so the worker isn't left burning capacity
        // on output nobody will read.
        com.bytechef.atlas.execution.facade.JobFacade jobFacade =
            mock(com.bytechef.atlas.execution.facade.JobFacade.class);

        AgentSubscriber localSubscriber = mock(AgentSubscriber.class);
        WebhookResumeRegistry localRegistry = new WebhookResumeRegistry(
            new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        WorkflowChatJobRegistry localJobRegistry = new WorkflowChatJobRegistry(
            new ConcurrentMapCacheManager(WorkflowChatJobRegistry.CACHE_NAME));
        WorkflowChatGuard localGuard = new WorkflowChatGuard(
            new ConcurrentMapCacheManager(
                WorkflowChatGuard.LAST_TURN_CACHE_NAME, WorkflowChatGuard.IN_FLIGHT_CACHE_NAME));

        AgUiStreamBridge bridgeWithJobFacade = new AgUiStreamBridge(
            localSubscriber, MESSAGE_ID, THREAD_ID, RUN_ID, TASK_ID, localRegistry, localJobRegistry,
            localGuard, jobFacade, () -> {});

        // Simulate the executor's start event so the bridge populates the job registry.
        bridgeWithJobFacade.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 7777L)));

        bridgeWithJobFacade.onError(new RuntimeException("workflow failed"));

        // The cancel must fire — without it, a long-running workflow whose client disconnected keeps eating
        // worker capacity until the engine's own timeout elapses (sometimes minutes).
        verify(jobFacade, times(1)).stopJob(7777L);
    }

    @Test
    void testOnErrorSwallowsStopJobExceptionsSoRunErrorStillFires() {
        // The cancel is best-effort. A failure (job already finished, coordinator transient outage) must NOT
        // propagate further — the user's RUN_ERROR event still fires below regardless of stopJob's outcome.
        com.bytechef.atlas.execution.facade.JobFacade jobFacade =
            mock(com.bytechef.atlas.execution.facade.JobFacade.class);

        org.mockito.Mockito.doThrow(new RuntimeException("coordinator down"))
            .when(jobFacade)
            .stopJob(org.mockito.ArgumentMatchers.anyLong());

        AgentSubscriber localSubscriber = mock(AgentSubscriber.class);
        WebhookResumeRegistry localRegistry = new WebhookResumeRegistry(
            new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        WorkflowChatJobRegistry localJobRegistry = new WorkflowChatJobRegistry(
            new ConcurrentMapCacheManager(WorkflowChatJobRegistry.CACHE_NAME));
        WorkflowChatGuard localGuard = new WorkflowChatGuard(
            new ConcurrentMapCacheManager(
                WorkflowChatGuard.LAST_TURN_CACHE_NAME, WorkflowChatGuard.IN_FLIGHT_CACHE_NAME));

        AgUiStreamBridge bridgeWithFlakyJobFacade = new AgUiStreamBridge(
            localSubscriber, MESSAGE_ID, THREAD_ID, RUN_ID, TASK_ID, localRegistry, localJobRegistry,
            localGuard, jobFacade, () -> {});

        bridgeWithFlakyJobFacade.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 8888L)));

        // onError must NOT throw even when the cancel fails. Pin: subscriber still gets the RUN_ERROR event.
        bridgeWithFlakyJobFacade.onError(new RuntimeException("workflow failed"));

        verify(localSubscriber).onRunErrorEvent(any(RunErrorEvent.class));
    }

    @Test
    void testOnErrorSkipsStopJobWhenNoJobIdRegistered() {
        // No start event arrived (sync-mode workflow, or the start event was malformed and the registry was
        // never populated). The cancel path must skip stopJob entirely — calling it with a null/zero jobId
        // would either throw NPE or cancel an unrelated job.
        com.bytechef.atlas.execution.facade.JobFacade jobFacade =
            mock(com.bytechef.atlas.execution.facade.JobFacade.class);

        AgentSubscriber localSubscriber = mock(AgentSubscriber.class);
        WebhookResumeRegistry localRegistry = new WebhookResumeRegistry(
            new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        WorkflowChatJobRegistry localJobRegistry = new WorkflowChatJobRegistry(
            new ConcurrentMapCacheManager(WorkflowChatJobRegistry.CACHE_NAME));
        WorkflowChatGuard localGuard = new WorkflowChatGuard(
            new ConcurrentMapCacheManager(
                WorkflowChatGuard.LAST_TURN_CACHE_NAME, WorkflowChatGuard.IN_FLIGHT_CACHE_NAME));

        AgUiStreamBridge bridgeWithJobFacade = new AgUiStreamBridge(
            localSubscriber, MESSAGE_ID, THREAD_ID, RUN_ID, TASK_ID, localRegistry, localJobRegistry,
            localGuard, jobFacade, () -> {});

        // No prior start event — registry is empty.
        bridgeWithJobFacade.onError(new RuntimeException("workflow failed"));

        verify(jobFacade, never()).stopJob(org.mockito.ArgumentMatchers.anyLong());
    }
}
