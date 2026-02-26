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

package com.bytechef.component.ai.agent.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for parsing and manipulating {@code fromAi(...)} expressions. Expressions may contain one or more
 * {@code fromAi} calls embedded in larger SpEL expressions (e.g. {@code ='Prefix: ' + fromAi('subject') +
 * fromAi('name')}).
 *
 * @author Ivica Cardic
 */
public class FromAiExpressionUtils {

    /**
     * Extracts all individual {@code fromAi(...)} call substrings from the given expression. The parser tracks
     * parenthesis depth and respects single-quoted string literals so that quoted parentheses are not misinterpreted.
     *
     * @param expression the full expression string (may or may not start with {@code =})
     * @return ordered list of {@code fromAi(...)} call substrings found in the expression
     */
    public static List<String> extractFromAiCallStrings(String expression) {
        List<String> calls = new ArrayList<>();

        String marker = "fromAi(";
        int searchFrom = 0;

        while (searchFrom < expression.length()) {
            int callStart = expression.indexOf(marker, searchFrom);

            if (callStart == -1) {
                break;
            }

            int parenStart = callStart + marker.length() - 1;
            int depth = 1;
            boolean inSingleQuote = false;
            int position = parenStart + 1;

            while (position < expression.length() && depth > 0) {
                char character = expression.charAt(position);

                if (inSingleQuote) {
                    if (character == '\'') {
                        if (position + 1 < expression.length() && expression.charAt(position + 1) == '\'') {
                            position++;
                        } else {
                            inSingleQuote = false;
                        }
                    }
                } else {
                    if (character == '\'') {
                        inSingleQuote = true;
                    } else if (character == '(') {
                        depth++;
                    } else if (character == ')') {
                        depth--;
                    }
                }

                position++;
            }

            if (depth == 0) {
                calls.add(expression.substring(callStart, position));
            }

            searchFrom = position;
        }

        return calls;
    }

    /**
     * Converts a Java value to a SpEL literal string suitable for expression substitution.
     *
     * @param value the value to convert (may be null)
     * @return a SpEL-safe literal representation (e.g. {@code 'hello'}, {@code 42}, {@code true}, {@code null})
     */
    public static String toSpelLiteral(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }

        return "'" + value.toString()
            .replace("'", "''") + "'";
    }

}
