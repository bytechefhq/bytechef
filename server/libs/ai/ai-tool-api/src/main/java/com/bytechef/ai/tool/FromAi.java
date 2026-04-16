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

package com.bytechef.ai.tool;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
public class FromAi implements MethodExecutor {

    private static final Set<String> VALID_TYPES = Set.of(
        "STRING", "NUMBER", "INTEGER", "BOOLEAN", "ARRAY", "OBJECT", "DATE", "TIME", "DATE_TIME");

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        if (arguments.length == 0) {
            throw new IllegalArgumentException("fromAi requires at least a name argument.");
        }

        if (arguments.length > 5) {
            throw new IllegalArgumentException(
                "fromAi accepts at most 5 arguments (name, type, description, default, options).");
        }

        Object nameArgument = arguments[0];

        if (!(nameArgument instanceof String string) || StringUtils.isBlank(string)) {
            throw new IllegalArgumentException("fromAi name argument must be a non-empty String.");
        }

        String name = sanitizeName(string.trim());

        String type = "STRING";

        if (arguments.length > 1) {
            Object typeArgument = arguments[1];

            if (typeArgument != null && !(typeArgument instanceof String)) {
                throw new IllegalArgumentException("fromAi type argument must be a String or null.");
            }

            String typeCandidate = (String) typeArgument;

            String trimmedTypeCandidate = typeCandidate != null ? typeCandidate.trim()
                .toUpperCase() : null;

            if (trimmedTypeCandidate != null && !trimmedTypeCandidate.isEmpty()) {
                if (!VALID_TYPES.contains(trimmedTypeCandidate)) {
                    throw new IllegalArgumentException(
                        "fromAi type argument must be one of " + VALID_TYPES + ", got: '" + trimmedTypeCandidate +
                            "'.");
                }

                type = trimmedTypeCandidate;
            }
        }

        String description = null;
        Object defaultValue = null;
        List<Object> options = null;
        boolean required = false;

        if (arguments.length > 2) {
            Object thirdArgument = arguments[2];

            if (thirdArgument instanceof Map<?, ?> parameterMap) {
                Object descArg = parameterMap.get("description");

                if (descArg != null && !(descArg instanceof String)) {
                    throw new IllegalArgumentException("fromAi 'description' in map must be a String or null.");
                }

                description = (String) descArg;
                defaultValue = parameterMap.get("defaultValue");

                Object optionsArg = parameterMap.get("options");

                if (optionsArg != null && !(optionsArg instanceof List)) {
                    throw new IllegalArgumentException("fromAi 'options' in map must be a List or null.");
                }

                if (optionsArg != null) {
                    @SuppressWarnings("unchecked")
                    List<Object> castedOptions = (List<Object>) optionsArg;

                    options = castedOptions;
                }

                Object requiredArg = parameterMap.get("required");

                if (requiredArg != null && !(requiredArg instanceof Boolean)) {
                    throw new IllegalArgumentException("fromAi 'required' in map must be a boolean.");
                }

                if (requiredArg != null) {
                    required = (Boolean) requiredArg;
                }
            }
        }

        return new TypedValue(new FromAiResult(name, type, description, defaultValue, options, required));
    }

    private static String sanitizeName(String name) {
        String sanitized = name.replaceAll("[^a-zA-Z0-9_.\\-]", "_");

        return sanitized.length() > 64 ? sanitized.substring(0, 64) : sanitized;
    }
}
