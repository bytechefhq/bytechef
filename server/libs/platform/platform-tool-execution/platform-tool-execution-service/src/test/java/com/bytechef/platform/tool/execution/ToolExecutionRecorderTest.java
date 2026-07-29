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

package com.bytechef.platform.tool.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class ToolExecutionRecorderTest {

    private ApplicationEventPublisher applicationEventPublisher;
    private ToolExecutionRecorderImpl toolExecutionRecorder;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);

        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(null);

        toolExecutionRecorder = new ToolExecutionRecorderImpl(applicationEventPublisher, meterRegistryProvider);
    }

    @Test
    void testRecordSuccessPublishesSuccessEvent() {
        Object output = new Object();

        Object result = toolExecutionRecorder.record(
            ToolExecutionEvent.builder(
                ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.COMPONENT, "slack_sendMessage")
                .tenantId("tenant1"),
            () -> output);

        assertThat(result).isSameAs(output);

        ToolExecutionEvent event = capturePublishedEvent();

        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.MCP_AUTOMATION);
        assertThat(event.kind()).isEqualTo(ToolExecutionKind.COMPONENT);
        assertThat(event.toolName()).isEqualTo("slack_sendMessage");
        assertThat(event.tenantId()).isEqualTo("tenant1");
        assertThat(event.errorType()).isNull();
    }

    @Test
    void testRecordErrorPublishesErrorEventAndRethrows() {
        IllegalStateException failure = new IllegalStateException("boom");

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(
                () -> toolExecutionRecorder.record(
                    ToolExecutionEvent.builder(
                        ToolExecutionSurface.EMBEDDED_API_ACTION, ToolExecutionKind.COMPONENT, "slack_sendMessage"),
                    () -> {
                        throw failure;
                    }));

        ToolExecutionEvent event = capturePublishedEvent();

        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.ERROR);
        assertThat(event.errorType()).isEqualTo("IllegalStateException");
        assertThat(event.errorMessage()).isEqualTo("boom");
    }

    @Test
    void testRecordTimeoutIsClassifiedFromCauseChain() {
        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(
                () -> toolExecutionRecorder.record(
                    ToolExecutionEvent.builder(
                        ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.WORKFLOW, "runWorkflow"),
                    () -> {
                        throw new CompletionException(new TimeoutException("timed out"));
                    }));

        assertThat(capturePublishedEvent().outcome()).isEqualTo(ToolExecutionOutcome.TIMEOUT);
    }

    @Test
    void testRecordEventPublishesDirectly() {
        ToolExecutionEvent connectionRequired = ToolExecutionEvent.builder(
            ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionKind.COMPONENT, "slack_sendMessage")
            .outcome(ToolExecutionOutcome.CONNECTION_REQUIRED)
            .build();

        toolExecutionRecorder.record(connectionRequired);

        assertThat(capturePublishedEvent()).isSameAs(connectionRequired);
    }

    @Test
    void testErrorMessageIsTruncated() {
        String longMessage = "x".repeat(ToolExecutionEvent.ERROR_MESSAGE_MAX_LENGTH + 100);

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(
                () -> toolExecutionRecorder.record(
                    ToolExecutionEvent.builder(
                        ToolExecutionSurface.EMBEDDED_API_TOOL, ToolExecutionKind.COMPONENT, "slack_sendMessage"),
                    () -> {
                        throw new RuntimeException(longMessage);
                    }));

        assertThat(capturePublishedEvent().errorMessage()).hasSize(ToolExecutionEvent.ERROR_MESSAGE_MAX_LENGTH);
    }

    private ToolExecutionEvent capturePublishedEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(applicationEventPublisher).publishEvent(captor.capture());

        return (ToolExecutionEvent) captor.getValue();
    }
}
