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

package com.bytechef.component.ai.agent.guardrails.customregex.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.bytechef.component.ai.agent.guardrails.util.RegexParser;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class CustomRegexTest {

    @Test
    void testCheckMatchesCustomPattern() throws Exception {
        GuardrailCheckFunction function =
            (GuardrailCheckFunction) CustomRegex.ofCheck()
                .getElement();

        Optional<Violation> violation = function.apply(
            "order id ORD-1234 shipped",
            contextOf(Map.of("name", "ORDER_ID", "regex", "ORD-\\d{4}")));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("ORD-1234");
    }

    @Test
    void testPatternViolationCarriesEveryMatchAndPatternNameInfo() throws Exception {
        GuardrailCheckFunction function =
            (GuardrailCheckFunction) CustomRegex.ofCheck()
                .getElement();

        Optional<Violation> violation = function.apply(
            "orders ORD-1234 and ORD-5678 shipped",
            contextOf(Map.of("name", "ORDER_ID", "regex", "ORD-\\d{4}")));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("ORD-1234", "ORD-5678");
        assertThat(violation.get()
            .info()).containsEntry("patternNames", List.of("ORDER_ID"));
    }

    @Test
    void testSanitizeUsesNameAsPlaceholder() throws Exception {
        GuardrailSanitizerFunction function =
            (GuardrailSanitizerFunction) CustomRegex.ofSanitize()
                .getElement();

        String sanitized = function.apply(
            "order id ORD-1234 shipped",
            contextOf(Map.of("name", "ORDER_ID", "regex", "ORD-\\d{4}")));

        assertThat(sanitized).isEqualTo("order id [ORDER_ID] shipped");
    }

    @Test
    void testSanitizeQuotesReplacementString() throws Exception {
        // Name contains `$` and `\` which are replacement-string metacharacters. Without Matcher.quoteReplacement,
        // this triggers IllegalArgumentException / IndexOutOfBoundsException or unintended back-references.
        GuardrailSanitizerFunction function =
            (GuardrailSanitizerFunction) CustomRegex.ofSanitize()
                .getElement();

        String sanitized = function.apply(
            "secret ABC",
            contextOf(Map.of("name", "$1\\0", "regex", "ABC")));

        assertThat(sanitized).isEqualTo("secret [$1\\0]");
    }

    @Test
    void testValidateRegexRejectsBadPattern() {
        assertThatThrownBy(() -> CustomRegex.validateRegex("(unclosed"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid regex");
    }

    @Test
    void testValidateRegexAcceptsValidPattern() {
        CustomRegex.validateRegex("[a-z]+");
    }

    @Test
    void testMultipleNamedPatternsAllRunInOneInstance() throws Exception {
        GuardrailCheckFunction function =
            (GuardrailCheckFunction) CustomRegex.ofCheck()
                .getElement();

        Optional<Violation> violation = function.apply(
            "ref MY-INTERNAL-1234 for TCK-987654",
            contextOf(Map.of(
                "patterns", List.of(
                    Map.of("name", "internal-id", "regex", "MY-INTERNAL-\\d{4}"),
                    Map.of("name", "ticket", "regex", "TCK-\\d{6}")))));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("MY-INTERNAL-1234", "TCK-987654");
        assertThat(violation.get()
            .info()).containsEntry("patternNames", List.of("internal-id", "ticket"));
    }

    @Test
    void testMultipleNamedPatternsSanitizeWithEachOwnPlaceholder() throws Exception {
        GuardrailSanitizerFunction function =
            (GuardrailSanitizerFunction) CustomRegex.ofSanitize()
                .getElement();

        String sanitized = function.apply(
            "ref MY-INTERNAL-1234 for TCK-987654",
            contextOf(Map.of(
                "patterns", List.of(
                    Map.of("name", "internal-id", "regex", "MY-INTERNAL-\\d{4}"),
                    Map.of("name", "ticket", "regex", "TCK-\\d{6}")))));

        assertThat(sanitized).isEqualTo("ref [internal-id] for [ticket]");
    }

    @Test
    void testInvalidRegexThrows() {
        GuardrailCheckFunction function = (GuardrailCheckFunction) CustomRegex.ofCheck()
            .getElement();

        assertThatThrownBy(() -> function.apply(
            "text",
            contextOf(Map.of("name", "BAD", "regex", "(unclosed"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid regex");
    }

    /**
     * Builds the {@code a?{n}a{n}} pattern against {@code "a".repeat(n)} — classic Cox-style catastrophic backtracking
     * that reliably blows past {@link RegexParser#MAX_CHAR_ACCESSES}. The JDK regex engine does not short-circuit this,
     * so {@link RegexParser#bounded} is the only thing keeping the matcher from hanging. Matches the construction in
     * {@code PiiDetectorCustomRegexTest.testCatastrophicBacktrackingCustomRegexSurfacesAsRegexExecutionLimit}.
     */
    private static String reDoSPattern(int count) {
        StringBuilder pattern = new StringBuilder();

        for (int index = 0; index < count; index++) {
            pattern.append("a?");
        }

        for (int index = 0; index < count; index++) {
            pattern.append("a");
        }

        return pattern.toString();
    }

    @Test
    void testCheckCatastrophicBacktrackAbortsWithinBudget() {
        GuardrailCheckFunction function = (GuardrailCheckFunction) CustomRegex.ofCheck()
            .getElement();

        int count = 25;
        String pattern = reDoSPattern(count);
        String input = "a".repeat(count);

        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> assertThatThrownBy(
            () -> function.apply(input, contextOf(Map.of("name", "BOOM", "regex", pattern))))
                .isInstanceOf(RegexParser.RegexExecutionLimitException.class)
                .hasMessageContaining("BOOM"));
    }

    @Test
    void testCheckAggregatesBudgetFailuresAcrossEntries() {
        // Two pathological entries: first aborts on budget, second must still run and also abort. The advisor only sees
        // one headline exception, but both entry names must appear (headline + suppressed) so operators can diagnose
        // which entries are bad.
        GuardrailCheckFunction function = (GuardrailCheckFunction) CustomRegex.ofCheck()
            .getElement();

        int count = 25;
        String pattern = reDoSPattern(count);
        String input = "a".repeat(count);

        assertTimeoutPreemptively(Duration.ofSeconds(8), () -> assertThatThrownBy(
            () -> function.apply(
                input,
                contextOf(Map.of(
                    "patterns", List.of(
                        Map.of("name", "FIRST_BAD", "regex", pattern),
                        Map.of("name", "SECOND_BAD", "regex", pattern))))))
                            .isInstanceOf(RegexParser.RegexExecutionLimitException.class)
                            .satisfies(throwable -> {
                                StringBuilder combined = new StringBuilder(throwable.getMessage());

                                for (Throwable suppressed : throwable.getSuppressed()) {
                                    combined.append(' ')
                                        .append(suppressed.getMessage());
                                }

                                String combinedMessage = combined.toString();

                                assertThat(combinedMessage).contains("FIRST_BAD");
                                assertThat(combinedMessage).contains("SECOND_BAD");
                            }));
    }

    @Test
    void testSanitizeCatastrophicBacktrackAbortsWithinBudget() {
        GuardrailSanitizerFunction function = (GuardrailSanitizerFunction) CustomRegex.ofSanitize()
            .getElement();

        int count = 25;
        String pattern = reDoSPattern(count);
        String input = "a".repeat(count);

        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> assertThatThrownBy(
            () -> function.apply(input, contextOf(Map.of("name", "BOOM", "regex", pattern))))
                .isInstanceOf(RegexParser.RegexExecutionLimitException.class)
                .hasMessageContaining("BOOM"));
    }

    private static GuardrailContext contextOf(Map<String, ?> input) {
        return new GuardrailContext(
            ParametersFactory.create(input),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of());
    }
}
