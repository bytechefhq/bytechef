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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Default {@link ToolExecutionRecorder}: increments a Micrometer counter, writes a structured log line, and publishes a
 * {@link ToolExecutionEvent} as a Spring application event for any (EE) persistence listener. All three sinks are
 * best-effort — a failure to record never affects the wrapped execution.
 *
 * @author Ivica Cardic
 */
public class ToolExecutionRecorderImpl implements ToolExecutionRecorder {

    public static final String INVOCATION_COUNTER = "bytechef_tool_invocation";

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionRecorderImpl.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @SuppressFBWarnings("EI2")
    public ToolExecutionRecorderImpl(
        ApplicationEventPublisher applicationEventPublisher, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public <V> V record(ToolExecutionEvent.Builder builder, Supplier<V> execution) {
        long startNanos = System.nanoTime();

        try {
            V result = execution.get();

            emit(
                builder
                    .outcome(ToolExecutionOutcome.SUCCESS)
                    .durationMs(elapsedMs(startNanos))
                    .build());

            return result;
        } catch (RuntimeException exception) {
            emit(
                builder
                    .outcome(isTimeout(exception) ? ToolExecutionOutcome.TIMEOUT : ToolExecutionOutcome.ERROR)
                    .errorType(exception.getClass()
                        .getSimpleName())
                    .errorMessage(exception.getMessage())
                    .durationMs(elapsedMs(startNanos))
                    .build());

            throw exception;
        }
    }

    @Override
    public void record(ToolExecutionEvent event) {
        emit(event);
    }

    private void emit(ToolExecutionEvent event) {
        try {
            MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

            if (meterRegistry != null) {
                Counter.builder(INVOCATION_COUNTER)
                    .tag("surface", event.surface()
                        .name())
                    .tag("outcome", event.outcome()
                        .name())
                    .register(meterRegistry)
                    .increment();
            }

            if (log.isDebugEnabled()) {
                log.debug(
                    "Tool invocation surface={} kind={} tool={} outcome={} durationMs={} tenant={}",
                    event.surface(), event.kind(), event.toolName(), event.outcome(), event.durationMs(),
                    event.tenantId());
            }

            applicationEventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.warn("Failed to record tool invocation event: {}", exception.getMessage());
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
