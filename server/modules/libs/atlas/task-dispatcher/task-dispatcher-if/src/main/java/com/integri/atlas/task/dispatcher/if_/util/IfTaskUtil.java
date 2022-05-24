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

package com.integri.atlas.task.dispatcher.if_.util;

import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.CombineOperation;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.Operation;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_BOOLEAN;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_COMBINE_OPERATION;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_CONDITIONS;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_DATE_TIME;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_NUMBER;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_OPERATION;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_RAW_CONDITIONS;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_STRING;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_VALUE_1;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_VALUE_2;

import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.task.execution.TaskExecution;
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
        boolean rawConditions = ifTask.get(PROPERTY_RAW_CONDITIONS, Boolean.class, false);

        if (rawConditions) {
            return ifTask.getBoolean(PROPERTY_CONDITIONS);
        } else {
            List<MapObject> conditions = ifTask.getList(PROPERTY_CONDITIONS, MapObject.class);
            String combineOperation = ifTask.getRequiredString(PROPERTY_COMBINE_OPERATION);

            Boolean result = expressionParser
                .parseExpression(String.join(getBooleanOperator(combineOperation), getConditionExpressions(conditions)))
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
                    .get(conditionParts.getRequiredString(PROPERTY_OPERATION));

                conditionExpressions.add(
                    conditionTemplate
                        .replace("${value1}", conditionParts.getRequiredString(PROPERTY_VALUE_1))
                        .replace("${value2}", conditionParts.getRequiredString(PROPERTY_VALUE_2))
                );
            }
        }

        return conditionExpressions;
    }

    private static String getBooleanOperator(String combineOperation) {
        if (combineOperation.equalsIgnoreCase(CombineOperation.ANY.name())) {
            return "||";
        } else if (combineOperation.equalsIgnoreCase(CombineOperation.ALL.name())) {
            return "&&";
        }

        throw new IllegalArgumentException("Invalid combine operation: " + combineOperation);
    }

    private static final Map<String, Map<String, String>> conditionTemplates = new HashMap<>();

    static {
        conditionTemplates.put(
            PROPERTY_BOOLEAN,
            Map.ofEntries(
                Map.entry(Operation.EQUALS.name(), "${value1} == ${value2}"),
                Map.entry(Operation.NOT_EQUALS.name(), "${value1} != ${value2}")
            )
        );

        conditionTemplates.put(
            PROPERTY_DATE_TIME,
            Map.ofEntries(
                Map.entry(
                    Operation.AFTER.name(),
                    "T(java.time.LocalDateTime).parse('${value1}').isAfter(T(java.time.LocalDateTime).parse('${value2}'))"
                ),
                Map.entry(
                    Operation.BEFORE.name(),
                    "T(java.time.LocalDateTime).parse('${value1}').isBefore(T(java.time.LocalDateTime).parse('${value2}'))"
                )
            )
        );

        conditionTemplates.put(
            PROPERTY_NUMBER,
            Map.ofEntries(
                Map.entry(Operation.EQUALS.name(), "${value1} == ${value2}"),
                Map.entry(Operation.NOT_EQUALS.name(), "${value1} != ${value2}"),
                Map.entry(Operation.GREATER.name(), "${value1} > ${value2}"),
                Map.entry(Operation.LESS.name(), "${value1} < ${value2}"),
                Map.entry(Operation.GREATER_EQUALS.name(), "${value1} >= ${value2}"),
                Map.entry(Operation.LESS_EQUALS.name(), "${value1} <= ${value2}")
            )
        );

        conditionTemplates.put(
            PROPERTY_STRING,
            Map.ofEntries(
                Map.entry(Operation.EQUALS.name(), "'${value1}'.equals('${value2}')"),
                Map.entry(Operation.NOT_EQUALS.name(), "!'${value1}'.equals('${value2}')"),
                Map.entry(Operation.CONTAINS.name(), "'${value1}'.contains('${value2}')"),
                Map.entry(Operation.NOT_CONTAINS.name(), "!'${value1}'.contains('${value2}')"),
                Map.entry(Operation.STARTS_WITH.name(), "'${value1}'.startsWith('${value2}')"),
                Map.entry(Operation.ENDS_WITH.name(), "'${value1}'.endsWith('${value2}')"),
                Map.entry(Operation.EMPTY.name(), "'${value1}'.isEmpty()"),
                Map.entry(Operation.REGEX.name(), "'${value1}' matches '${value2}'")
            )
        );
    }
}
