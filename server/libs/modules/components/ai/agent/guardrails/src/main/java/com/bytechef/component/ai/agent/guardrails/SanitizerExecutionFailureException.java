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

package com.bytechef.component.ai.agent.guardrails;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates sanitizer-execution failures across a single sanitize pass. Thrown by {@code SanitizeTextAdvisor} when one
 * or more sanitizers threw during a pass so observability sees the whole picture instead of a single WARN per call. The
 * advisor's streaming path withholds the response on this exception; synchronous callers must catch it.
 *
 * <p>
 * The first entry's {@link Throwable} becomes the primary {@link #getCause() cause} so Sentry / log pipelines see a
 * populated "Caused by" chain and can deduplicate on that stack trace. Every subsequent entry is attached via
 * {@link #addSuppressed(Throwable)} so the full per-sanitizer stacks remain reachable from one aggregated exception.
 *
 * @author Ivica Cardic
 */
public final class SanitizerExecutionFailureException extends GuardrailException {

    private final Map<String, Throwable> failures;

    public SanitizerExecutionFailureException(Map<String, Throwable> failures) {
        // Reject null/empty at the boundary — an empty-failures aggregate is semantically nonsensical and
        // producing "Failed checks: " (no entries) would surprise operators. Delegate to the private constructor
        // so the ordered copy is computed exactly once and then reused in super() + the body; inlining the
        // computation would mean validateAndOrder runs three times (once for super's message, once for super's
        // cause, once for the field assignment). The private constructor takes LinkedHashMap explicitly so its
        // signature differs from the public constructor's Map parameter — otherwise they'd collide.
        this(validateAndOrder(failures));
    }

    private SanitizerExecutionFailureException(LinkedHashMap<String, Throwable> ordered) {
        super(formatMessage(ordered), firstCauseOf(ordered));

        this.failures = Map.copyOf(ordered);

        // super(...) received the first failure as cause so "Caused by:" is populated. Attach every additional
        // failure as a suppressed exception so operators reading the stack see each sanitizer's trace without having
        // to reach through failures() programmatically. Skip index 0 — it's already the cause.
        Iterator<Throwable> iterator = ordered.values()
            .iterator();

        if (iterator.hasNext()) {
            iterator.next();
        }

        while (iterator.hasNext()) {
            addSuppressed(iterator.next());
        }
    }

    private static Throwable firstCauseOf(Map<String, Throwable> ordered) {
        return ordered.values()
            .iterator()
            .next();
    }

    private static LinkedHashMap<String, Throwable> validateAndOrder(Map<String, Throwable> failures) {
        if (failures == null) {
            throw new IllegalArgumentException("failures map must not be null");
        }

        if (failures.isEmpty()) {
            throw new IllegalArgumentException("failures map must not be empty");
        }

        return new LinkedHashMap<>(failures);
    }

    public Map<String, Throwable> failures() {
        return failures;
    }

    /** {@code null} because this exception aggregates failures across multiple guardrails. */
    @Override
    public String guardrailName() {
        return null;
    }

    @Override
    public GuardrailExceptionKind kind() {
        return GuardrailExceptionKind.SANITIZER_FAILED;
    }

    /**
     * Build the headline message for the aggregated exception. Each entry is tagged by guardrail name + a
     * version-stable failure kind ({@link GuardrailExceptionKind}) or simple class name, NEVER the raw
     * {@code getMessage()} of the underlying cause. Cause messages are free to include user prompt fragments or raw LLM
     * output (the per-guardrail detector's choice), so concatenating them into this headline would leak sensitive
     * substrings into every log that prints {@code exception.getMessage()}. Operators wanting details drill into the
     * populated cause / suppressed-exception chain instead.
     */
    private static String formatMessage(Map<String, Throwable> failures) {
        StringBuilder builder = new StringBuilder("Failed checks: ");
        int index = 0;

        for (Map.Entry<String, Throwable> entry : failures.entrySet()) {
            if (index > 0) {
                builder.append("; ");
            }

            builder.append(entry.getKey())
                .append(" - ")
                .append(describeCause(entry.getValue()));

            index++;
        }

        return builder.toString();
    }

    private static String describeCause(Throwable cause) {
        if (cause instanceof GuardrailException guardrailException) {
            return guardrailException.kind()
                .name();
        }

        return cause.getClass()
            .getSimpleName();
    }
}
