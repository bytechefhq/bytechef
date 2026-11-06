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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class ExternalInvocationRecorderImpl implements ExternalInvocationRecorder {

    public static final String INVOCATION_COUNTER = "bytechef_external_invocation";

    private static final Logger log = LoggerFactory.getLogger(ExternalInvocationRecorderImpl.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @SuppressFBWarnings("EI2")
    public ExternalInvocationRecorderImpl(
        ApplicationEventPublisher applicationEventPublisher, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public <V> V record(
        ExternalInvocationEvent.Builder builder, Supplier<V> execution,
        Function<? super V, ExternalInvocationOutcome> outcomeResolver) {

        long startNanos = System.nanoTime();

        try {
            V result = execution.get();

            emit(
                builder
                    .outcome(outcomeResolver.apply(result))
                    .durationMs(elapsedMs(startNanos))
                    .build());

            return result;
        } catch (RuntimeException exception) {
            emit(
                builder
                    .outcome(isTimeout(exception) ? ExternalInvocationOutcome.TIMEOUT : ExternalInvocationOutcome.ERROR)
                    .errorType(exception.getClass()
                        .getSimpleName())
                    .errorMessage(exception.getMessage())
                    .durationMs(elapsedMs(startNanos))
                    .build());

            throw exception;
        }
    }

    @Override
    public void record(ExternalInvocationEvent event) {
        emit(event);
    }

    private void emit(ExternalInvocationEvent event) {
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
                    "External invocation type={} surface={} kind={} tool={} outcome={} durationMs={}",
                    event.type(), event.surface(), event.kind(), event.toolName(), event.outcome(),
                    event.durationMs());
            }

            applicationEventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.warn("Failed to record external invocation event: {}", exception.getMessage());
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
