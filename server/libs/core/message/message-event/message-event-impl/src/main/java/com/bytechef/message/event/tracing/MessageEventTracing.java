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

package com.bytechef.message.event.tracing;

import com.bytechef.message.event.MessageEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

/**
 * Extracts trace context from {@link MessageEvent} metadata and executes a {@link Runnable} within a child span. Used
 * by message broker delegate classes to restore trace context on the receive side.
 *
 * @author Matija Petanjek
 */
@Component
public class MessageEventTracing {

    private static final Propagator.Getter<MessageEvent<?>> MESSAGE_EVENT_GETTER =
        (messageEvent, key) -> {
            Object value = messageEvent.getMetadata(key);

            if (value instanceof String stringValue) {
                return stringValue;
            }

            return null;
        };

    private final Propagator propagator;
    private final Tracer tracer;

    public MessageEventTracing(Propagator propagator, Tracer tracer) {
        this.propagator = propagator;
        this.tracer = tracer;
    }

    /**
     * Extracts trace context from the message event metadata, creates a child span with the given name, and runs the
     * action within that span's scope. If no trace context is present, the action runs without tracing.
     */
    @SuppressWarnings("PMD.UnusedLocalVariable")
    public void runWithTraceContext(MessageEvent<?> messageEvent, String spanName, Runnable action) {
        Span.Builder spanBuilder = propagator.extract(messageEvent, MESSAGE_EVENT_GETTER);

        Span span = spanBuilder.name(spanName)
            .kind(Span.Kind.CONSUMER)
            .start();

        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            action.run();
        } finally {
            span.end();
        }
    }

}
