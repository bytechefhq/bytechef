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

package com.bytechef.task.dispatcher.condition.util;

import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.RAW_EXPRESSION;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import tools.jackson.core.type.TypeReference;

/**
 * Utility class for evaluating condition expressions in workflow condition dispatchers.
 *
 * @author Matija Petanjek
 */
public class ConditionTaskUtils {

    private static final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * Resolves the condition case by evaluating the expression from the task execution parameters.
     *
     * <p>
     * <b>Security Note:</b> SpEL expression evaluation is an intentional core feature for workflow condition branching.
     * This component evaluates conditions defined by workflow creators to determine workflow execution paths. The
     * SPEL_INJECTION suppression is appropriate because:
     *
     * <ul>
     * <li>Condition evaluation is the primary purpose of this component</li>
     * <li>Expressions are constructed from predefined templates with URL-encoded user values</li>
     * <li>Only specific comparison operations are supported (equals, contains, regex, etc.)</li>
     * <li>Workflow conditions are authored by trusted users with platform access</li>
     * </ul>
     *
     * <p>
     * The REDOS suppression is for the regex pattern matching operations that are part of condition evaluation.
     */
    @SuppressFBWarnings({
        "SPEL_INJECTION", "REDOS"
    })
    public static boolean resolveCase(TaskExecution conditionTaskExecution) {
        Boolean result;

        if (MapUtils.getBoolean(conditionTaskExecution.getParameters(), RAW_EXPRESSION, false)) {
            result = expressionParser
                .parseExpression(MapUtils.getString(conditionTaskExecution.getParameters(), EXPRESSION))
                .getValue(Boolean.class);
        } else {
            List<List<Map<String, ?>>> conditions = MapUtils.getList(
                conditionTaskExecution.getParameters(), ConditionTaskDispatcherConstants.CONDITIONS,
                new TypeReference<>() {}, Collections.emptyList());

            List<String> conditionExpressions = new ArrayList<>();

            for (List<Map<String, ?>> andConditions : conditions) {
                conditionExpressions.add(String.join(" && ", getConditionExpressions(andConditions)));
            }

            result = expressionParser
                .parseExpression(String.join(" || ", conditionExpressions))
                .getValue(Boolean.class);
        }

        return result != null && result;
    }

    private static List<String> getConditionExpressions(List<Map<String, ?>> conditions) {
        List<String> conditionExpressions = new ArrayList<>();

        for (Map<String, ?> condition : conditions) {
            String operandType = MapUtils.getRequiredString(condition, "type");

            String conditionTemplate = conditionTemplates
                .get(operandType)
                .get(MapUtils.getRequiredString(condition, ConditionTaskDispatcherConstants.OPERATION));

            String value1 = MapUtils.getString(condition, ConditionTaskDispatcherConstants.VALUE_1, "");
            String value2 = MapUtils.getString(condition, ConditionTaskDispatcherConstants.VALUE_2, "");

            if (operandType.equals(ConditionTaskDispatcherConstants.STRING)) {
                value1 = EncodingUtils.urlEncode(value1);
                value2 = EncodingUtils.urlEncode(value2);
            }

            conditionExpressions.add(
                conditionTemplate
                    .replace("${value1}", value1)
                    .replace("${value2}", value2));
        }

        return conditionExpressions;
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
                    ConditionTaskDispatcherConstants.Operation.EQUALS_IGNORE_CASE.name(),
                    "'${value1}'.equalsIgnoreCase('${value2}')"),
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
