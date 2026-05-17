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
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextAdvisorTripwireSemanticsTest {

    @Test
    void sanitizeReturnsMaskedTextEvenWhenEverySanitizerFires() {
        GuardrailSanitizerFunction piiMask = (text, context) -> text.replace("a@b.com", "<EMAIL>");

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("pii", piiMask, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        String result = advisor.sanitizeForTesting("reach me at a@b.com");

        assertThat(result).isEqualTo("reach me at <EMAIL>");
    }

    @Test
    void sanitizeLeavesTextUnchangedWhenNoSanitizerFires() {
        // Complement: a non-firing sanitizer must not alter text. Without this, a regression that unconditionally
        // mutates (e.g. normalises whitespace) would slip past the firing-sanitizer test alone.
        GuardrailSanitizerFunction piiMask = (text, context) -> text.replace("a@b.com", "<EMAIL>");

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("pii", piiMask, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        String input = "nothing sensitive here";

        assertThat(advisor.sanitizeForTesting(input)).isEqualTo(input);
    }

    @Test
    void sanitizeChainsMultipleFiringSanitizersLeftToRight() {
        // Two sanitizers, each responsible for a different placeholder, must both apply — the advisor passes the
        // progressively-rewritten text to the next sanitizer rather than running every sanitizer on the original.
        GuardrailSanitizerFunction piiMask = (text, context) -> text.replace("a@b.com", "<EMAIL>");
        GuardrailSanitizerFunction secretMask = (text, context) -> text.replace("AKIAXYZ", "<AWS_KEY>");

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("pii", piiMask, empty, empty, empty, Map.of(), null)
            .add("secretKeys", secretMask, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        String result = advisor.sanitizeForTesting("email a@b.com key AKIAXYZ");

        assertThat(result).isEqualTo("email <EMAIL> key <AWS_KEY>");
    }
}
