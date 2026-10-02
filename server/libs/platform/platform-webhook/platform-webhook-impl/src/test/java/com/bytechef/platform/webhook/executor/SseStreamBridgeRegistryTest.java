/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.webhook.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry.Registration;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
class SseStreamBridgeRegistryTest {

    private static final long JOB_ID = 42L;

    private SseStreamBridge sseStreamBridge;
    private SseStreamBridgeRegistry sseStreamBridgeRegistry;

    @BeforeEach
    void beforeEach() {
        sseStreamBridge = mock(SseStreamBridge.class);
        sseStreamBridgeRegistry = new SseStreamBridgeRegistry();
    }

    @Test
    void testRegisterDeliversEventsAndCompletesOnTerminalStatus() {
        Registration registration = sseStreamBridgeRegistry.register(JOB_ID, sseStreamBridge);

        sendData("Hello");
        sendJobStatus("COMPLETED");

        verify(sseStreamBridge).onEvent("Hello");
        verify(sseStreamBridge).onComplete();

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testAnAskUserQuestionEventThatNoBridgeDeliversIsLoggedAsAWarning() {
        Map<String, Object> questionEvent = Map.of(
            AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION, "questions", List.of());

        doThrow(new IllegalStateException("not delivered")).when(sseStreamBridge)
            .onEvent(questionEvent);

        sseStreamBridgeRegistry.register(JOB_ID, sseStreamBridge);

        Logger logger = (Logger) LoggerFactory.getLogger(SseStreamBridgeRegistry.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            sendData(questionEvent);
            sendData("chunk");
        } finally {
            logger.detachAppender(listAppender);
        }

        assertThat(listAppender.list)
            .filteredOn(loggingEvent -> loggingEvent.getLevel() == Level.WARN)
            .hasSize(1);
    }

    @Test
    void testRegisterSignalsASuspendOnStoppedStatus() {
        Registration registration = sseStreamBridgeRegistry.register(JOB_ID, sseStreamBridge);

        sendJobStatus("STOPPED");

        verify(sseStreamBridge).onSuspend();
        verify(sseStreamBridge, never()).onComplete();

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testRegisterSignalsASuspendOnStoppedStatusOfASuspendedJob() {
        Registration registration = sseStreamBridgeRegistry.register(JOB_ID, sseStreamBridge);

        sendStoppedJobStatus(true);

        verify(sseStreamBridge).onSuspend();
        verify(sseStreamBridge, never()).onComplete();

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testRegisterCompletesOnStoppedStatusOfAJobThatWasNotSuspended() {
        Registration registration = sseStreamBridgeRegistry.register(JOB_ID, sseStreamBridge);

        sendStoppedJobStatus(false);

        verify(sseStreamBridge).onComplete();
        verify(sseStreamBridge, never()).onSuspend();

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testRegisterForResumeCompletesWhenTheResumedJobIsStoppedWithoutASuspend() {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendStoppedJobStatus(false);

        verify(sseStreamBridge).onComplete();
        verify(sseStreamBridge, never()).onSuspend();

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testRegisterForResumeIgnoresEventsWhilePendingAndActivatesOnStoppedStatus() {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendData("stale delta");
        sendEvent(SseStreamEvent.EVENT_TYPE_COMPLETE, null);
        sendEvent(SseStreamEvent.EVENT_TYPE_ERROR, "stale error");
        sendJobStatus("STOPPED");

        verifyNoInteractions(sseStreamBridge);

        assertThat(registration.completion()).isNotDone();

        sendData("Hello");

        verify(sseStreamBridge).onEvent("Hello");
        verify(sseStreamBridge, never()).onComplete();
        verify(sseStreamBridge, never()).onError(any());
    }

    @Test
    void testRegisterForResumeActivatesOnTaskStartedAndDeliversIt() {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendData("stale delta");

        sendEvent(SseStreamEvent.EVENT_TYPE_TASK_STARTED, 7L);
        sendData("Hello");

        InOrder inOrder = inOrder(sseStreamBridge);

        inOrder.verify(sseStreamBridge)
            .onEvent(7L);
        inOrder.verify(sseStreamBridge)
            .onEvent("Hello");

        verify(sseStreamBridge, never()).onEvent("stale delta");

        assertThat(registration.completion()).isNotDone();
    }

    @Test
    void testRegisterForResumeActivatesImmediatelyWhenStoppedWasAlreadySeen() {
        sendJobStatus("STOPPED");

        sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendData("Hello");

        verify(sseStreamBridge).onEvent("Hello");
    }

    @Test
    void testRegisterForResumeConsumesTheSeenStoppedStatusOnce() {
        SseStreamBridge laterSseStreamBridge = mock(SseStreamBridge.class);

        sendJobStatus("STOPPED");

        sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);
        sseStreamBridgeRegistry.registerForResume(JOB_ID, laterSseStreamBridge);

        sendData("Hello");

        verify(sseStreamBridge).onEvent("Hello");
        verifyNoInteractions(laterSseStreamBridge);
    }

    @Test
    void testRegisterForResumeIgnoresAStoppedStatusSeenBeforeANonStreamingResume() {
        sendJobStatus("STOPPED");
        sendEvent(SseStreamEvent.EVENT_TYPE_TASK_STARTED, 7L);

        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendData("stale delta");
        sendJobStatus("STOPPED");

        verifyNoInteractions(sseStreamBridge);

        assertThat(registration.completion()).isNotDone();

        sendData("Hello");

        verify(sseStreamBridge).onEvent("Hello");
        verify(sseStreamBridge, never()).onComplete();
    }

    @Test
    void testClosingPendingRegistrationStopsLaterEvents() throws Exception {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        registration.close();

        sendJobStatus("STOPPED");
        sendEvent(SseStreamEvent.EVENT_TYPE_TASK_STARTED, 7L);
        sendData("Hello");
        sendJobStatus("COMPLETED");

        verifyNoInteractions(sseStreamBridge);
    }

    @Test
    void testRegistrationCompletionCannotBeCompletedByAHolder() {
        Registration registration = sseStreamBridgeRegistry.register(JOB_ID, sseStreamBridge);

        CompletionStage<Void> completion = registration.completion();

        CompletableFuture<Void> completionFuture = completion.toCompletableFuture();

        completionFuture.complete(null);

        Registration otherRegistration = sseStreamBridgeRegistry.register(JOB_ID, mock(SseStreamBridge.class));

        assertThat(otherRegistration.completion()).isNotDone();
    }

    @Test
    void testRegistrationClosesItsHandleOnlyOnceAndSwallowsFailures() throws Exception {
        AutoCloseable handle = mock(AutoCloseable.class);

        doThrow(new IllegalStateException("boom")).when(handle)
            .close();

        Registration registration = new Registration(handle, new CompletableFuture<>());

        registration.close();
        registration.close();

        verify(handle, times(1)).close();
    }

    @Test
    void testClosingActiveRegistrationStopsLaterEvents() throws Exception {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendJobStatus("STOPPED");
        sendData("Hello");

        registration.close();

        sendData("after close");

        verify(sseStreamBridge).onEvent("Hello");
        verify(sseStreamBridge, never()).onEvent("after close");
    }

    @Test
    void testRegisterForResumeCompletionCompletesOnLaterCompletedStatus() {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendJobStatus("STOPPED");
        sendData("Hello");

        assertThat(registration.completion()).isNotDone();

        sendJobStatus("COMPLETED");

        verify(sseStreamBridge).onComplete();

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testRegisterForResumeCompletionCompletesOnLaterFailedStatus() {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendEvent(SseStreamEvent.EVENT_TYPE_TASK_STARTED, 7L);
        sendJobStatus("FAILED");

        verify(sseStreamBridge).onError(any(RuntimeException.class));

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testRegisterForResumeFailsOnFailedStatusWithoutTaskStarted() {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendJobStatus("FAILED");

        verify(sseStreamBridge).onError(any(RuntimeException.class));
        verify(sseStreamBridge, never()).onComplete();

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testRegisterForResumeCompletesOnCompletedStatusWithoutTaskStarted() {
        Registration registration = sseStreamBridgeRegistry.registerForResume(JOB_ID, sseStreamBridge);

        sendJobStatus("COMPLETED");

        verify(sseStreamBridge).onComplete();
        verify(sseStreamBridge, never()).onError(any());

        assertThat(registration.completion()).isDone();
    }

    @Test
    void testFailedStatusForwardsTheFailedTaskErrorMessage() {
        sseStreamBridgeRegistry.register(JOB_ID, sseStreamBridge);

        SseStreamEvent sseStreamEvent = new SseStreamEvent(JOB_ID, SseStreamEvent.EVENT_TYPE_JOB_STATUS, "FAILED");

        sseStreamEvent.putMetadata(SseStreamEvent.METADATA_ERROR_MESSAGE, "The stored conversation could not be read");

        sseStreamBridgeRegistry.onSseStreamEvent(sseStreamEvent);

        ArgumentCaptor<Throwable> throwableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(sseStreamBridge).onError(throwableArgumentCaptor.capture());

        assertThat(throwableArgumentCaptor.getValue()).hasMessage("The stored conversation could not be read");
    }

    @Test
    void testFailedStatusWithoutAnErrorMessageReportsThatTheJobFailed() {
        sseStreamBridgeRegistry.register(JOB_ID, sseStreamBridge);

        sendJobStatus("FAILED");

        ArgumentCaptor<Throwable> throwableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(sseStreamBridge).onError(throwableArgumentCaptor.capture());

        assertThat(throwableArgumentCaptor.getValue()).hasMessage("Job failed");
    }

    @Test
    void testPendingRegistrationActivatesWhenTheResumedJobHasNotStartedInTime() {
        AtomicLong nanos = new AtomicLong();

        SseStreamBridgeRegistry registry =
            new SseStreamBridgeRegistry(Duration.ofMinutes(2), nanos::get, Runnable::run);

        Registration registration = registry.registerForResume(JOB_ID, sseStreamBridge);

        nanos.addAndGet(Duration.ofMinutes(3)
            .toNanos());

        registry.cleanUp();

        registry.onSseStreamEvent(new SseStreamEvent(JOB_ID, SseStreamEvent.EVENT_TYPE_DATA, "late delta"));

        verify(sseStreamBridge, timeout(5000)).onEvent("late delta");
        verify(sseStreamBridge, never()).onError(any());

        assertThat(registration.completion()).isNotDone();
    }

    @Test
    void testExpiryDoesNotFailOrReactivateAnActivatedRegistration() {
        AtomicLong nanos = new AtomicLong();

        SseStreamBridgeRegistry registry =
            new SseStreamBridgeRegistry(Duration.ofMinutes(2), nanos::get, Runnable::run);

        registry.registerForResume(JOB_ID, sseStreamBridge);

        registry.onSseStreamEvent(new SseStreamEvent(JOB_ID, SseStreamEvent.EVENT_TYPE_TASK_STARTED, "task-1"));

        nanos.addAndGet(Duration.ofMinutes(3)
            .toNanos());

        registry.cleanUp();

        registry.onSseStreamEvent(new SseStreamEvent(JOB_ID, SseStreamEvent.EVENT_TYPE_DATA, "delta"));

        verify(sseStreamBridge, after(200).never()).onError(any());
        verify(sseStreamBridge, times(1)).onEvent("delta");
    }

    private void sendData(Object payload) {
        sendEvent(SseStreamEvent.EVENT_TYPE_DATA, payload);
    }

    private void sendEvent(String eventType, @Nullable Object payload) {
        sseStreamBridgeRegistry.onSseStreamEvent(new SseStreamEvent(JOB_ID, eventType, payload));
    }

    private void sendJobStatus(String status) {
        sendEvent(SseStreamEvent.EVENT_TYPE_JOB_STATUS, status);
    }

    private void sendStoppedJobStatus(boolean suspended) {
        SseStreamEvent sseStreamEvent = new SseStreamEvent(JOB_ID, SseStreamEvent.EVENT_TYPE_JOB_STATUS, "STOPPED");

        sseStreamEvent.putMetadata(SseStreamEvent.METADATA_SUSPENDED, suspended);

        sseStreamBridgeRegistry.onSseStreamEvent(sseStreamEvent);
    }
}
