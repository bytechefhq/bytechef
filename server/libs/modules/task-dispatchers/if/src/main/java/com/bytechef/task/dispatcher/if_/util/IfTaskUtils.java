
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.dispatcher.if_.util;

import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.COMBINE_OPERATION;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.RAW_EXPRESSION;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.commons.utils.MapUtils;
import com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants;
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
public class IfTaskUtils {

    private static final ExpressionParser expressionParser = new SpelExpressionParser();

    public static boolean resolveCase(TaskExecution ifTaskExecution) {
        Boolean result;

        if (MapUtils.getBoolean(ifTaskExecution.getParameters(), RAW_EXPRESSION, false)) {
            result = expressionParser
                .parseExpression(MapUtils.getString(ifTaskExecution.getParameters(), EXPRESSION))
                .getValue(Boolean.class);
        } else {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions = (List) MapUtils.getList(
                ifTaskExecution.getParameters(),
                IfTaskDispatcherConstants.CONDITIONS,
                Map.class,
                Collections.emptyList());
            String combineOperation = MapUtils.getRequiredString(ifTaskExecution.getParameters(), COMBINE_OPERATION);

            result = expressionParser
                .parseExpression(
                    String.join(getBooleanOperator(combineOperation), getConditionExpressions(conditions)))
                .getValue(Boolean.class);
        }

        return result != null && result;
    }

    private static List<String> getConditionExpressions(List<Map<String, Object>> conditions) {
        List<String> conditionExpressions = new ArrayList<>();

        for (Map<String, Object> condition : conditions) {
            for (String operandType : condition.keySet()) {
                Map<String, Object> conditionParts = MapUtils.getMap(condition, operandType);

                String conditionTemplate = conditionTemplates
                    .get(operandType)
                    .get(MapUtils.getRequiredString(conditionParts, IfTaskDispatcherConstants.OPERATION));

                conditionExpressions.add(conditionTemplate
                    .replace(
                        "${value1}",
                        MapUtils.getRequiredString(conditionParts, IfTaskDispatcherConstants.VALUE_1))
                    .replace(
                        "${value2}",
                        MapUtils.getRequiredString(conditionParts, IfTaskDispatcherConstants.VALUE_2)));
            }
        }

        return conditionExpressions;
    }

    private static String getBooleanOperator(String combineOperation) {
        if (combineOperation.equalsIgnoreCase(IfTaskDispatcherConstants.CombineOperation.ANY.name())) {
            return "||";
        } else if (combineOperation.equalsIgnoreCase(IfTaskDispatcherConstants.CombineOperation.ALL.name())) {
            return "&&";
        }

        throw new IllegalArgumentException("Invalid combine operation: " + combineOperation);
    }

    private static final Map<String, Map<String, String>> conditionTemplates = new HashMap<>();

    static {
        conditionTemplates.put(
            IfTaskDispatcherConstants.BOOLEAN,
            Map.ofEntries(
                Map.entry(IfTaskDispatcherConstants.Operation.EQUALS.name(), "${value1} == ${value2}"),
                Map.entry(IfTaskDispatcherConstants.Operation.NOT_EQUALS.name(), "${value1} != ${value2}")));

        conditionTemplates.put(
            IfTaskDispatcherConstants.DATE_TIME,
            Map.ofEntries(
                Map.entry(
                    IfTaskDispatcherConstants.Operation.AFTER.name(),
                    "T(java.time.LocalDateTime).parse('${value1}').isAfter(T(java.time.LocalDateTime).parse('${value2}'))"),
                Map.entry(
                    IfTaskDispatcherConstants.Operation.BEFORE.name(),
                    "T(java.time.LocalDateTime).parse('${value1}').isBefore(T(java.time.LocalDateTime).parse('${value2}'))")));

        conditionTemplates.put(
            IfTaskDispatcherConstants.NUMBER,
            Map.ofEntries(
                Map.entry(IfTaskDispatcherConstants.Operation.EQUALS.name(), "${value1} == ${value2}"),
                Map.entry(IfTaskDispatcherConstants.Operation.NOT_EQUALS.name(), "${value1} != ${value2}"),
                Map.entry(IfTaskDispatcherConstants.Operation.GREATER.name(), "${value1} > ${value2}"),
                Map.entry(IfTaskDispatcherConstants.Operation.LESS.name(), "${value1} < ${value2}"),
                Map.entry(IfTaskDispatcherConstants.Operation.GREATER_EQUALS.name(), "${value1} >= ${value2}"),
                Map.entry(IfTaskDispatcherConstants.Operation.LESS_EQUALS.name(), "${value1} <= ${value2}")));

        conditionTemplates.put(
            IfTaskDispatcherConstants.STRING,
            Map.ofEntries(
                Map.entry(IfTaskDispatcherConstants.Operation.EQUALS.name(), "'${value1}'.equals('${value2}')"),
                Map.entry(
                    IfTaskDispatcherConstants.Operation.NOT_EQUALS.name(),
                    "!'${value1}'.equals('${value2}')"),
                Map.entry(
                    IfTaskDispatcherConstants.Operation.CONTAINS.name(),
                    "'${value1}'.contains('${value2}')"),
                Map.entry(
                    IfTaskDispatcherConstants.Operation.NOT_CONTAINS.name(),
                    "!'${value1}'.contains('${value2}')"),
                Map.entry(
                    IfTaskDispatcherConstants.Operation.STARTS_WITH.name(),
                    "'${value1}'.startsWith('${value2}')"),
                Map.entry(
                    IfTaskDispatcherConstants.Operation.ENDS_WITH.name(),
                    "'${value1}'.endsWith('${value2}')"),
                Map.entry(IfTaskDispatcherConstants.Operation.EMPTY.name(), "'${value1}'.isEmpty()"),
                Map.entry(
                    IfTaskDispatcherConstants.Operation.REGEX.name(), "'${value1}' matches '${value2}'")));
    }
}
