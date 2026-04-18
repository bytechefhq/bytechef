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

import com.bytechef.component.ai.agent.guardrails.util.PiiDetector.PiiMatch;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PiiDetectorCustomRegexTest {

    @Test
    void testCustomRegexAdditionsAreIncludedInDetection() {
        List<Pattern> extra = List.of(Pattern.compile("\\bMY-CUSTOMER-\\d{4}\\b"));

        List<PiiMatch> matches = PiiDetector.detect(
            "ref: MY-CUSTOMER-1234", PiiDetector.DEFAULT_PII_PATTERNS, extra);

        assertThat(matches).extracting(PiiMatch::type)
            .contains("CUSTOM");
        assertThat(matches).extracting(PiiMatch::value)
            .contains("MY-CUSTOMER-1234");
    }

    @Test
    void testCustomRegexNullDoesNotBreakDetection() {
        List<PiiMatch> matches = PiiDetector.detect(
            "Contact: user@example.com", PiiDetector.DEFAULT_PII_PATTERNS, null);

        assertThat(matches).extracting(PiiMatch::type)
            .contains("EMAIL_ADDRESS");
    }

    @Test
    void testCatastrophicBacktrackingCustomRegexSurfacesAsRegexExecutionLimit() {
        // End-to-end ReDoS pin: a user-supplied pattern with catastrophic backtracking must hit the per-pattern
        // bounded() budget and surface RegexExecutionLimitException to the advisor. The advisor then treats it as a
        // configuration error and blocks fail-closed. Without this test a future refactor that routes extraRegexes
        // around RegexParser.bounded would be undetectable at the utility level.
        // Uses the same Cox-style pattern as RegexParserTest.testBoundedAbortsCatastrophicBacktracking: a?{N}a{N}
        // against N 'a's, which the JDK regex engine does NOT short-circuit so charAt accesses reliably blow past
        // the 10M cap.
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

        assertThatThrownBy(() -> PiiDetector.detect(
            pathological, PiiDetector.DEFAULT_PII_PATTERNS, List.of(evil)))
                .isInstanceOf(RegexParser.RegexExecutionLimitException.class)
                .hasMessageContaining("extraRegex[0]");
    }
}
