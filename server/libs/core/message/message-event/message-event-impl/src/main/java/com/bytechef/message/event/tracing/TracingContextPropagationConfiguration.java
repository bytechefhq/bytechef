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

import io.micrometer.context.ContextRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link ObservationAwareSpanThreadLocalAccessor} in the global {@link ContextRegistry} so that
 * {@code ContextSnapshot.captureAll()} captures Micrometer tracing context. Without this registration, spans are not
 * propagated across {@code @Async} and thread pool boundaries, causing {@code tracer.currentSpan()} to return
 * {@code null} on worker threads.
 *
 * @author Matija Petanjek
 */
@Configuration
class TracingContextPropagationConfiguration {

    TracingContextPropagationConfiguration(Tracer tracer) {
        ContextRegistry.getInstance()
            .registerThreadLocalAccessor(new ObservationAwareSpanThreadLocalAccessor(tracer));
    }

}
