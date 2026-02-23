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
import com.bytechef.message.event.MessageEventPreSendProcessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

/**
 * Injects the current trace context into {@link MessageEvent} metadata before sending. This enables trace propagation
 * across async message broker boundaries (in-memory, Redis, Kafka, AMQP, JMS).
 *
 * @author Matija Petanjek
 */
@Component
class TracingMessageEventPreSendProcessor implements MessageEventPreSendProcessor {

    private final Propagator propagator;
    private final Tracer tracer;

    TracingMessageEventPreSendProcessor(Propagator propagator, Tracer tracer) {
        this.propagator = propagator;
        this.tracer = tracer;
    }

    @Override
    public MessageEvent<?> process(MessageEvent<?> messageEvent) {
        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {
            return messageEvent;
        }

        TraceContext traceContext = currentSpan.context();

        propagator.inject(traceContext, messageEvent, MessageEvent::putMetadata);

        return messageEvent;
    }

}
