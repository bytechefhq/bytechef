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

package com.bytechef.component.ai.agent.guardrails.pii.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class PiiTest {

    @Test
    void testCheckFindsEmail() throws Exception {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "contact me at user@example.com", contextOf(Map.of("type", "ALL")));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("user@example.com");
    }

    @Test
    void testCheckSelectedFiltersTypes() throws Exception {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "user@example.com", contextOf(Map.of("type", "SELECTED", "entities", List.of("US_SSN"))));

        assertThat(violation).isEmpty();
    }

    @Test
    void testPatternViolationCarriesEveryMatchAndEntityTypeInfo() throws Exception {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply("contact a@b.com or c@d.com", contextOf(Map.of("type", "ALL")));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("a@b.com", "c@d.com");
        assertThat(violation.get()
            .info()).containsEntry("entityTypes", new ArrayList<>(List.of("EMAIL_ADDRESS")));
    }

    @Test
    void testSanitizeApplyMasksInline() throws Exception {
        GuardrailSanitizerFunction function = Pii.ofSanitize()
            .getElement();

        String masked = function.apply("contact me at user@example.com please", contextOf(Map.of("type", "ALL")));

        assertThat(masked).contains("<EMAIL_ADDRESS>");
        assertThat(masked).doesNotContain("user@example.com");
    }

    @Test
    void testSanitizeMaskReturnsTheMaskedTextRatherThanAnEntityMap() {
        PreflightSanitizerFunction function = (PreflightSanitizerFunction) Pii.ofSanitize()
            .getElement();

        MaskResult result = function.mask(
            "email user@example.com and phone 555-123-4567", contextOf(Map.of("type", "ALL")));

        assertThat(result).isInstanceOf(MaskResult.Masked.class);
        assertThat(((MaskResult.Masked) result).text())
            .isEqualTo("email <EMAIL_ADDRESS> and phone <PHONE_NUMBER>");
    }

    @Test
    void testCheckUnionsPublishedSpansWithItsOwnDetection() throws Exception {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        GuardrailContext context = contextOf(Map.of("type", "ALL"))
            .withPublishedInputSpans(List.of(SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 0, 16)));

        Optional<Violation> violation = function.apply("[PII_EMAIL_ADDRESS_1_abcd] or 555-123-4567", context);

        assertThat(violation).isPresent();
        assertThat(violation.get()).isInstanceOf(Violation.SpanViolation.class);
        assertThat(((Violation.SpanViolation) violation.get()).matchCount()).isEqualTo(2);
        assertThat(violation.get()
            .info()).containsEntry("entityTypes", new ArrayList<>(List.of("EMAIL_ADDRESS", "PHONE_NUMBER")));
    }

    @Test
    void testPublishedSpansAreFilteredByTheNodesOwnSelection() throws Exception {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        GuardrailContext context = contextOf(Map.of("type", "SELECTED", "entities", List.of("US_SSN")))
            .withPublishedInputSpans(List.of(SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 0, 16)));

        assertThat(function.apply("[PII_EMAIL_ADDRESS_1_abcd]", context))
            .as("a node that only asked about SSNs does not fire on the floor's e-mail span")
            .isEmpty();
    }

    @Test
    void testTypeAllAcceptsPublishedSpanWithCategoryOutsideTheCatalog() throws Exception {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        GuardrailContext context = contextOf(Map.of("type", "ALL"))
            .withPublishedInputSpans(List.of(SensitiveSpan.of(SensitiveKind.PII, "EMPLOYEE_ID", 0, 5)));

        assertThat(function.apply("EMP12 works here", context)).isPresent();
    }

    @Test
    void testTypeSelectedIgnoresPublishedSpanWithCategoryOutsideTheCatalog() throws Exception {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        GuardrailContext context = contextOf(Map.of("type", "SELECTED", "entities", List.of("US_SSN")))
            .withPublishedInputSpans(List.of(SensitiveSpan.of(SensitiveKind.PII, "EMPLOYEE_ID", 0, 5)));

        assertThat(function.apply("EMP12 works here", context))
            .as("a custom rule's category cannot appear in the picker, so it cannot have been selected")
            .isEmpty();
    }

    @Test
    void testPublishedSecretSpansNeverCountForThePiiCheck() throws Exception {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        GuardrailContext context = contextOf(Map.of("type", "ALL"))
            .withPublishedInputSpans(List.of(SensitiveSpan.of(SensitiveKind.SECRET, "AWS_ACCESS_KEY", 0, 20)));

        assertThat(function.apply("[REDACTED_AWS_ACCESS_KEY]", context)).isEmpty();
    }

    @Test
    void testSanitizeMaskIsUnchangedWhenNoMatches() {
        PreflightSanitizerFunction function = (PreflightSanitizerFunction) Pii.ofSanitize()
            .getElement();

        MaskResult result = function.mask(
            "no sensitive data here",
            contextOf(Map.of("type", "ALL")));

        assertThat(result).isInstanceOf(MaskResult.Unchanged.class);
    }

    @Test
    void testCheckSelectedWithEmptyEntitiesFailsClosed() {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        assertThatThrownBy(
            () -> function.apply("contact me at user@example.com", contextOf(Map.of("type", "SELECTED"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one entity");

        assertThatThrownBy(
            () -> function.apply(
                "contact me at user@example.com",
                contextOf(Map.of("type", "SELECTED", "entities", List.of()))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one entity");
    }

    @Test
    void testCheckSelectedWithOnlyUnknownEntitiesFailsClosed() {
        GuardrailCheckFunction function = Pii.ofCheck()
            .getElement();

        assertThatThrownBy(
            () -> function.apply(
                "contact me at user@example.com",
                contextOf(Map.of("type", "SELECTED", "entities", List.of("DOES_NOT_EXIST")))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one entity");
    }

    @Test
    void testSanitizeMaskSelectedWithEmptyEntitiesFailsClosed() {
        PreflightSanitizerFunction function = (PreflightSanitizerFunction) Pii.ofSanitize()
            .getElement();

        assertThatThrownBy(() -> function.mask("user@example.com", contextOf(Map.of("type", "SELECTED"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one entity");
    }

    private static GuardrailContext contextOf(Map<String, ?> input) {
        return new GuardrailContext(
            ParametersFactory.create(input), ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Map.of(), null, mock(Context.class));
    }
}
