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
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class SuspendAwareSseEmitterHandlerTest {

    @Test
    void testGetSuspendReturnsTheSuspendWithTheJobResumeIdAfterACompletedStream() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));
        when(actionContext.getJobResumeId()).thenReturn("jobResumeId");

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

        Suspend suspend = suspendAwareSseEmitterHandler.getSuspend();

        assertThat(suspend).isNotNull();

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertThat(continueParameters.get("pendingToolCallId")).isEqualTo("call_1");
        assertThat(continueParameters.get(MetadataConstants.JOB_RESUME_ID)).isEqualTo("jobResumeId");
    }

    @Test
    void testGetSuspendReturnsNullAfterAFailedStream() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        SseEmitter sseEmitter = mock(SseEmitter.class);
        IllegalStateException streamException = new IllegalStateException("stream failed");

        SseEmitterHandler failingSseEmitterHandler = emitter -> emitter.error(streamException);

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            failingSseEmitterHandler, actionContext);

        suspendAwareSseEmitterHandler.handle(sseEmitter);

        verify(sseEmitter).error(streamException);

        assertThat(suspendAwareSseEmitterHandler.getSuspend()).isNull();
    }

    @Test
    void testGetSuspendReturnsNullWhenNothingSuspended() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, actionContext);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThat(suspendAwareSseEmitterHandler.getSuspend()).isNull();
    }

    @Test
    void testGetSuspendOrThrowThrowsWithTheStreamErrorAsCause() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

        IllegalStateException streamException = new IllegalStateException("stream failed");

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            emitter -> emitter.error(streamException), actionContext);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThat(suspendAwareSseEmitterHandler.getSuspend()).isNull();
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

        assertThat(suspendAwareSseEmitterHandler.getSuspend()).isNull();
        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.getSuspendOrThrow(7L))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void testGetSuspendOrThrowReturnsTheSuspendWithTheJobResumeIdAfterACompletedStream() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new Suspend(Map.of("pendingToolCallId", "call_1"), null));
        when(actionContext.getJobResumeId()).thenReturn("jobResumeId");

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, actionContext);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        Suspend suspend = suspendAwareSseEmitterHandler.getSuspendOrThrow(1L);

        assertThat(suspend).isNotNull();

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertThat(continueParameters.get("pendingToolCallId")).isEqualTo("call_1");
        assertThat(continueParameters.get(MetadataConstants.JOB_RESUME_ID)).isEqualTo("jobResumeId");
    }

    @Test
    void testGetSuspendOrThrowReturnsNullWhenNothingSuspended() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = new SuspendAwareSseEmitterHandler(
            SseEmitter::complete, actionContext);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThat(suspendAwareSseEmitterHandler.getSuspendOrThrow(1L)).isNull();
    }
}
