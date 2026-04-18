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

import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class PiiTest {

    @Test
    void testCheckFindsEmail() throws Exception {
        GuardrailCheckFunction function = (GuardrailCheckFunction) Pii.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "contact me at user@example.com",
            contextOf(Map.of("type", "ALL")));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("user@example.com");
    }

    @Test
    void testCheckSelectedFiltersTypes() throws Exception {
        GuardrailCheckFunction function = (GuardrailCheckFunction) Pii.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "user@example.com",
            contextOf(Map.of("type", "SELECTED", "entities", List.of("US_SSN"))));

        assertThat(violation).isEmpty();
    }

    @Test
    void testPatternViolationCarriesEveryMatchAndEntityTypeInfo() throws Exception {
        GuardrailCheckFunction function = (GuardrailCheckFunction) Pii.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "contact a@b.com or c@d.com",
            contextOf(Map.of("type", "ALL")));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("a@b.com", "c@d.com");
        assertThat(violation.get()
            .info()).containsEntry("entityTypes", List.of("EMAIL_ADDRESS"));
    }

    @Test
    void testSanitizeReplacesEmail() throws Exception {
        GuardrailSanitizerFunction function =
            (GuardrailSanitizerFunction) Pii.ofSanitize()
                .getElement();

        String sanitized = function.apply(
            "contact me at user@example.com please",
            contextOf(Map.of("type", "ALL")));

        assertThat(sanitized).isEqualTo("contact me at <EMAIL_ADDRESS> please");
    }

    @Test
    void testSanitizeMaskGroupsEntitiesByType() {
        PreflightSanitizerFunction function =
            (PreflightSanitizerFunction) Pii.ofSanitize()
                .getElement();

        MaskResult result = function.mask(
            "email user@example.com and phone 555-123-4567",
            contextOf(Map.of("type", "ALL")));

        assertThat(result).isInstanceOf(MaskResult.Entities.class);

        Map<String, List<String>> entities = ((MaskResult.Entities) result).entities();

        assertThat(entities)
            .containsKey("EMAIL_ADDRESS")
            .containsKey("PHONE_NUMBER");
        assertThat(entities.get("EMAIL_ADDRESS")).contains("user@example.com");
        assertThat(entities.get("PHONE_NUMBER")).contains("555-123-4567");
    }

    @Test
    void testSanitizeMaskIsUnchangedWhenNoMatches() {
        PreflightSanitizerFunction function =
            (PreflightSanitizerFunction) Pii.ofSanitize()
                .getElement();

        MaskResult result = function.mask(
            "no sensitive data here",
            contextOf(Map.of("type", "ALL")));

        assertThat(result).isInstanceOf(MaskResult.Unchanged.class);
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
