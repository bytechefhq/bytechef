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

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RegexParserUtilsTest {

    @Test
    void testParsesLiteralWithCaseInsensitiveFlag() {
        Pattern pattern = RegexParserUtils.compile("/hello/i");

        assertThat(pattern.matcher("HELLO")
            .find()).isTrue();
    }

    @Test
    void testParsesLiteralWithoutFlagsCaseSensitive() {
        Pattern pattern = RegexParserUtils.compile("/hello/");

        assertThat(pattern.matcher("HELLO")
            .find()).isFalse();
    }

    @Test
    void testCompilesBareRegexAsCaseSensitiveByDefault() {
        Pattern pattern = RegexParserUtils.compile("hello");

        assertThat(pattern.matcher("HELLO")
            .find()).isFalse();
    }

    @Test
    void testTranslatesDotallFlag() {
        Pattern pattern = RegexParserUtils.compile("/a.b/s");

        assertThat(pattern.matcher("a\nb")
            .find()).isTrue();
    }

    @Test
    void testRejectsUnknownFlag() {
        assertThatThrownBy(() -> RegexParserUtils.compile("/hello/zx"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAcceptsAndIgnoresGlobalFlag() {
        Pattern pattern = RegexParserUtils.compile("/hello/gi");

        assertThat(pattern.matcher("HELLO")
            .find()).isTrue();
    }

    @Test
    void testRejectsNullOrEmpty() {
        assertThatThrownBy(() -> RegexParserUtils.compile(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RegexParserUtils.compile("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBoundedAbortsCatastrophicBacktracking() {
        // Cox-style exponential backtracking: the regex a?{N} a{N} matched against N 'a's forces the engine to try
        // every possible split of the optional group, which grows as 2^N. Java's regex engine does NOT short-circuit
        // this pattern (unlike simpler (a+)+$ cases), so charAt calls reliably exceed the 10M bound.
        int n = 25;

        StringBuilder patternSource = new StringBuilder();

        for (int index = 0; index < n; index++) {
            patternSource.append("a?");
        }

        for (int index = 0; index < n; index++) {
            patternSource.append("a");
        }

        Pattern pathological = Pattern.compile(patternSource.toString());
        String evil = "a".repeat(n);

        assertThatThrownBy(() -> pathological.matcher(RegexParserUtils.bounded(evil))
            .matches())
                .isInstanceOf(RegexParserUtils.RegexExecutionLimitException.class)
                .hasMessageContaining("character accesses");
    }

    @Test
    void testBoundedRejectsInputLargerThanMaxLength() {
        String oversized = "a".repeat(RegexParserUtils.MAX_INPUT_LENGTH + 1);

        assertThatThrownBy(() -> RegexParserUtils.bounded(oversized))
            .isInstanceOf(RegexParserUtils.RegexExecutionLimitException.class)
            .hasMessageContaining("maximum regex scan length");
    }

    @Test
    void testBoundedAllowsOrdinaryMatching() {
        Pattern pattern = Pattern.compile("\\bfoo\\b");
        CharSequence bounded = RegexParserUtils.bounded("lots of foo in the text");

        assertThat(pattern.matcher(bounded)
            .find()).isTrue();
    }

    @Test
    void testBoundedReturnsNullForNullInput() {
        assertThat(RegexParserUtils.bounded(null)).isNull();
    }

    @Test
    void testCompileRejectsExpressionLargerThanMaxLength() {
        String oversizedPattern = "a".repeat(RegexParserUtils.MAX_EXPRESSION_LENGTH + 1);

        assertThatThrownBy(() -> RegexParserUtils.compile(oversizedPattern))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expression exceeds max length");
    }

    @Test
    void testCompileAcceptsExpressionAtMaxLengthBoundary() {
        String maxSizedPattern = "a".repeat(RegexParserUtils.MAX_EXPRESSION_LENGTH);

        Pattern pattern = RegexParserUtils.compile(maxSizedPattern);

        assertThat(pattern).isNotNull();
    }
}
