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
