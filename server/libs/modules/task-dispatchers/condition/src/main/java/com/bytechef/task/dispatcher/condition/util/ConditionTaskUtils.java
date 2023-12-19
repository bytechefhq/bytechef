/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.task.dispatcher.condition.util;

import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.COMBINE_OPERATION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.RAW_EXPRESSION;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Matija Petanjek
 */
public class ConditionTaskUtils {

    private static final ExpressionParser expressionParser = new SpelExpressionParser();

    public static boolean resolveCase(TaskExecution conditionTaskExecution) {
        Boolean result;

        if (MapUtils.getBoolean(conditionTaskExecution.getParameters(), RAW_EXPRESSION, false)) {
            result = expressionParser
                .parseExpression(MapUtils.getString(conditionTaskExecution.getParameters(), EXPRESSION))
                .getValue(Boolean.class);
        } else {
            List<Map<String, Map<String, ?>>> conditions = MapUtils.getList(
                conditionTaskExecution.getParameters(), ConditionTaskDispatcherConstants.CONDITIONS,
                new TypeReference<>() {}, Collections.emptyList());
            String combineOperation = MapUtils.getRequiredString(
                conditionTaskExecution.getParameters(), COMBINE_OPERATION);

            result = expressionParser
                .parseExpression(String.join(getBooleanOperator(combineOperation), getConditionExpressions(conditions)))
                .getValue(Boolean.class);
        }

        return result != null && result;
    }

    private static List<String> getConditionExpressions(List<Map<String, Map<String, ?>>> conditions) {
        List<String> conditionExpressions = new ArrayList<>();

        for (Map<String, Map<String, ?>> condition : conditions) {
            for (String operandType : condition.keySet()) {
                Map<String, ?> conditionParts = MapUtils.getMap(condition, operandType);

                String conditionTemplate = conditionTemplates
                    .get(operandType)
                    .get(MapUtils.getRequiredString(conditionParts, ConditionTaskDispatcherConstants.OPERATION));

                conditionExpressions.add(
                    conditionTemplate
                        .replace(
                            "${value1}",
                            MapUtils.getRequiredString(conditionParts, ConditionTaskDispatcherConstants.VALUE_1))
                        .replace(
                            "${value2}",
                            MapUtils.getRequiredString(conditionParts, ConditionTaskDispatcherConstants.VALUE_2)));
            }
        }

        return conditionExpressions;
    }

    private static String getBooleanOperator(String combineOperation) {
        if (combineOperation.equalsIgnoreCase(ConditionTaskDispatcherConstants.CombineOperation.ANY.name())) {
            return "||";
        } else if (combineOperation.equalsIgnoreCase(ConditionTaskDispatcherConstants.CombineOperation.ALL.name())) {
            return "&&";
        }

        throw new IllegalArgumentException("Invalid combine operation: " + combineOperation);
    }

    private static final Map<String, Map<String, String>> conditionTemplates = new HashMap<>();

    static {
        conditionTemplates.put(
            ConditionTaskDispatcherConstants.BOOLEAN,
            Map.ofEntries(
                Map.entry(ConditionTaskDispatcherConstants.Operation.EQUALS.name(), "${value1} == ${value2}"),
                Map.entry(ConditionTaskDispatcherConstants.Operation.NOT_EQUALS.name(), "${value1} != ${value2}")));

        conditionTemplates.put(
            ConditionTaskDispatcherConstants.DATE_TIME,
            Map.ofEntries(
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.AFTER.name(),
                    "T(java.time.LocalDateTime).parse('${value1}').isAfter(T(java.time.LocalDateTime).parse('${value2}'))"),
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.BEFORE.name(),
                    "T(java.time.LocalDateTime).parse('${value1}').isBefore(T(java.time.LocalDateTime).parse('${value2}'))")));

        conditionTemplates.put(
            ConditionTaskDispatcherConstants.NUMBER,
            Map.ofEntries(
                Map.entry(ConditionTaskDispatcherConstants.Operation.EQUALS.name(), "${value1} == ${value2}"),
                Map.entry(ConditionTaskDispatcherConstants.Operation.NOT_EQUALS.name(), "${value1} != ${value2}"),
                Map.entry(ConditionTaskDispatcherConstants.Operation.GREATER.name(), "${value1} > ${value2}"),
                Map.entry(ConditionTaskDispatcherConstants.Operation.LESS.name(), "${value1} < ${value2}"),
                Map.entry(ConditionTaskDispatcherConstants.Operation.GREATER_EQUALS.name(), "${value1} >= ${value2}"),
                Map.entry(ConditionTaskDispatcherConstants.Operation.LESS_EQUALS.name(), "${value1} <= ${value2}")));

        conditionTemplates.put(
            ConditionTaskDispatcherConstants.STRING,
            Map.ofEntries(
                Map.entry(ConditionTaskDispatcherConstants.Operation.EQUALS.name(), "'${value1}'.equals('${value2}')"),
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.NOT_EQUALS.name(),
                    "!'${value1}'.equals('${value2}')"),
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.CONTAINS.name(),
                    "'${value1}'.contains('${value2}')"),
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.NOT_CONTAINS.name(),
                    "!'${value1}'.contains('${value2}')"),
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.STARTS_WITH.name(),
                    "'${value1}'.startsWith('${value2}')"),
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.ENDS_WITH.name(),
                    "'${value1}'.endsWith('${value2}')"),
                Map.entry(ConditionTaskDispatcherConstants.Operation.EMPTY.name(), "'${value1}'.isEmpty()"),
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.REGEX.name(), "'${value1}' matches '${value2}'")));
    }
}
