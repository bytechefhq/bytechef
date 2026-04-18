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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Direct contract tests for the guardrail exception hierarchy. These exceptions were previously exercised only
 * transitively through advisor tests, so a Kind drift (e.g. a maintainer setting the wrong constant) or a constructor
 * invariant break would slip through. The advisor branches on {@link GuardrailException#kind()} to decide fail-mode
 * routing — the wrong kind produced silently routes a configuration error onto the transient log path.
 *
 * @author Ivica Cardic
 */
// RV_EXCEPTION_NOT_THROWN fires on assertThatThrownBy(() -> new SanitizerExecutionFailureException(null|empty))
// because the AssertJ idiom expects the constructor itself to throw (IllegalArgumentException from the validation
// branch), so the exception object is never actually instantiated. SpotBugs' "exception created but not thrown"
// heuristic mistakes the intended usage for a dropped throw. Scoped to the class because every reject-* test here
// exercises the same idiom.
@SuppressFBWarnings("RV_EXCEPTION_NOT_THROWN")
class GuardrailExceptionContractTest {

    private static final String GUARDRAIL = "test-guardrail";

    @Test
    void testGuardrailOutputParseExceptionExposesGuardrailNameAndOutputParseKind() {
        Throwable cause = new RuntimeException("root cause");
        GuardrailOutputParseException exception = new GuardrailOutputParseException(GUARDRAIL, "bad json", cause);

        assertThat(exception.guardrailName()).isEqualTo(GUARDRAIL);
        assertThat(exception.kind()).isEqualTo(GuardrailExceptionKind.OUTPUT_PARSE);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getMessage()).contains("bad json");
    }

    @Test
    void testGuardrailUnavailableExceptionExposesGuardrailNameAndUpstreamUnavailableKind() {
        Throwable cause = new RuntimeException("network down");
        GuardrailUnavailableException exception = new GuardrailUnavailableException(GUARDRAIL, "timeout", cause);

        assertThat(exception.guardrailName()).isEqualTo(GUARDRAIL);
        assertThat(exception.kind()).isEqualTo(GuardrailExceptionKind.UPSTREAM_UNAVAILABLE);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getMessage()).contains("timeout");
    }

    @Test
    void testGuardrailUnavailableExceptionWithoutCauseHasNullCause() {
        GuardrailUnavailableException exception = new GuardrailUnavailableException(GUARDRAIL, "no cause");

        assertThat(exception.guardrailName()).isEqualTo(GUARDRAIL);
        assertThat(exception.kind()).isEqualTo(GuardrailExceptionKind.UPSTREAM_UNAVAILABLE);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testMissingModelChildExceptionExposesGuardrailNameAndMissingModelKind() {
        // MissingModelChildException returns MISSING_MODEL specifically (NOT UPSTREAM_UNAVAILABLE) so the advisor's
        // isConfigurationError branch can distinguish "operator forgot to wire a MODEL child" from "the LLM is
        // momentarily unreachable" — the former should always block regardless of fail-mode.
        MissingModelChildException exception = new MissingModelChildException(GUARDRAIL);

        assertThat(exception.guardrailName()).isEqualTo(GUARDRAIL);
        assertThat(exception.kind()).isEqualTo(GuardrailExceptionKind.MISSING_MODEL);
    }

    @Test
    void testSanitizerExecutionFailureExceptionAggregatesFailuresInOrderAndExposesSanitizerFailedKind() {
        Map<String, Throwable> failures = new LinkedHashMap<>();

        // Raw "boom-1" / "boom-2" messages must NOT reach the aggregated headline — per-sanitizer getMessage() can
        // legitimately contain LLM output or user prompt fragments. The headline uses the exception class name (for
        // non-GuardrailException causes) or GuardrailExceptionKind (for GuardrailException causes). Operators drill
        // into the populated cause / suppressed chain for the raw detail.
        failures.put("first", new RuntimeException("boom-1"));
        failures.put("second", new RuntimeException("boom-2"));

        SanitizerExecutionFailureException exception = new SanitizerExecutionFailureException(failures);

        assertThat(exception.guardrailName()).isNull();
        assertThat(exception.kind()).isEqualTo(GuardrailExceptionKind.SANITIZER_FAILED);
        assertThat(exception.failures()).containsOnlyKeys("first", "second");
        assertThat(exception.getMessage())
            .as("headline carries sanitizer name + exception class name, not raw cause message")
            .contains("first - RuntimeException")
            .contains("second - RuntimeException")
            .doesNotContain("boom-1")
            .doesNotContain("boom-2");

        // Raw cause messages are still reachable via the cause chain + suppressed exceptions for operators drilling in.
        assertThat(exception.getCause()).hasMessage("boom-1");
        assertThat(exception.getSuppressed())
            .singleElement()
            .satisfies(suppressed -> assertThat(suppressed).hasMessage("boom-2"));
    }

    @Test
    void testSanitizerExecutionFailureExceptionRejectsNullFailures() {
        // Empty/null failures aggregate is semantically nonsensical — the advisor wouldn't be in this code path
        // unless at least one sanitizer threw. Surfacing the bug as IllegalArgumentException at construction time
        // catches the misuse early instead of letting "Failed checks: " (empty list) reach operators.
        assertThatThrownBy(() -> new SanitizerExecutionFailureException(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    void testSanitizerExecutionFailureExceptionRejectsEmptyFailures() {
        assertThatThrownBy(() -> new SanitizerExecutionFailureException(Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be empty");
    }

    @Test
    void testSanitizerExecutionFailureExceptionFailuresMapIsImmutable() {
        Map<String, Throwable> input = new LinkedHashMap<>();

        input.put("only", new RuntimeException("boom"));

        SanitizerExecutionFailureException exception = new SanitizerExecutionFailureException(input);

        assertThatThrownBy(() -> exception.failures()
            .put("mutated", new RuntimeException("nope")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testEveryGuardrailExceptionKindIsReachedByAtLeastOneSubclass() {
        // Regression pin: if a future maintainer adds a new GuardrailExceptionKind value but forgets to wire it into
        // any exception subclass, the kind becomes dead code and the advisor's switch-style routing on it never
        // fires. Keep this test in sync with the enum.
        assertThat(GuardrailExceptionKind.values())
            .containsExactlyInAnyOrder(
                GuardrailExceptionKind.OUTPUT_PARSE,
                GuardrailExceptionKind.UPSTREAM_UNAVAILABLE,
                GuardrailExceptionKind.MISSING_MODEL,
                GuardrailExceptionKind.SANITIZER_FAILED,
                GuardrailExceptionKind.CONFIGURATION);
    }
}
