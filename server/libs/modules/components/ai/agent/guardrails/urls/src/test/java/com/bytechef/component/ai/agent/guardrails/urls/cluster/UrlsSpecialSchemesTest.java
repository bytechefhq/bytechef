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

package com.bytechef.component.ai.agent.guardrails.urls.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins that the URLs guardrail at the cluster layer (not just the detector) flags special schemes ({@code javascript:},
 * {@code data:}, {@code vbscript:}) as violations under the default policy. The detector-level tests cover detection;
 * this test pins blocking through the cluster element so a change to {@code Urls.of(...)} that defaults to permitting
 * these schemes would be caught.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class UrlsSpecialSchemesTest {

    @Test
    void testJavascriptSchemeIsFlaggedUnderDefaultPolicy() throws Exception {
        GuardrailCheckFunction function = Urls.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "click javascript:alert(1) please",
            contextOf(Map.of(
                "allowedUrls", List.of(),
                "allowedSchemes", List.of("https", "http"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(violation)
            .as("javascript: scheme must be flagged when not in allowedSchemes")
            .isPresent();

        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .anySatisfy(substring -> assertThat(substring).contains("javascript:"));
    }

    @Test
    void testDataSchemeIsFlaggedUnderDefaultPolicy() throws Exception {
        GuardrailCheckFunction function = Urls.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "open data:text/html,<script>alert(1)</script>",
            contextOf(Map.of(
                "allowedUrls", List.of(),
                "allowedSchemes", List.of("https", "http"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(violation)
            .as("data: scheme must be flagged when not in allowedSchemes")
            .isPresent();

        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .anySatisfy(substring -> assertThat(substring).contains("data:"));
    }

    @Test
    void testVbscriptSchemeIsFlaggedUnderDefaultPolicy() throws Exception {
        GuardrailCheckFunction function = Urls.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "run vbscript:msgbox(1)",
            contextOf(Map.of(
                "allowedUrls", List.of(),
                "allowedSchemes", List.of("https", "http"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(violation)
            .as("vbscript: scheme must be flagged when not in allowedSchemes")
            .isPresent();
    }

    @Test
    void testJavascriptCanBeAllowlistedExplicitly() throws Exception {
        GuardrailCheckFunction function = Urls.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "see javascript:doThing()",
            contextOf(Map.of(
                "allowedUrls", List.of(),
                "allowedSchemes", List.of("https", "http", "javascript"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(violation)
            .as("explicit allowlist of javascript scheme permits it")
            .isEmpty();
    }

    private static GuardrailContext contextOf(Map<String, ?> input) {
        return new GuardrailContext(
            ParametersFactory.create(input), ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Map.of(), null, mock(Context.class));
    }
}
