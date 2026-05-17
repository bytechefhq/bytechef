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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A detected guardrail violation. Three variants: {@link PatternViolation} (rule-based, with matched substrings),
 * {@link ClassifiedViolation} (LLM-based, with confidence score), and {@link ExecutionFailureViolation} (the check
 * could not run). Use the {@code of...} factory methods rather than constructing records directly.
 *
 * @author Ivica Cardic
 */
public sealed interface Violation
    permits Violation.PatternViolation, Violation.ClassifiedViolation, Violation.ExecutionFailureViolation {

    String guardrail();

    /**
     * Per-check structured diagnostics. Immutable, never {@code null}. Do NOT put raw user content here — it leaks
     * through the metadata channel; report counts, categories, or type labels instead.
     */
    Map<String, Serializable> info();

    static Violation ofMatch(String guardrail, String matchedSubstring) {
        if (matchedSubstring == null || matchedSubstring.isEmpty()) {
            throw new IllegalArgumentException("matchedSubstring must be non-empty");
        }

        return new PatternViolation(guardrail, List.of(matchedSubstring));
    }

    static Violation ofMatches(String guardrail, List<String> matchedSubstrings) {
        return new PatternViolation(guardrail, matchedSubstrings);
    }

    static Violation ofMatches(
        String guardrail, List<String> matchedSubstrings, Map<String, ? extends Serializable> info) {

        return new PatternViolation(guardrail, matchedSubstrings, copyInfo(info));
    }

    static Violation ofClassification(String guardrail, double confidenceScore) {
        return new ClassifiedViolation(guardrail, confidenceScore);
    }

    static Violation ofClassification(
        String guardrail, double confidenceScore, Map<String, ? extends Serializable> info) {

        return new ClassifiedViolation(guardrail, confidenceScore, copyInfo(info));
    }

    static Violation ofExecutionFailure(String guardrail, Throwable cause) {
        return new ExecutionFailureViolation(guardrail, cause);
    }

    private static Map<String, Serializable> copyInfo(Map<String, ? extends Serializable> info) {
        if (info == null || info.isEmpty()) {
            return Map.of();
        }

        return Map.copyOf(info);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    record PatternViolation(String guardrail, List<String> matchedSubstrings, Map<String, Serializable> info)
        implements Violation {

        public PatternViolation(String guardrail, List<String> matchedSubstrings) {
            this(guardrail, matchedSubstrings, Map.of());
        }

        public PatternViolation {
            if (guardrail == null || guardrail.isBlank()) {
                throw new IllegalArgumentException("guardrail must be non-blank");
            }

            if (matchedSubstrings == null || matchedSubstrings.isEmpty()) {
                throw new IllegalArgumentException("matchedSubstrings must be non-empty");
            }

            for (String substring : matchedSubstrings) {
                if (substring == null || substring.isEmpty()) {
                    throw new IllegalArgumentException("matchedSubstrings must not contain null or empty entries");
                }
            }

            matchedSubstrings = List.copyOf(matchedSubstrings);
            info = info == null ? Map.of() : Map.copyOf(info);
        }
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    record ClassifiedViolation(String guardrail, double confidenceScore, Map<String, Serializable> info)
        implements Violation {

        public ClassifiedViolation(String guardrail, double confidenceScore) {
            this(guardrail, confidenceScore, Map.of());
        }

        public ClassifiedViolation {
            if (guardrail == null || guardrail.isBlank()) {
                throw new IllegalArgumentException("guardrail must be non-blank");
            }

            if (Double.isNaN(confidenceScore) || confidenceScore < 0.0 || confidenceScore > 1.0) {
                throw new IllegalArgumentException("confidenceScore must be in [0.0, 1.0], got " + confidenceScore);
            }

            info = info == null ? Map.of() : Map.copyOf(info);
        }
    }

    /**
     * Carries the raw {@link Throwable} from a failed guardrail execution. The exception reference is shared; consumers
     * MUST NOT mutate it.
     */
    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    record ExecutionFailureViolation(String guardrail, Throwable exception) implements Violation {

        public ExecutionFailureViolation {
            if (guardrail == null || guardrail.isBlank()) {
                throw new IllegalArgumentException("guardrail must be non-blank");
            }

            if (exception == null) {
                throw new IllegalArgumentException("exception must not be null");
            }
        }

        @Override
        public Map<String, Serializable> info() {
            return Map.of();
        }
    }

}
