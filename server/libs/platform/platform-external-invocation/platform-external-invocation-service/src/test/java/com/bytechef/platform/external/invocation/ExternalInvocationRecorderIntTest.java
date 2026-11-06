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

package com.bytechef.platform.external.invocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.config.ExternalInvocationConfiguration;
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
        ExternalInvocationConfiguration.class,
        ExternalInvocationRecorderIntTest.ExternalInvocationRecorderIntTestConfiguration.class
    })
class ExternalInvocationRecorderIntTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ExternalInvocationEventCollector externalInvocationEventCollector;

    @Autowired
    private ExternalInvocationRecorder externalInvocationRecorder;

    @BeforeEach
    void beforeEach() {
        meterRegistry.clear();
        externalInvocationEventCollector.reset();
    }

    @Test
    void testRecordSuccessPublishesSuccessEvent() {
        Object output = new Object();

        Object result = externalInvocationRecorder.record(
            ExternalInvocationEvent.builder(
                PlatformType.AUTOMATION, ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationKind.COMPONENT,
                "slack_sendMessage"),
            () -> output);

        assertThat(result).isSameAs(output);

        ExternalInvocationEvent event = getPublishedEvent();

        assertThat(event.outcome()).isEqualTo(ExternalInvocationOutcome.SUCCESS);
        assertThat(event.surface()).isEqualTo(ExternalInvocationSurface.MCP_AUTOMATION);
        assertThat(event.kind()).isEqualTo(ExternalInvocationKind.COMPONENT);
        assertThat(event.toolName()).isEqualTo("slack_sendMessage");
        assertThat(event.type()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(event.errorType()).isNull();
    }

    @Test
    void testRecordErrorPublishesErrorEventAndRethrows() {
        IllegalStateException failure = new IllegalStateException("boom");

        ExternalInvocationEvent.Builder externalInvocationEventBuilder = ExternalInvocationEvent.builder(
            PlatformType.AUTOMATION, ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationKind.COMPONENT,
            "slack_sendMessage");

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(
                () -> externalInvocationRecorder.record(
                    externalInvocationEventBuilder,
                    () -> {
                        throw failure;
                    }));

        ExternalInvocationEvent event = getPublishedEvent();

        assertThat(event.outcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
        assertThat(event.errorType()).isEqualTo("IllegalStateException");
        assertThat(event.errorMessage()).isEqualTo("boom");
    }

    @Test
    void testRecordTimeoutIsClassifiedFromCauseChain() {
        ExternalInvocationEvent.Builder externalInvocationEventBuilder = ExternalInvocationEvent.builder(
            PlatformType.AUTOMATION, ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationKind.WORKFLOW,
            "runWorkflow");

        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(
                () -> externalInvocationRecorder.record(
                    externalInvocationEventBuilder,
                    () -> {
                        throw new CompletionException(new TimeoutException("timed out"));
                    }));

        assertThat(getPublishedEvent().outcome()).isEqualTo(ExternalInvocationOutcome.TIMEOUT);
    }

    @Test
    void testRecordEventPublishesDirectly() {
        ExternalInvocationEvent connectionRequired = ExternalInvocationEvent.builder(
            PlatformType.EMBEDDED, ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationKind.COMPONENT,
            "slack_sendMessage")
            .outcome(ExternalInvocationOutcome.CONNECTION_REQUIRED)
            .build();

        externalInvocationRecorder.record(connectionRequired);

        assertThat(getPublishedEvent()).isSameAs(connectionRequired);
    }

    @Test
    void testErrorMessageIsTruncated() {
        String longMessage = "x".repeat(ExternalInvocationEvent.ERROR_MESSAGE_MAX_LENGTH + 100);

        ExternalInvocationEvent.Builder externalInvocationEventBuilder = ExternalInvocationEvent.builder(
            PlatformType.AUTOMATION, ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationKind.COMPONENT,
            "slack_sendMessage");

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(
                () -> externalInvocationRecorder.record(
                    externalInvocationEventBuilder,
                    () -> {
                        throw new RuntimeException(longMessage);
                    }));

        assertThat(getPublishedEvent().errorMessage()).hasSize(ExternalInvocationEvent.ERROR_MESSAGE_MAX_LENGTH);
    }

    @Test
    void testRecordResolvesTheOutcomeFromTheResult() {
        Object result = externalInvocationRecorder.record(
            ExternalInvocationEvent.builder(
                PlatformType.EMBEDDED, ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationKind.WORKFLOW,
                "runWorkflow"),
            () -> "paused",
            output -> "paused".equals(output) ? ExternalInvocationOutcome.APPROVAL_REQUIRED
                : ExternalInvocationOutcome.SUCCESS);

        assertThat(result).isEqualTo("paused");
        assertThat(getPublishedEvent().outcome()).isEqualTo(ExternalInvocationOutcome.APPROVAL_REQUIRED);
    }

    @Test
    void testRecordKeepsWhatTheExecutionSetsOnTheBuilder() {
        ExternalInvocationEvent.Builder externalInvocationEventBuilder = ExternalInvocationEvent.builder(
            PlatformType.EMBEDDED, ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationKind.WORKFLOW,
            "runWorkflow");

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(
                () -> externalInvocationRecorder.record(
                    externalInvocationEventBuilder,
                    () -> {
                        externalInvocationEventBuilder.jobId(5L);

                        throw new IllegalStateException("Job 5 failed");
                    }));

        ExternalInvocationEvent event = getPublishedEvent();

        assertThat(event.jobId()).isEqualTo(5L);
        assertThat(event.outcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
    }

    @Test
    void testRecordIncrementsTheInvocationCounterTaggedWithSurfaceAndOutcome() {
        externalInvocationRecorder.record(
            ExternalInvocationEvent.builder(
                PlatformType.EMBEDDED, ExternalInvocationSurface.EMBEDDED_API_TOOL, ExternalInvocationKind.COMPONENT,
                "slack_sendMessage"),
            () -> "ok");

        externalInvocationRecorder.record(
            ExternalInvocationEvent.builder(
                PlatformType.EMBEDDED, ExternalInvocationSurface.EMBEDDED_API_TOOL, ExternalInvocationKind.COMPONENT,
                "slack_sendMessage"),
            () -> "ok");

        Counter counter = meterRegistry.find(ExternalInvocationRecorderImpl.INVOCATION_COUNTER)
            .tag("surface", "EMBEDDED_API_TOOL")
            .tag("outcome", "SUCCESS")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void testAFailingListenerDoesNotBreakTheToolCall() {
        externalInvocationEventCollector.failOnNextEvent();

        Object result = externalInvocationRecorder.record(
            ExternalInvocationEvent.builder(
                PlatformType.AUTOMATION, ExternalInvocationSurface.MCP_AUTOMATION, ExternalInvocationKind.COMPONENT,
                "slack_sendMessage"),
            () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    private ExternalInvocationEvent getPublishedEvent() {
        List<ExternalInvocationEvent> externalInvocationEvents =
            externalInvocationEventCollector.getExternalInvocationEvents();

        assertThat(externalInvocationEvents).hasSize(1);

        return externalInvocationEvents.getFirst();
    }

    static class ExternalInvocationEventCollector {

        private final AtomicBoolean failOnNextEvent = new AtomicBoolean();
        private final List<ExternalInvocationEvent> externalInvocationEvents = new CopyOnWriteArrayList<>();

        void failOnNextEvent() {
            failOnNextEvent.set(true);
        }

        List<ExternalInvocationEvent> getExternalInvocationEvents() {
            return List.copyOf(externalInvocationEvents);
        }

        @EventListener
        void onExternalInvocationEvent(ExternalInvocationEvent externalInvocationEvent) {
            if (failOnNextEvent.getAndSet(false)) {
                throw new IllegalStateException("Listener failed");
            }

            externalInvocationEvents.add(externalInvocationEvent);
        }

        void reset() {
            failOnNextEvent.set(false);
            externalInvocationEvents.clear();
        }
    }

    @Configuration
    static class ExternalInvocationRecorderIntTestConfiguration {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        ExternalInvocationEventCollector externalInvocationEventCollector() {
            return new ExternalInvocationEventCollector();
        }
    }
}
