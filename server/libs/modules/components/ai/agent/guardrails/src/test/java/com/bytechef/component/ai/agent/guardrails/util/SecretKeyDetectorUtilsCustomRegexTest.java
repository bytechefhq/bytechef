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

package com.bytechef.component.ai.agent.guardrails.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.Permissiveness;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.SecretMatch;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SecretKeyDetectorUtilsCustomRegexTest {

    @Test
    void testCustomRegexAdditionsAreIncludedInDetection() {
        List<Pattern> extra = List.of(Pattern.compile("\\bMY-INTERNAL-\\d{6}\\b"));

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "leaked: MY-INTERNAL-987654", Permissiveness.PERMISSIVE, extra);

        assertThat(matches).extracting(SecretMatch::type)
            .contains("CUSTOM");
        assertThat(matches).extracting(SecretMatch::value)
            .contains("MY-INTERNAL-987654");
    }

    @Test
    void testCustomRegexNullDoesNotBreakDetection() {
        List<SecretMatch> matches =
            SecretKeyDetectorUtils.detect("AKIA0123456789ABCDEF", Permissiveness.PERMISSIVE, null);

        assertThat(matches).extracting(SecretMatch::type)
            .contains("AWS_ACCESS_KEY");
    }

    @Test
    void testCatastrophicBacktrackingCustomRegexSurfacesAsRegexExecutionLimit() {
        // End-to-end ReDoS pin: a user-supplied pattern with catastrophic backtracking must hit the per-pattern
        // bounded() budget and surface RegexExecutionLimitException to the advisor. The advisor then treats it as a
        // configuration error and blocks fail-closed. PII has a sibling test; this one pins the same guarantee for
        // SecretKeys' extraRegexes pipeline so a future refactor that routes extras around RegexParserUtils.bounded
        // cannot regress silently.
        int n = 25;
        StringBuilder patternSource = new StringBuilder();

        for (int index = 0; index < n; index++) {
            patternSource.append("a?");
        }

        for (int index = 0; index < n; index++) {
            patternSource.append("a");
        }

        Pattern evil = Pattern.compile(patternSource.toString());
        String pathological = "a".repeat(n);

        assertThatThrownBy(() -> SecretKeyDetectorUtils.detect(
            pathological, Permissiveness.BALANCED, List.of(evil)))
                .isInstanceOf(RegexParserUtils.RegexExecutionLimitException.class)
                .hasMessageContaining("extraRegex[0]");
    }
}
