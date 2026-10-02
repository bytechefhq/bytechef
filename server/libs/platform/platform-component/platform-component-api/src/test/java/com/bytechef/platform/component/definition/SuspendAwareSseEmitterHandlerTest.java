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

package com.bytechef.platform.component.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class SuspendAwareSseEmitterHandlerTest {

    @Test
    void testGetSuspendOrThrowReturnsTheRecordedSuspendAfterACompletedStream() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        SseEmitter sseEmitter = mock(SseEmitter.class);

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> {
                emitter.send("chunk");
                emitter.complete();
            },
            actionContext);

        suspendAwareSseEmitterHandler.handle(sseEmitter);

        verify(sseEmitter).send("chunk");
        verify(sseEmitter).complete();

        Suspend suspend = suspendAwareSseEmitterHandler.getSuspendOrThrow(1L);

        assertThat(suspend).isNotNull();
        assertThat(suspend.continueParameters()
            .get("pendingToolCallId")).isEqualTo("call_1");
    }

    @Test
    void testGetSuspendOrThrowThrowsWithTheStreamErrorAsCause() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        IllegalStateException streamException = new IllegalStateException("stream failed");

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> emitter.error(streamException), actionContext);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.getSuspendOrThrow(42L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("42")
            .hasCause(streamException);
    }

    @Test
    void testGetSuspendOrThrowKeepsTheFirstFailure() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        IllegalStateException firstException = new IllegalStateException("first");

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> {
                emitter.error(firstException);
                emitter.error(new IllegalStateException("second"));
            },
            actionContext);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.getSuspendOrThrow(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasCause(firstException);
    }

    @Test
    void testGetSuspendOrThrowThrowsTimeoutWhenTheUnderlyingEmitterTimesOut() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        SseEmitter sseEmitter = mock(SseEmitter.class);

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> emitter.send("chunk"), actionContext);

        suspendAwareSseEmitterHandler.handle(sseEmitter);

        ArgumentCaptor<Runnable> timeoutListenerCaptor = ArgumentCaptor.forClass(Runnable.class);

        verify(sseEmitter).addTimeoutListener(timeoutListenerCaptor.capture());

        Runnable timeoutListener = timeoutListenerCaptor.getValue();

        timeoutListener.run();

        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.getSuspendOrThrow(7L))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void testGetSuspendOrThrowReturnsNullWhenNothingSuspended() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, actionContext);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThat(suspendAwareSseEmitterHandler.getSuspendOrThrow(1L)).isNull();
    }

    @Test
    void testWithBeforeSuspendInvokesTheConsumerOnceWithTheRecordedSuspend() {
        ActionContextAware actionContext = mock(ActionContextAware.class);
        Suspend recordedSuspend = new Suspend(Map.of("pendingToolCallId", "call_1"), null);

        when(actionContext.getSuspend()).thenReturn(recordedSuspend);

        List<Suspend> beforeSuspendInvocations = new ArrayList<>();

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, actionContext).withBeforeSuspend(beforeSuspendInvocations::add);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThat(beforeSuspendInvocations).isEmpty();

        Suspend suspend = suspendAwareSseEmitterHandler.getSuspendOrThrow(1L);

        suspendAwareSseEmitterHandler.getSuspendOrThrow(1L);

        assertThat(beforeSuspendInvocations).containsExactly(recordedSuspend);
        assertThat(suspend).isSameAs(recordedSuspend);
    }

    @Test
    void testWithBeforeSuspendKeepsAFailureRecordedBeforeItWasSet() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        List<Suspend> beforeSuspendInvocations = new ArrayList<>();

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> emitter.error(new IllegalStateException("stream failed")), actionContext);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        SuspendAwareSseEmitterHandler withBeforeSuspend =
            suspendAwareSseEmitterHandler.withBeforeSuspend(beforeSuspendInvocations::add);

        assertThat(withBeforeSuspend).isSameAs(suspendAwareSseEmitterHandler);
        assertThatThrownBy(() -> withBeforeSuspend.getSuspendOrThrow(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("stream failed");
        assertThat(beforeSuspendInvocations).isEmpty();
    }

    @Test
    void testWithBeforeSuspendCanOnlyBeSetOnce() {
        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, mock(ActionContextAware.class)).withBeforeSuspend(suspend -> {});

        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.withBeforeSuspend(suspend -> {}))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testWithBeforeSuspendRetriesTheConsumerAfterItFailed() {
        ActionContextAware actionContext = mock(ActionContextAware.class);
        Suspend recordedSuspend = new Suspend(Map.of("pendingToolCallId", "call_1"), null);

        when(actionContext.getSuspend()).thenReturn(recordedSuspend);

        List<Suspend> beforeSuspendInvocations = new ArrayList<>();

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, actionContext).withBeforeSuspend(suspend -> {
                beforeSuspendInvocations.add(suspend);

                if (beforeSuspendInvocations.size() == 1) {
                    throw new IllegalStateException("Notification failed");
                }
            });

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.getSuspendOrThrow(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Notification failed");

        assertThat(suspendAwareSseEmitterHandler.getSuspendOrThrow(1L)).isNotNull();
        assertThat(suspendAwareSseEmitterHandler.getSuspendOrThrow(1L)).isNotNull();
        assertThat(beforeSuspendInvocations).hasSize(2);
    }

    @Test
    void testWithBeforeSuspendSkipsTheConsumerWhenNothingSuspended() {
        List<Suspend> beforeSuspendInvocations = new ArrayList<>();

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, mock(ActionContextAware.class)).withBeforeSuspend(beforeSuspendInvocations::add);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThat(suspendAwareSseEmitterHandler.getSuspendOrThrow(1L)).isNull();
        assertThat(beforeSuspendInvocations).isEmpty();
    }

    @Test
    void testWithBeforeSuspendSkipsTheConsumerAfterAFailedStream() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        List<Suspend> beforeSuspendInvocations = new ArrayList<>();

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> emitter.error(new IllegalStateException("stream failed")), actionContext)
                .withBeforeSuspend(beforeSuspendInvocations::add);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.getSuspendOrThrow(1L))
            .isInstanceOf(IllegalStateException.class);
        assertThat(beforeSuspendInvocations).isEmpty();
    }

    @Test
    void testWithBeforeSuspendPropagatesTheConsumerFailure() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        IllegalStateException hookException = new IllegalStateException("hook failed");

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, actionContext).withBeforeSuspend(suspend -> {
                throw hookException;
            });

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.getSuspendOrThrow(1L)).isSameAs(hookException);
    }
}
