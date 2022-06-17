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

import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.task.dispatcher.if_.IfTaskConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Matija Petanjek
 */
public class IfTaskUtil {

    private static final ExpressionParser expressionParser = new SpelExpressionParser();

    public static boolean resolveCase(TaskExecution ifTask) {
        boolean rawConditions = ifTask.get(IfTaskConstants.PROPERTY_RAW_CONDITIONS, Boolean.class, false);

        if (rawConditions) {
            return ifTask.getBoolean(IfTaskConstants.PROPERTY_CONDITIONS);
        } else {
            List<MapObject> conditions = ifTask.getList(IfTaskConstants.PROPERTY_CONDITIONS, MapObject.class);
            String combineOperation = ifTask.getRequiredString(IfTaskConstants.PROPERTY_COMBINE_OPERATION);

            Boolean result = expressionParser
                    .parseExpression(
                            String.join(getBooleanOperator(combineOperation), getConditionExpressions(conditions)))
                    .getValue(Boolean.class);

            return result != null && result;
        }
    }

    private static List<String> getConditionExpressions(List<MapObject> conditions) {
        List<String> conditionExpressions = new ArrayList<>();

        for (MapObject condition : conditions) {
            for (String operandType : condition.keySet()) {
                MapObject conditionParts = condition.get(operandType, MapObject.class);

                String conditionTemplate = conditionTemplates
                        .get(operandType)
                        .get(conditionParts.getRequiredString(IfTaskConstants.PROPERTY_OPERATION));

                conditionExpressions.add(conditionTemplate
                        .replace("${value1}", conditionParts.getRequiredString(IfTaskConstants.PROPERTY_VALUE_1))
                        .replace("${value2}", conditionParts.getRequiredString(IfTaskConstants.PROPERTY_VALUE_2)));
            }
        }

        return conditionExpressions;
    }

    private static String getBooleanOperator(String combineOperation) {
        if (combineOperation.equalsIgnoreCase(IfTaskConstants.CombineOperation.ANY.name())) {
            return "||";
        } else if (combineOperation.equalsIgnoreCase(IfTaskConstants.CombineOperation.ALL.name())) {
            return "&&";
        }

        throw new IllegalArgumentException("Invalid combine operation: " + combineOperation);
    }

    private static final Map<String, Map<String, String>> conditionTemplates = new HashMap<>();

    static {
        conditionTemplates.put(
                IfTaskConstants.PROPERTY_BOOLEAN,
                Map.ofEntries(
                        Map.entry(IfTaskConstants.Operation.EQUALS.name(), "${value1} == ${value2}"),
                        Map.entry(IfTaskConstants.Operation.NOT_EQUALS.name(), "${value1} != ${value2}")));

        conditionTemplates.put(
                IfTaskConstants.PROPERTY_DATE_TIME,
                Map.ofEntries(
                        Map.entry(
                                IfTaskConstants.Operation.AFTER.name(),
                                "T(java.time.LocalDateTime).parse('${value1}').isAfter(T(java.time.LocalDateTime).parse('${value2}'))"),
                        Map.entry(
                                IfTaskConstants.Operation.BEFORE.name(),
                                "T(java.time.LocalDateTime).parse('${value1}').isBefore(T(java.time.LocalDateTime).parse('${value2}'))")));

        conditionTemplates.put(
                IfTaskConstants.PROPERTY_NUMBER,
                Map.ofEntries(
                        Map.entry(IfTaskConstants.Operation.EQUALS.name(), "${value1} == ${value2}"),
                        Map.entry(IfTaskConstants.Operation.NOT_EQUALS.name(), "${value1} != ${value2}"),
                        Map.entry(IfTaskConstants.Operation.GREATER.name(), "${value1} > ${value2}"),
                        Map.entry(IfTaskConstants.Operation.LESS.name(), "${value1} < ${value2}"),
                        Map.entry(IfTaskConstants.Operation.GREATER_EQUALS.name(), "${value1} >= ${value2}"),
                        Map.entry(IfTaskConstants.Operation.LESS_EQUALS.name(), "${value1} <= ${value2}")));

        conditionTemplates.put(
                IfTaskConstants.PROPERTY_STRING,
                Map.ofEntries(
                        Map.entry(IfTaskConstants.Operation.EQUALS.name(), "'${value1}'.equals('${value2}')"),
                        Map.entry(IfTaskConstants.Operation.NOT_EQUALS.name(), "!'${value1}'.equals('${value2}')"),
                        Map.entry(IfTaskConstants.Operation.CONTAINS.name(), "'${value1}'.contains('${value2}')"),
                        Map.entry(IfTaskConstants.Operation.NOT_CONTAINS.name(), "!'${value1}'.contains('${value2}')"),
                        Map.entry(IfTaskConstants.Operation.STARTS_WITH.name(), "'${value1}'.startsWith('${value2}')"),
                        Map.entry(IfTaskConstants.Operation.ENDS_WITH.name(), "'${value1}'.endsWith('${value2}')"),
                        Map.entry(IfTaskConstants.Operation.EMPTY.name(), "'${value1}'.isEmpty()"),
                        Map.entry(IfTaskConstants.Operation.REGEX.name(), "'${value1}' matches '${value2}'")));
    }
}
