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

package com.bytechef.component.ai.agent.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Symmetric with {@code CheckForViolationsAdvisorTest.testAdvisorIsThreadSafeAcrossConcurrentInvocations}. SanitizeText
 * advisors are also instantiated once per agent and reused across requests — the per-call MaskEntityMap and
 * closedFailures must not leak between concurrent invocations.
 *
 * @author Ivica Cardic
 */
class SanitizeTextAdvisorConcurrentInvocationsTest {

    @Test
    void testSanitizeIsThreadSafeAcrossConcurrentInvocations() {
        AtomicInteger callCount = new AtomicInteger();

        GuardrailSanitizerFunction perRequest = (text, context) -> {
            callCount.incrementAndGet();

            // Per-call sanitization that depends only on the input text — any state leak would surface as a value
            // from another thread's invocation appearing in this thread's output.
            return text + "-marked";
        };

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("perRequest", perRequest, null, null)
            .context(mock(Context.class))
            .build();

        int concurrency = 100;
        Set<String> distinctOutputs = ConcurrentHashMap.newKeySet();

        IntStream.range(0, concurrency)
            .parallel()
            .forEach(index -> {
                String input = "request-" + index;
                String sanitized = advisor.sanitizeForTesting(input);

                distinctOutputs.add(sanitized);

                assertThat(sanitized)
                    .as("each concurrent invocation sees only its own input")
                    .isEqualTo(input + "-marked");
            });

        assertThat(callCount.get()).isEqualTo(concurrency);
        assertThat(distinctOutputs).hasSize(concurrency);
    }
}
