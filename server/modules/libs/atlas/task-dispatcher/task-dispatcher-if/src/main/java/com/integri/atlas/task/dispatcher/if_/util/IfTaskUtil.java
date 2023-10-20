/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.task.dispatcher.if_.util;

import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
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
        List<MapObject> conditions = ifTask.getList("conditions", MapObject.class);
        String combineOperation = ifTask.getRequiredString("combineOperation");

        return expressionParser
            .parseExpression(String.join(getBooleanOperator(combineOperation), getConditionExpressions(conditions)))
            .getValue(Boolean.class);
    }

    private static List<String> getConditionExpressions(List<MapObject> conditions) {
        List<String> conditionExpressions = new ArrayList<>();

        for (MapObject condition : conditions) {
            for (String operandType : condition.keySet()) {
                MapObject conditionParts = condition.get(operandType, MapObject.class);

                String conditionTemplate = conditionTemplates
                    .get(operandType)
                    .get(conditionParts.getRequiredString("operation"));

                conditionExpressions.add(
                    conditionTemplate
                        .replace("${value1}", conditionParts.getRequiredString("value1"))
                        .replace("${value2}", conditionParts.getRequiredString("value2"))
                );
            }
        }

        return conditionExpressions;
    }

    private static String getBooleanOperator(String combineOperation) {
        if (combineOperation.equalsIgnoreCase("ANY")) {
            return "||";
        } else if (combineOperation.equalsIgnoreCase("ALL")) {
            return "&&";
        }

        throw new IllegalArgumentException("Invalid combine operation: " + combineOperation);
    }

    private static final Map<String, Map<String, String>> conditionTemplates = new HashMap<>();

    static {
        conditionTemplates.put(
            "boolean",
            Map.ofEntries(
                Map.entry("equals", "${value1} == ${value2}"),
                Map.entry("notEquals", "${value1} != ${value2}")
            )
        );

        conditionTemplates.put(
            "dateTime",
            Map.ofEntries(
                Map.entry(
                    "after",
                    "T(java.time.LocalDateTime).parse('${value1}').isAfter(T(java.time.LocalDateTime).parse('${value2}'))"
                ),
                Map.entry(
                    "before",
                    "T(java.time.LocalDateTime).parse('${value1}').isBefore(T(java.time.LocalDateTime).parse('${value2}'))"
                )
            )
        );

        conditionTemplates.put(
            "number",
            Map.ofEntries(
                Map.entry("equals", "${value1} == ${value2}"),
                Map.entry("notEquals", "${value1} != ${value2}"),
                Map.entry("greater", "${value1} > ${value2}"),
                Map.entry("less", "${value1} < ${value2}"),
                Map.entry("greaterEquals", "${value1} >= ${value2}"),
                Map.entry("lessEquals", "${value1} <= ${value2}")
            )
        );

        conditionTemplates.put(
            "string",
            Map.ofEntries(
                Map.entry("equals", "'${value1}'.equals('${value2}')"),
                Map.entry("notEquals", "!'${value1}'.equals('${value2}')"),
                Map.entry("contains", "'${value1}'.contains('${value2}')"),
                Map.entry("notContains", "!'${value1}'.contains('${value2}')"),
                Map.entry("startsWith", "'${value1}'.startsWith('${value2}')"),
                Map.entry("endsWith", "'${value1}'.endsWith('${value2}')"),
                Map.entry("isEmpty", "'${value1}'.isEmpty()"),
                Map.entry("regex", "'${value1}' matches '${value2}'")
            )
        );
    }
}
