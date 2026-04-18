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
class UrlsTest {

    @Test
    void testCheckFlagsNonAllowlistedHost() throws Exception {
        GuardrailCheckFunction function = (GuardrailCheckFunction) Urls.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "visit https://evil.com/page now",
            contextOf(Map.of(
                "allowedUrls", List.of("good.com"),
                "allowedSchemes", List.of("https"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(violation).isPresent();
    }

    @Test
    void testPatternViolationCarriesEveryMatchAndOmitsInfoLeak() throws Exception {
        GuardrailCheckFunction function = (GuardrailCheckFunction) Urls.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "see https://evil.com and https://malicious.org for details",
            contextOf(Map.of(
                "allowedUrls", List.of("good.com"),
                "allowedSchemes", List.of("https"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("https://evil.com", "https://malicious.org");
        // Raw URLs must NOT be duplicated into info: the advisor's public-view projection reduces matchedSubstrings to
        // matchCount, but copies info verbatim, so any raw value placed in info would bypass the scrubbing. Keep this
        // assertion so a future "convenience" info key on urls does not regress the invariant.
        assertThat(violation.get()
            .info()).doesNotContainKey("blocked");
    }

    @Test
    void testCheckAcceptsUppercaseSchemeInAllowlist() throws Exception {
        GuardrailCheckFunction function = (GuardrailCheckFunction) Urls.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "visit https://good.com/page now",
            contextOf(Map.of(
                "allowedUrls", List.of("good.com"),
                "allowedSchemes", List.of("HTTPS"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(violation).isEmpty();
    }

    @Test
    void testSanitizeMasksUrl() throws Exception {
        GuardrailSanitizerFunction function =
            (GuardrailSanitizerFunction) Urls.ofSanitize()
                .getElement();

        String sanitized = function.apply(
            "see https://evil.com/p now",
            contextOf(Map.of(
                "allowedUrls", List.of(),
                "allowedSchemes", List.of("https"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(sanitized).isEqualTo("see <URL> now");
    }

    @Test
    void testSanitizeMaskReturnsBlockedUrlsUnderUrlType() {
        PreflightSanitizerFunction function =
            (PreflightSanitizerFunction) Urls.ofSanitize()
                .getElement();

        MaskResult result = function.mask(
            "visit https://evil.com/p and https://bad.com/x now",
            contextOf(Map.of(
                "allowedUrls", List.of(),
                "allowedSchemes", List.of("https"),
                "blockUserinfo", true,
                "allowSubdomain", true)));

        assertThat(result).isInstanceOf(MaskResult.Entities.class);

        Map<String, List<String>> entities = ((MaskResult.Entities) result).entities();

        assertThat(entities).containsKey("URL");
        assertThat(entities.get("URL")).contains("https://evil.com/p", "https://bad.com/x");
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
