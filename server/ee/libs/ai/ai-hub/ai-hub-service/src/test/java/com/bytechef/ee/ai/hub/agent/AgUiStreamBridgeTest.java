/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.CustomEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunFinishedEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.event.TextMessageEndEvent;
import com.agui.core.event.TextMessageStartEvent;
import com.agui.core.event.ToolCallEndEvent;
import com.agui.core.event.ToolCallStartEvent;
import com.bytechef.atlas.execution.facade.JobFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Unit tests for {@link AgUiStreamBridge} focused on the protocol translation contract: webhook stream chunks,
 * ask_user_question events, completion and errors get translated into the right AG-UI events, and the bridge captures
 * resume URLs and job ids into their registries.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AgUiStreamBridgeTest {

    private static final String MESSAGE_ID = "msg-1";
    private static final String THREAD_ID = "thread-1";
    private static final String RUN_ID = "run-1";
    private static final long CHAT_ID = 42L;

    private AgentSubscriber subscriber;
    private WebhookResumeRegistry registry;
    private WorkflowChatJobRegistry jobRegistry;
    private AtomicInteger finalizeCount;
    private AgUiStreamBridge bridge;

    @BeforeEach
    void setUp() {
        subscriber = mock(AgentSubscriber.class);
        registry = new WebhookResumeRegistry(new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        jobRegistry = spy(
            new WorkflowChatJobRegistry(new ConcurrentMapCacheManager(WorkflowChatJobRegistry.CACHE_NAME)));
        finalizeCount = new AtomicInteger();

        bridge = newBridge(jobId -> {});
    }

    @Test
    void testTextChunkEmitsStartAndContent() {
        bridge.onEvent("hello");

        verify(subscriber, times(1)).onTextMessageStartEvent(any(TextMessageStartEvent.class));

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber).onTextMessageContentEvent(contentCaptor.capture());

        TextMessageContentEvent contentEvent = contentCaptor.getValue();

        assertThat(contentEvent.getDelta()).isEqualTo("hello");
    }

    @Test
    void testMultipleChunksFireStartOnlyOnce() {
        bridge.onEvent("hello ");
        bridge.onEvent("world");

        verify(subscriber, times(1)).onTextMessageStartEvent(any());
        verify(subscriber, times(2)).onTextMessageContentEvent(any());
        assertThat(bridge.getAccumulatedAssistantText()).isEqualTo("hello world");
    }

    @Test
    void testStartEventReportsTheJobId() {
        AtomicLong seen = new AtomicLong();
        AgUiStreamBridge bridge = newBridge(seen::set);

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", "77")));

        assertThat(seen.get()).isEqualTo(77L);
        verify(jobRegistry).register(CHAT_ID, 77L);
        verify(subscriber, never()).onEvent(any());
    }

    @Test
    void testOnlyTheFirstStartEventReportsTheJobId() {
        List<Long> seenJobIds = new ArrayList<>();
        AgUiStreamBridge bridge = newBridge(seenJobIds::add);

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 11L)));
        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 12L)));

        assertThat(seenJobIds).containsExactly(11L);
        verify(jobRegistry, times(1)).register(anyLong(), anyLong());
        assertThat(jobRegistry.get(CHAT_ID)).isEqualTo(11L);
    }

    @Test
    void testStartEventWithUnparsableJobIdReportsNothing() {
        AtomicInteger calls = new AtomicInteger();
        AgUiStreamBridge bridge = newBridge(jobId -> calls.incrementAndGet());

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", "not-a-number")));

        assertThat(calls.get()).isZero();
        verify(jobRegistry, never()).register(anyLong(), anyLong());
    }

    @Test
    void testAskUserQuestionRegistersResumeUrlAndEmitsCustomEvent() {
        Map<String, Object> payload = Map.of(
            "ask_user_question", true,
            "questions", List.of("Which option?"),
            "resumeUrl", "https://example.com/resume/abc");

        bridge.onEvent(payload);

        assertThat(registry.consume(CHAT_ID)).isEqualTo("https://example.com/resume/abc");

        ArgumentCaptor<CustomEvent> eventCaptor = ArgumentCaptor.forClass(CustomEvent.class);

        verify(subscriber).onCustomEvent(eventCaptor.capture());

        CustomEvent customEvent = eventCaptor.getValue();

        assertThat(customEvent.getName()).isEqualTo(AgUiStreamBridge.ASK_WORKFLOW_QUESTION_EVENT_NAME);
        verify(subscriber, never()).onTextMessageStartEvent(any());
        verify(subscriber, never()).onRunErrorEvent(any());
        assertThat(finalizeCount.get()).isZero();
    }

    @Test
    void testAskUserQuestionWithoutResumeUrlShowsTheQuestionAndRegistersNoResume() {
        JobFacade jobFacade = mock(JobFacade.class);
        AgUiStreamBridge bridge = newBridge(jobFacade, jobId -> {});

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 9L)));
        bridge.onEvent(
            Map.of("__eventType", "ask_user_question", "questions", List.of(Map.of("question", "Which account?"))));

        ArgumentCaptor<CustomEvent> eventCaptor = ArgumentCaptor.forClass(CustomEvent.class);

        verify(subscriber).onCustomEvent(eventCaptor.capture());

        CustomEvent customEvent = eventCaptor.getValue();

        assertThat(customEvent.getName()).isEqualTo(AgUiStreamBridge.ASK_WORKFLOW_QUESTION_EVENT_NAME);
        assertThat(registry.consume(CHAT_ID)).isNull();
        verify(subscriber, never()).onRunErrorEvent(any());
        verify(jobFacade, never()).stopJob(anyLong());
        assertThat(finalizeCount.get()).isZero();

        bridge.onComplete();

        verify(subscriber).onRunFinishedEvent(any());
        verify(jobFacade, never()).stopJob(anyLong());
        assertThat(finalizeCount.get()).isEqualTo(1);
    }

    @Test
    void testAiAgentSseEventPassesThroughWithoutItsTypeKey() {
        bridge.onEvent(Map.of("__eventType", "tool_execution", "toolName", "search"));

        ArgumentCaptor<CustomEvent> eventCaptor = ArgumentCaptor.forClass(CustomEvent.class);

        verify(subscriber).onCustomEvent(eventCaptor.capture());

        CustomEvent customEvent = eventCaptor.getValue();

        assertThat(customEvent.getName()).isEqualTo("tool_execution");
        assertThat(customEvent.getValue()).isEqualTo(Map.of("toolName", "search"));
        assertThat(bridge.getAccumulatedAssistantText()).isNull();
    }

    @Test
    void testUnrecognisedMapEmitsGenericCustomEvent() {
        bridge.onEvent(Map.of("some_other_key", "value"));

        ArgumentCaptor<CustomEvent> eventCaptor = ArgumentCaptor.forClass(CustomEvent.class);

        verify(subscriber).onCustomEvent(eventCaptor.capture());

        CustomEvent customEvent = eventCaptor.getValue();

        assertThat(customEvent.getName()).isEqualTo(AgUiStreamBridge.WORKFLOW_EVENT_NAME);
    }

    @Test
    void testNonTextNonMapPayloadIsDropped() {
        bridge.onEvent(123L);

        verify(subscriber, never()).onEvent(any());
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

        verify(subscriber, never()).onTextMessageEndEvent(any());
        verify(subscriber).onRunFinishedEvent(any());
    }

    @Test
    void testOnCompleteClearsTheJobRegistryAndFinalizesOnce() {
        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 5L)));
        bridge.onComplete();
        bridge.onComplete();

        verify(subscriber, times(1)).onRunFinishedEvent(any());
        assertThat(jobRegistry.get(CHAT_ID)).isNull();
        assertThat(finalizeCount.get()).isEqualTo(1);
    }

    @Test
    void testOnSuspendThenOnCompleteFinishesOnce() {
        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 5L)));
        bridge.onSuspend();
        bridge.onComplete();

        verify(subscriber, times(1)).onRunFinishedEvent(any());
        verify(jobRegistry).clear(CHAT_ID);
        assertThat(jobRegistry.get(CHAT_ID)).isNull();
        assertThat(finalizeCount.get()).isEqualTo(1);
    }

    @Test
    void testOnErrorEmitsRunErrorEventOnceAndFinalizesOnce() {
        bridge.onError(new IllegalStateException("workflow failed"));
        bridge.onError(new IllegalStateException("again"));

        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, times(1)).onRunErrorEvent(errorCaptor.capture());

        RunErrorEvent runErrorEvent = errorCaptor.getValue();

        assertThat(runErrorEvent.getError()).isEqualTo("workflow failed");
        assertThat(finalizeCount.get()).isEqualTo(1);
    }

    @Test
    void testOnCompleteAfterOnErrorIsIgnored() {
        bridge.onError(new IllegalStateException("workflow failed"));
        bridge.onComplete();

        verify(subscriber, never()).onRunFinishedEvent(any());
        assertThat(finalizeCount.get()).isEqualTo(1);
    }

    @Test
    void testThrowingFinalizerDoesNotEscape() {
        AgUiStreamBridge bridge = new AgUiStreamBridge(
            subscriber, MESSAGE_ID, THREAD_ID, RUN_ID, CHAT_ID, registry, jobRegistry, null, jobId -> {},
            () -> {
                throw new IllegalStateException("finalize failed");
            });

        bridge.onComplete();

        verify(subscriber).onRunFinishedEvent(any());
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
        JobFacade jobFacade = mock(JobFacade.class);
        AgUiStreamBridge bridge = newBridge(jobFacade, jobId -> {});

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 7777L)));
        bridge.onError(new RuntimeException("workflow failed"));

        verify(jobFacade, times(1)).stopJob(7777L);
        assertThat(jobRegistry.get(CHAT_ID)).isNull();
    }

    @Test
    void testOnErrorSwallowsStopJobExceptionsSoRunErrorStillFires() {
        JobFacade jobFacade = mock(JobFacade.class);

        doThrow(new RuntimeException("coordinator down")).when(jobFacade)
            .stopJob(anyLong());

        AgUiStreamBridge bridge = newBridge(jobFacade, jobId -> {});

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 8888L)));
        bridge.onError(new RuntimeException("workflow failed"));

        verify(subscriber).onRunErrorEvent(any(RunErrorEvent.class));
        assertThat(finalizeCount.get()).isEqualTo(1);
    }

    @Test
    void testOnErrorSkipsStopJobWhenNoJobIdRegistered() {
        JobFacade jobFacade = mock(JobFacade.class);
        AgUiStreamBridge bridge = newBridge(jobFacade, jobId -> {});

        bridge.onError(new RuntimeException("workflow failed"));

        verify(jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testEnrichedTaskStartedRendersAsToolCallChip() {
        bridge.onEvent(
            Map.of(
                "event", "task_started",
                "payload", Map.of("taskExecutionId", 7L, "name", "slack_1", "type", "slack/v1/sendMessage")));

        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);

        verify(subscriber, times(2)).onEvent(eventCaptor.capture());

        List<BaseEvent> events = eventCaptor.getAllValues();

        assertThat(events.get(0)).isInstanceOf(ToolCallStartEvent.class);

        ToolCallStartEvent toolCallStartEvent = (ToolCallStartEvent) events.get(0);

        assertThat(toolCallStartEvent.getToolCallId()).isEqualTo("task-7");
        assertThat(toolCallStartEvent.getToolCallName()).isEqualTo("slack_1");
        assertThat(toolCallStartEvent.getParentMessageId()).isEqualTo(MESSAGE_ID);
        assertThat(events.get(1)).isInstanceOf(ToolCallEndEvent.class);
        verify(subscriber, never()).onTextMessageStartEvent(any());
    }

    @Test
    void testTaskStartedFallsBackToTheTaskType() {
        bridge.onEvent(
            Map.of("event", "task_started", "payload", Map.of("taskExecutionId", 7L, "type", "slack/v1/sendMessage")));

        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);

        verify(subscriber, times(2)).onEvent(eventCaptor.capture());

        ToolCallStartEvent toolCallStartEvent = (ToolCallStartEvent) eventCaptor.getAllValues()
            .get(0);

        assertThat(toolCallStartEvent.getToolCallName()).isEqualTo("slack/v1/sendMessage");
    }

    @Test
    void testTaskStartedWithoutUsableLabelIsDropped() {
        bridge.onEvent(Map.of("event", "task_started", "payload", Map.of("taskExecutionId", 7L)));

        verify(subscriber, never()).onEvent(any());
        verify(subscriber, never()).onTextMessageStartEvent(any());
    }

    @Test
    void testResultRendersOnlyWhenNothingWasStreamed() {
        bridge.onEvent(Map.of("event", "result", "result", Map.of("message", "final reply")));

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber).onTextMessageContentEvent(contentCaptor.capture());

        TextMessageContentEvent textMessageContentEvent = contentCaptor.getValue();

        assertThat(textMessageContentEvent.getDelta()).isEqualTo("final reply");
        assertThat(bridge.getAccumulatedAssistantText()).isEqualTo("final reply");
    }

    @Test
    void testResultIsSkippedAfterStreamedText() {
        bridge.onEvent("streamed text");
        bridge.onEvent(Map.of("event", "result", "result", Map.of("message", "streamed text")));

        verify(subscriber, times(1)).onTextMessageContentEvent(any());
    }

    private AgUiStreamBridge newBridge(LongConsumer onJobId) {
        return newBridge(null, onJobId);
    }

    private AgUiStreamBridge newBridge(@Nullable JobFacade jobFacade, LongConsumer onJobId) {
        return new AgUiStreamBridge(
            subscriber, MESSAGE_ID, THREAD_ID, RUN_ID, CHAT_ID, registry, jobRegistry, jobFacade, onJobId,
            finalizeCount::incrementAndGet);
    }
}
