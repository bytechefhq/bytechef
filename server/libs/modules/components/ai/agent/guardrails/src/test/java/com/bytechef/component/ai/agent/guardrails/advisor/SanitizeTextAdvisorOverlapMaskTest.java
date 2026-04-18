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

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightSanitizerFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins overlap-masking on the sanitize side: when multiple preflight sanitizers declare {@code preflightMaskEntities},
 * the advisor merges them and applies replacement longest-first so overlapping matches from different sanitizers don't
 * leave stray fragments.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextAdvisorOverlapMaskTest {

    @Test
    void longerOverlappingEntityWinsRegardlessOfRegistrationOrder() {
        PreflightSanitizerFunction piiEntity = new PreflightSanitizerFunction() {

            @Override
            public String apply(String text, GuardrailContext context) {
                // Advisor routes masking via mask() for PreflightMasking sanitizers — Unchanged would skip this
                // sanitizer entirely; return the text as-is so direct callers still observe the expected contract.
                return text;
            }

            @Override
            public MaskResult mask(String text, GuardrailContext context) {
                return MaskResult.entities(Map.of("EMAIL", List.of("alice@corp.com")));
            }
        };

        PreflightSanitizerFunction urlEntity = new PreflightSanitizerFunction() {

            @Override
            public String apply(String text, GuardrailContext context) {
                return text;
            }

            @Override
            public MaskResult mask(String text, GuardrailContext context) {
                return MaskResult.entities(Map.of("URL", List.of("corp.com")));
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            // URLs first on purpose — the longest-first contract must not depend on order.
            .add("urls", urlEntity, empty, empty, empty, Map.of(), null)
            .add("pii", piiEntity, empty, empty, empty, Map.of(), null)
            .build();

        String result = advisor.sanitiseForTesting("ping alice@corp.com now");

        assertThat(result).isEqualTo("ping <EMAIL> now");
    }
}
