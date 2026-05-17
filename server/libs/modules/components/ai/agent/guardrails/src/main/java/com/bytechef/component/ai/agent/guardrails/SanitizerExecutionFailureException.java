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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public final class SanitizerExecutionFailureException extends GuardrailException {

    private final Map<String, Throwable> failures;

    public SanitizerExecutionFailureException(Map<String, Throwable> failures) {
        this(validateAndOrder(failures));
    }

    private SanitizerExecutionFailureException(LinkedHashMap<String, Throwable> ordered) {
        super(formatMessage(ordered), firstCauseOf(ordered));

        this.failures = Map.copyOf(ordered);

        Collection<Throwable> values = ordered.values();

        Iterator<Throwable> iterator = values.iterator();

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

    @Override
    public Optional<String> guardrailName() {
        return Optional.empty();
    }

    @Override
    public GuardrailExceptionKind kind() {
        return GuardrailExceptionKind.SANITIZER_FAILED;
    }

    private static String formatMessage(Map<String, Throwable> failures) {
        StringBuilder sb = new StringBuilder("Failed checks: ");
        int index = 0;

        for (Map.Entry<String, Throwable> entry : failures.entrySet()) {
            if (index > 0) {
                sb.append("; ");
            }

            sb.append(entry.getKey())
                .append(" - ")
                .append(describeCause(entry.getValue()));

            index++;
        }

        return sb.toString();
    }

    private static String describeCause(Throwable cause) {
        if (cause instanceof GuardrailException guardrailException) {
            GuardrailExceptionKind kind = guardrailException.kind();

            return kind.name();
        }

        Class<? extends Throwable> causeClass = cause.getClass();

        return causeClass.getSimpleName();
    }
}
