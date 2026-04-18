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

package com.bytechef.component.ai.agent.guardrails.secretkeys.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class SecretKeysTest {

    @Test
    void testCheckFindsAwsKey() throws Exception {
        GuardrailCheckFunction function =
            (GuardrailCheckFunction) SecretKeys.ofCheck()
                .getElement();

        Optional<Violation> violation = function.apply(
            "use AKIAIOSFODNN7EXAMPLE to sign",
            contextOf(Map.of("permissiveness", "PERMISSIVE")));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("AKIAIOSFODNN7EXAMPLE");
    }

    @Test
    void testPatternViolationCarriesEveryMatchAndProviderTypeInfo() throws Exception {
        GuardrailCheckFunction function = (GuardrailCheckFunction) SecretKeys.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "first AKIAIOSFODNN7EXAMPLE then AKIAIOSFODNN7ANOTHER end",
            contextOf(Map.of("permissiveness", "PERMISSIVE")));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("AKIAIOSFODNN7EXAMPLE", "AKIAIOSFODNN7ANOTHER");
        @SuppressWarnings("unchecked")
        List<String> providerTypes = (List<String>) violation.get()
            .info()
            .get("providerTypes");

        assertThat(providerTypes).contains("AWS_ACCESS_KEY", "PREFIXED_SECRET");
    }

    @Test
    void testSanitizeMasksKey() throws Exception {
        GuardrailSanitizerFunction function =
            (GuardrailSanitizerFunction) SecretKeys.ofSanitize()
                .getElement();

        String sanitized = function.apply(
            "use AKIAIOSFODNN7EXAMPLE to sign",
            contextOf(Map.of("permissiveness", "PERMISSIVE")));

        assertThat(sanitized).isEqualTo("use <AWS_ACCESS_KEY> to sign");
    }

    @Test
    void testSanitizeMaskGroupsEntitiesByProviderType() {
        PreflightSanitizerFunction function =
            (PreflightSanitizerFunction) SecretKeys.ofSanitize()
                .getElement();

        MaskResult result = function.mask(
            "use AKIAIOSFODNN7EXAMPLE for AWS",
            contextOf(Map.of("permissiveness", "PERMISSIVE")));

        assertThat(result).isInstanceOf(MaskResult.Entities.class);

        Map<String, List<String>> entities = ((MaskResult.Entities) result).entities();

        assertThat(entities).containsKey("AWS_ACCESS_KEY");
        assertThat(entities.get("AWS_ACCESS_KEY")).contains("AKIAIOSFODNN7EXAMPLE");
    }

    @Test
    void testUnknownPermissivenessThrows() {
        GuardrailCheckFunction function = (GuardrailCheckFunction) SecretKeys.ofCheck()
            .getElement();

        assertThatThrownBy(
            () -> function.apply("text", contextOf(Map.of("permissiveness", "BOGUS"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOGUS")
                .hasMessageContaining("STRICT");
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
