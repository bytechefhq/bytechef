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

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.config.ToolExecutionConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Ivica Cardic
 */
@SpringJUnitConfig(
    classes = {
        ToolExecutionConfiguration.class, ToolExecutionRecorderIntTest.ToolExecutionRecorderIntTestConfiguration.class
    })
class ToolExecutionRecorderIntTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ToolExecutionEventCollector toolExecutionEventCollector;

    @Autowired
    private ToolExecutionRecorder toolExecutionRecorder;

    @BeforeEach
    void beforeEach() {
        meterRegistry.clear();
        toolExecutionEventCollector.reset();
    }

    @Test
    void testRecordSuccessPublishesSuccessEvent() {
        Object output = new Object();

        Object result = toolExecutionRecorder.record(
            ToolExecutionEvent.builder(
                PlatformType.AUTOMATION, ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.COMPONENT,
                "slack_sendMessage"),
            () -> output);

        assertThat(result).isSameAs(output);

        ToolExecutionEvent event = getPublishedEvent();

        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.MCP_AUTOMATION);
        assertThat(event.kind()).isEqualTo(ToolExecutionKind.COMPONENT);
        assertThat(event.toolName()).isEqualTo("slack_sendMessage");
        assertThat(event.type()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(event.errorType()).isNull();
    }

    @Test
    void testRecordErrorPublishesErrorEventAndRethrows() {
        IllegalStateException failure = new IllegalStateException("boom");

        ToolExecutionEvent.Builder toolExecutionEventBuilder = ToolExecutionEvent.builder(
            PlatformType.AUTOMATION, ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.COMPONENT,
            "slack_sendMessage");

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(
                () -> toolExecutionRecorder.record(
                    toolExecutionEventBuilder,
                    () -> {
                        throw failure;
                    }));

        ToolExecutionEvent event = getPublishedEvent();

        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.ERROR);
        assertThat(event.errorType()).isEqualTo("IllegalStateException");
        assertThat(event.errorMessage()).isEqualTo("boom");
    }

    @Test
    void testRecordTimeoutIsClassifiedFromCauseChain() {
        ToolExecutionEvent.Builder toolExecutionEventBuilder = ToolExecutionEvent.builder(
            PlatformType.AUTOMATION, ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.WORKFLOW, "runWorkflow");

        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(
                () -> toolExecutionRecorder.record(
                    toolExecutionEventBuilder,
                    () -> {
                        throw new CompletionException(new TimeoutException("timed out"));
                    }));

        assertThat(getPublishedEvent().outcome()).isEqualTo(ToolExecutionOutcome.TIMEOUT);
    }

    @Test
    void testRecordEventPublishesDirectly() {
        ToolExecutionEvent connectionRequired = ToolExecutionEvent.builder(
            PlatformType.EMBEDDED, ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionKind.COMPONENT, "slack_sendMessage")
            .outcome(ToolExecutionOutcome.CONNECTION_REQUIRED)
            .build();

        toolExecutionRecorder.record(connectionRequired);

        assertThat(getPublishedEvent()).isSameAs(connectionRequired);
    }

    @Test
    void testErrorMessageIsTruncated() {
        String longMessage = "x".repeat(ToolExecutionEvent.ERROR_MESSAGE_MAX_LENGTH + 100);

        ToolExecutionEvent.Builder toolExecutionEventBuilder = ToolExecutionEvent.builder(
            PlatformType.AUTOMATION, ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.COMPONENT,
            "slack_sendMessage");

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(
                () -> toolExecutionRecorder.record(
                    toolExecutionEventBuilder,
                    () -> {
                        throw new RuntimeException(longMessage);
                    }));

        assertThat(getPublishedEvent().errorMessage()).hasSize(ToolExecutionEvent.ERROR_MESSAGE_MAX_LENGTH);
    }

    @Test
    void testRecordIncrementsTheInvocationCounterTaggedWithSurfaceAndOutcome() {
        toolExecutionRecorder.record(
            ToolExecutionEvent.builder(
                PlatformType.EMBEDDED, ToolExecutionSurface.EMBEDDED_API_TOOL, ToolExecutionKind.COMPONENT,
                "slack_sendMessage"),
            () -> "ok");

        toolExecutionRecorder.record(
            ToolExecutionEvent.builder(
                PlatformType.EMBEDDED, ToolExecutionSurface.EMBEDDED_API_TOOL, ToolExecutionKind.COMPONENT,
                "slack_sendMessage"),
            () -> "ok");

        Counter counter = meterRegistry.find(ToolExecutionRecorderImpl.INVOCATION_COUNTER)
            .tag("surface", "EMBEDDED_API_TOOL")
            .tag("outcome", "SUCCESS")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void testAFailingListenerDoesNotBreakTheToolCall() {
        toolExecutionEventCollector.failOnNextEvent();

        Object result = toolExecutionRecorder.record(
            ToolExecutionEvent.builder(
                PlatformType.AUTOMATION, ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.COMPONENT,
                "slack_sendMessage"),
            () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    private ToolExecutionEvent getPublishedEvent() {
        List<ToolExecutionEvent> toolExecutionEvents = toolExecutionEventCollector.getToolExecutionEvents();

        assertThat(toolExecutionEvents).hasSize(1);

        return toolExecutionEvents.getFirst();
    }

    static class ToolExecutionEventCollector {

        private final AtomicBoolean failOnNextEvent = new AtomicBoolean();
        private final List<ToolExecutionEvent> toolExecutionEvents = new CopyOnWriteArrayList<>();

        void failOnNextEvent() {
            failOnNextEvent.set(true);
        }

        List<ToolExecutionEvent> getToolExecutionEvents() {
            return List.copyOf(toolExecutionEvents);
        }

        @EventListener
        void onToolExecutionEvent(ToolExecutionEvent toolExecutionEvent) {
            if (failOnNextEvent.getAndSet(false)) {
                throw new IllegalStateException("Listener failed");
            }

            toolExecutionEvents.add(toolExecutionEvent);
        }

        void reset() {
            failOnNextEvent.set(false);
            toolExecutionEvents.clear();
        }
    }

    @Configuration
    static class ToolExecutionRecorderIntTestConfiguration {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        ToolExecutionEventCollector toolExecutionEventCollector() {
            return new ToolExecutionEventCollector();
        }
    }
}
