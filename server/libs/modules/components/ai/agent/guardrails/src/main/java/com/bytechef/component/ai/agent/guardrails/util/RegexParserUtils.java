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

import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public final class RegexParserUtils {

    /**
     * Maximum length (characters) of a regex expression accepted by {@link #compile(String)}. Realistic user-supplied
     * patterns are well below 1 KiB; a compile-time DoS pattern tends to be long due to deeply nested alternation.
     */
    public static final int MAX_EXPRESSION_LENGTH = 4_096;

    private RegexParserUtils() {
    }

    public static Pattern compile(String expression) {
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("expression must be non-empty");
        }

        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException(
                "expression exceeds max length " + MAX_EXPRESSION_LENGTH + " (got " + expression.length() + ")");
        }

        if (expression.charAt(0) != '/') {
            return Pattern.compile(expression);
        }

        int lastSlash = expression.lastIndexOf('/');

        if (lastSlash <= 0) {
            throw new IllegalArgumentException("missing closing '/' in: " + expression);
        }

        String body = expression.substring(1, lastSlash);

        if (body.isEmpty()) {
            throw new IllegalArgumentException("empty regex body in: " + expression);
        }

        String flags = expression.substring(lastSlash + 1);
        int flagBits = 0;

        for (int index = 0; index < flags.length(); index++) {
            char character = flags.charAt(index);

            switch (character) {
                case 'i' -> flagBits |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                case 'm' -> flagBits |= Pattern.MULTILINE;
                case 's' -> flagBits |= Pattern.DOTALL;
                case 'u' -> flagBits |= Pattern.UNICODE_CHARACTER_CLASS;
                case 'x' -> flagBits |= Pattern.COMMENTS;
                case 'g' -> {
                    // 'g' (JS global flag) is accepted for compatibility but has no Java Pattern equivalent.
                    continue;
                }
                default -> throw new IllegalArgumentException("unsupported flag: " + character);
            }
        }

        return Pattern.compile(body, flagBits);
    }
}
