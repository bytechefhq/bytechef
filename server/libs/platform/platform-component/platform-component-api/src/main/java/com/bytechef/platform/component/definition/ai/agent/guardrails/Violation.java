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
import java.util.List;
import java.util.Map;

/**
 * A detected guardrail violation. Three kinds are distinguishable at the type level:
 * <ul>
 * <li>{@link PatternViolation} — rule-based guardrails that fired on one or more concrete substring matches.</li>
 * <li>{@link ClassifiedViolation} — LLM-based guardrails where the signal is a classifier confidence score.</li>
 * <li>{@link ExecutionFailureViolation} — the check could not run (LLM outage, missing MODEL child, bad config). The
 * root cause is carried on {@link ExecutionFailureViolation#exception()}.</li>
 * </ul>
 *
 * <p>
 * Use the factory methods {@link #ofMatch(String, String)}, {@link #ofMatches(String, List)},
 * {@link #ofMatches(String, List, Map)}, {@link #ofClassification(String, double)},
 * {@link #ofClassification(String, double, Map)}, or {@link #ofExecutionFailure(String, Throwable)} rather than
 * constructing the records directly.
 *
 * <p>
 * Consumers that care about variant-specific state must pattern-match on the sealed hierarchy. The earlier universal
 * accessors that returned empty / null for irrelevant variants have been removed — the sealed switch is exhaustive and
 * each variant's state is exposed only on the record type that owns it:
 *
 * <pre>{@code
 * switch (violation) {
 *     case PatternViolation p -> handleMatches(p.matchedSubstrings());
 *     case ClassifiedViolation c -> handleScore(c.confidenceScore());
 *     case ExecutionFailureViolation f -> handleFailure(f.exception());
 * }
 * }</pre>
 *
 * @author Ivica Cardic
 */
public sealed interface Violation
    permits Violation.PatternViolation, Violation.ClassifiedViolation, Violation.ExecutionFailureViolation {

    String guardrail();

    /**
     * Per-check structured diagnostics. Always non-null; empty when the guardrail has nothing to report. Keys are
     * guardrail-specific — e.g. {@code entityTypes} for PII, {@code providerTypes} for secret keys.
     *
     * <p>
     * <b>Immutability contract:</b> the returned map is immutable (defensively copied via {@link Map#copyOf} in the
     * record compact constructor); mutation attempts throw {@link UnsupportedOperationException}. Never {@code null}.
     *
     * <p>
     * Safe to serialize: internal preflight-only keys (tracked in package-private {@link ViolationInfo}) are stripped
     * at construction, so this map never carries preflight-internal artefacts (like {@code maskEntities}) regardless of
     * what the caller passed in. Subtypes cannot weaken this guarantee because the filtering happens in the record
     * compact constructors.
     *
     * <p>
     * <b>Do NOT put raw user content here.</b> The check advisor reduces {@link PatternViolation#matchedSubstrings} to
     * {@code matchCount} in its public view, but copies {@code info} verbatim; placing matched values in {@code info}
     * bypasses that scrubbing and leaks sensitive data through the metadata channel. Report counts, categories, or type
     * labels instead.
     */
    Map<String, Object> info();

    static Violation ofMatch(String guardrail, String matchedSubstring) {
        if (matchedSubstring == null) {
            throw new IllegalArgumentException("matchedSubstring must not be null; use ofMatches with an empty list if "
                + "no concrete substring is known");
        }

        return new PatternViolation(guardrail, List.of(matchedSubstring));
    }

    static Violation ofMatches(String guardrail, List<String> matchedSubstrings) {
        if (matchedSubstrings == null) {
            throw new IllegalArgumentException(
                "matchedSubstrings must not be null; pass List.of() to represent \"no concrete substring\"");
        }

        return new PatternViolation(guardrail, matchedSubstrings);
    }

    static Violation ofMatches(String guardrail, List<String> matchedSubstrings, Map<String, Object> info) {
        if (matchedSubstrings == null) {
            throw new IllegalArgumentException(
                "matchedSubstrings must not be null; pass List.of() to represent \"no concrete substring\"");
        }

        return new PatternViolation(guardrail, matchedSubstrings, info);
    }

    static Violation ofClassification(String guardrail, double confidenceScore) {
        return new ClassifiedViolation(guardrail, confidenceScore);
    }

    static Violation ofClassification(String guardrail, double confidenceScore, Map<String, Object> info) {
        return new ClassifiedViolation(guardrail, confidenceScore, info);
    }

    static Violation ofExecutionFailure(String guardrail, Throwable cause) {
        return new ExecutionFailureViolation(guardrail, cause);
    }

    // matchedSubstrings and info are both defensively copied via List.copyOf / ViolationInfo.sanitize(Map.copyOf);
    // SpotBugs can't see through those so the record accessor still shows as a potential exposure. The suppression is
    // correct for this concrete record.
    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    record PatternViolation(String guardrail, List<String> matchedSubstrings, Map<String, Object> info)
        implements Violation {

        public PatternViolation(String guardrail, List<String> matchedSubstrings) {
            this(guardrail, matchedSubstrings, Map.of());
        }

        public PatternViolation {
            if (guardrail == null || guardrail.isBlank()) {
                throw new IllegalArgumentException("guardrail must be non-blank");
            }

            if (matchedSubstrings == null) {
                throw new IllegalArgumentException(
                    "matchedSubstrings must not be null; pass List.of() to represent \"no concrete substring\"");
            }

            matchedSubstrings = List.copyOf(matchedSubstrings);
            info = ViolationInfo.sanitize(info);
        }
    }

    // info is defensively copied via ViolationInfo.sanitize(Map.copyOf); SpotBugs can't see through Map.copyOf so the
    // record accessor still shows as a potential exposure. Suppression is correct here.
    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    record ClassifiedViolation(String guardrail, double confidenceScore, Map<String, Object> info)
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

            info = ViolationInfo.sanitize(info);
        }
    }

    // Throwable is inherently mutable (stack trace, suppressed exceptions) and cannot be defensively copied, so the
    // SpotBugs suppression is legitimate here: the exposure IS real, but the caller that produced the Throwable is the
    // only realistic source of mutation and any ordering concerns are already caller-local.
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
        public Map<String, Object> info() {
            return Map.of();
        }
    }
}
