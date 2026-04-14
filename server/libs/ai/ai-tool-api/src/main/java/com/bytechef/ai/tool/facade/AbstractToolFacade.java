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

package com.bytechef.ai.tool.facade;

import com.bytechef.ai.tool.FromAiResult;
import com.bytechef.ai.tool.constant.ToolConstants;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.evaluator.Evaluator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Matija Petanjek
 */
public abstract class AbstractToolFacade {

    private final Evaluator evaluator;

    protected AbstractToolFacade(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    protected List<FromAiResult> extractFromAiResults(Map<String, ?> parameters) {
        Map<String, FromAiResult> fromAiResultsByName = new LinkedHashMap<>();

        if (parameters != null) {
            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof FromAiResult fromAiResult) {
                    fromAiResultsByName.putIfAbsent(fromAiResult.name(), fromAiResult);
                } else if (value instanceof Map<?, ?> map) {
                    if (ConvertUtils.canConvert(map, FromAiResult.class)) {
                        FromAiResult fromAiResult = ConvertUtils.convertValue(value, FromAiResult.class);

                        fromAiResultsByName.putIfAbsent(fromAiResult.name(), fromAiResult);
                    } else {
                        extractFromAiResults((Map<String, ?>) map).forEach(
                            fromAiResult -> fromAiResultsByName.putIfAbsent(fromAiResult.name(), fromAiResult));
                    }
                } else if (value instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof String itemExpression && itemExpression.contains("fromAi(")) {
                            for (String fromAiCall : extractFromAiCallStrings(itemExpression)) {
                                FromAiResult fromAiResult = evaluateSingleFromAi(fromAiCall);

                                if (fromAiResult != null) {
                                    fromAiResultsByName.putIfAbsent(fromAiResult.name(), fromAiResult);
                                }
                            }
                        } else if (item instanceof Map<?, ?> itemMap) {
                            if (ConvertUtils.canConvert(itemMap, FromAiResult.class)) {
                                FromAiResult fromAiResult = ConvertUtils.convertValue(item, FromAiResult.class);

                                fromAiResultsByName.putIfAbsent(fromAiResult.name(), fromAiResult);
                            } else {
                                extractFromAiResults((Map<String, ?>) itemMap).forEach(
                                    fromAiResult -> fromAiResultsByName.putIfAbsent(
                                        fromAiResult.name(), fromAiResult));
                            }
                        }
                    }
                } else if (value instanceof String expression && expression.contains("fromAi(")) {
                    for (String fromAiCall : extractFromAiCallStrings(expression)) {
                        FromAiResult fromAiResult = evaluateSingleFromAi(fromAiCall);

                        if (fromAiResult != null) {
                            fromAiResultsByName.putIfAbsent(fromAiResult.name(), fromAiResult);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(fromAiResultsByName.values());
    }

    protected Object resolveParameterValue(Object value, Map<String, Object> request) {
        if (value instanceof FromAiResult fromAiResult) {
            Object requestValue = request.get(fromAiResult.name());

            return requestValue != null ? requestValue : fromAiResult.defaultValue();
        }

        if (value instanceof Map<?, ?> map) {
            if (ConvertUtils.canConvert(map, FromAiResult.class)) {
                FromAiResult fromAiResult = ConvertUtils.convertValue(value, FromAiResult.class);

                Object requestValue = request.get(fromAiResult.name());

                return requestValue != null ? requestValue : fromAiResult.defaultValue();
            }

            Map<String, Object> resolvedMap = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                resolvedMap.put((String) entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            return resolvedMap;
        }

        if (value instanceof List<?> list) {
            List<Object> resolvedList = new ArrayList<>();

            for (Object item : list) {
                resolvedList.add(resolveParameterValue(item, request));
            }

            return resolvedList;
        }

        if (!(value instanceof String expression) || !expression.contains("fromAi(")) {
            return value;
        }

        List<String> fromAiCalls = extractFromAiCallStrings(expression);

        if (fromAiCalls.isEmpty()) {
            return value;
        }

        // Pure fromAi expression — direct mapping preserves type fidelity for non-string types

        String strippedExpression = expression.startsWith("=")
            ? StringUtils.trim(expression.substring(1)) : expression.trim();

        if (fromAiCalls.size() == 1 && strippedExpression.equals(fromAiCalls.getFirst())) {
            FromAiResult fromAiResult = evaluateSingleFromAi(fromAiCalls.getFirst());

            if (fromAiResult != null) {
                Object requestValue = request.get(fromAiResult.name());

                return requestValue != null ? requestValue : fromAiResult.defaultValue();
            }

            return value;
        }

        // Composite expression — replace all fromAi() calls with resolved values and re-evaluate

        String resolvedExpression = expression;

        for (String fromAiCall : fromAiCalls) {
            FromAiResult fromAiResult = evaluateSingleFromAi(fromAiCall);

            if (fromAiResult != null) {
                Object requestValue = request.get(fromAiResult.name());

                Object resolvedValue = requestValue != null ? requestValue : fromAiResult.defaultValue();

                resolvedExpression = resolvedExpression.replace(fromAiCall, toSpElLiteral(resolvedValue));
            }
        }

        if (!resolvedExpression.startsWith("=")) {
            resolvedExpression = "=" + resolvedExpression;
        }

        Map<String, Object> evaluated = evaluator.evaluate(Map.of("value", resolvedExpression), Map.of());

        return evaluated.get("value");
    }

    protected static @Nullable String getToolDescription(
        Map<String, ?> toolParameters, @Nullable Map<String, ?> toolExtensions) {

        String toolDescription = null;

        if (toolParameters != null) {
            Object description = toolParameters.get(ToolConstants.TOOL_DESCRIPTION);

            if (description instanceof String string && !string.isBlank()) {
                toolDescription = string;
            }
        }

        if (toolDescription == null && toolExtensions != null) {
            Object description = toolExtensions.get(ToolConstants.TOOL_DESCRIPTION);

            if (description instanceof String string && !string.isBlank()) {
                toolDescription = string;
            }
        }

        return toolDescription;
    }

    protected String getToolName(
        String componentName, String clusterElementName, Map<String, ?> inputParameters) {

        if (inputParameters != null) {
            Object toolName = inputParameters.get(ToolConstants.TOOL_NAME);

            if (toolName instanceof String string && !string.isBlank()) {
                return string;
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append(componentName.toUpperCase());
        sb.append("_");

        sb.append(Character.toUpperCase(clusterElementName.charAt(0)));

        for (int i = 1; i < clusterElementName.length(); i++) {
            char c = clusterElementName.charAt(i);

            if (Character.isUpperCase(c)) {
                sb.append('_')
                    .append(c);
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }

        return sb.toString();
    }

    protected static List<String> extractFromAiCallStrings(String expression) {
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

    protected static String toSpElLiteral(Object value) {
        if (value == null) {
            return "null";
        }

        String string = value.toString();

        if (value instanceof Boolean || value instanceof Number) {
            return string;
        }

        return "'" + string.replace("'", "''") + "'";
    }

    @Nullable
    private FromAiResult evaluateSingleFromAi(String fromAiCall) {
        Map<String, Object> evaluated = evaluator.evaluate(Map.of("value", "=" + fromAiCall), Map.of());

        if (evaluated.get("value") instanceof FromAiResult fromAiResult) {
            return fromAiResult;
        }

        return null;
    }
}
