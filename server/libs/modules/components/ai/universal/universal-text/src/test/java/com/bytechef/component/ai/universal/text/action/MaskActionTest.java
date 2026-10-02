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

package com.bytechef.component.ai.universal.text.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class MaskActionTest {

    @Test
    void testMasksSelectedPiiIntoTokensAndReturnsTheMap() {
        Map<String, Object> output = perform(Map.of(
            "text", "mail bob@acme.io or call 555-123-4567",
            "piiDetection", List.of("EMAIL_ADDRESS")));

        String text = (String) output.get("text");
        @SuppressWarnings("unchecked")
        Map<String, String> maskMap = (Map<String, String>) output.get("maskMap");

        assertThat(text).doesNotContain("bob@acme.io")
            .contains("555-123-4567");
        assertThat(maskMap).hasSize(1)
            .containsValue("bob@acme.io");
        assertThat(text).contains(maskMap.keySet()
            .iterator()
            .next());
    }

    @Test
    void testLegacyOptionValuesStillSelectTheirType() {
        Map<String, Object> output = perform(Map.of("text", "mail bob@acme.io", "piiDetection", List.of("EMAIL")));

        assertThat((String) output.get("text")).doesNotContain("bob@acme.io");
    }

    @Test
    void testEmptySelectionScansTheCuratedDefault() {
        Map<String, Object> output = perform(Map.of("text", "mail bob@acme.io on 2026-09-04"));

        assertThat((String) output.get("text")).doesNotContain("bob@acme.io")
            .as("DATE_TIME is outside the curated default")
            .contains("2026-09-04");
    }

    @Test
    void testSelectedWithOnlyUnknownTypesFailsClosed() {
        assertThatThrownBy(() -> perform(Map.of("text", "mail bob@acme.io", "piiDetection", List.of("DOES_NOT_EXIST"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSecretsAreRedactedAndNeverEnterTheMap() {
        Map<String, Object> output = perform(Map.of("text", "key AKIAIOSFODNN7EXAMPLE here"));

        @SuppressWarnings("unchecked")
        Map<String, String> maskMap = (Map<String, String>) output.get("maskMap");

        assertThat((String) output.get("text")).doesNotContain("AKIAIOSFODNN7EXAMPLE")
            .contains("[REDACTED_");
        assertThat(maskMap.values()).noneMatch(value -> value.contains("AKIAIOSFODNN7EXAMPLE"));
    }

    @Test
    void testKeywordsAndCustomPatternsBecomeReversibleSpans() {
        Map<String, Object> output = perform(Map.of(
            "text", "Project Falcon ships order ACME-1234 soon",
            "sensitiveKeywords", List.of("falcon"),
            "customRegexPatterns", List.of("ACME-\\d{4}")));

        @SuppressWarnings("unchecked")
        Map<String, String> maskMap = (Map<String, String>) output.get("maskMap");

        assertThat((String) output.get("text")).doesNotContain("Falcon")
            .doesNotContain("ACME-1234");
        assertThat(maskMap.values()).containsExactlyInAnyOrder("Falcon", "ACME-1234");
        assertThat(maskMap.keySet()).allMatch(token -> token.startsWith("[PII_KEYWORD_")
            || token.startsWith("[PII_CUSTOM_"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> perform(Map<String, Object> input) {
        return (Map<String, Object>) MaskAction.perform(
            ParametersFactory.create(input), ParametersFactory.create(Map.of()), Mockito.mock(ActionContext.class));
    }
}
