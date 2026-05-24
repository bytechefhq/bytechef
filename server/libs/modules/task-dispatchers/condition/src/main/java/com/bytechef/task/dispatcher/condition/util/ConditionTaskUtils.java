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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import tools.jackson.core.type.TypeReference;

/**
 * Utility class for evaluating condition expressions in workflow condition dispatchers.
 *
 * <p>
 * <b>Security:</b> Expression evaluation runs against a {@link SimpleEvaluationContext} that disables Java type
 * references ({@code T(...)}), constructors, and bean references. Method resolution is limited to instance methods via
 * {@code withInstanceMethods()}, which blocks static methods as well as methods declared on {@link Object},
 * {@link Class}, and {@link ClassLoader}. This closes the SpEL-injection sink reported in
 * <a href="https://github.com/bytechefhq/bytechef/issues/5081">#5081</a> that was independent of the
 * {@code SpelEvaluator} hardening in #5035.
 *
 * @author Matija Petanjek
 */
public class ConditionTaskUtils {

    private static final ExpressionParser expressionParser = new SpelExpressionParser();

    // SPEL_INJECTION is suppressed because SpotBugs flags any non-constant string flowing into
    // SpelExpressionParser. The actual sink reported in #5081 is closed by evaluating against the
    // SimpleEvaluationContext built below, which forbids T(...), constructors, bean references,
    // and methods on Object/Class/ClassLoader.
    @SuppressFBWarnings({
        "SPEL_INJECTION", "REDOS"
    })
    public static boolean resolveCase(TaskExecution conditionTaskExecution) {
        Map<String, Object> variables = new LinkedHashMap<>();
        String expression;

        if (MapUtils.getBoolean(conditionTaskExecution.getParameters(), RAW_EXPRESSION, false)) {
            expression = MapUtils.getString(conditionTaskExecution.getParameters(), EXPRESSION);
        } else {
            List<List<Map<String, ?>>> conditions = MapUtils.getList(
                conditionTaskExecution.getParameters(), ConditionTaskDispatcherConstants.CONDITIONS,
                new TypeReference<>() {}, Collections.emptyList());

            List<String> conditionExpressions = new ArrayList<>();

            for (List<Map<String, ?>> andConditions : conditions) {
                conditionExpressions.add(String.join(" && ", getConditionExpressions(andConditions, variables)));
            }

            expression = String.join(" || ", conditionExpressions);
        }

        EvaluationContext evaluationContext = SimpleEvaluationContext.forReadOnlyDataBinding()
            .withInstanceMethods()
            .build();

        variables.forEach(evaluationContext::setVariable);

        Boolean result = expressionParser.parseExpression(expression)
            .getValue(evaluationContext, Boolean.class);

        return result != null && result;
    }

    private static List<String> getConditionExpressions(
        List<Map<String, ?>> conditions, Map<String, Object> variables) {

        List<String> conditionExpressions = new ArrayList<>();

        for (Map<String, ?> condition : conditions) {
            String operandType = MapUtils.getRequiredString(condition, "type");

            String conditionTemplate = conditionTemplates
                .get(operandType)
                .get(MapUtils.getRequiredString(condition, ConditionTaskDispatcherConstants.OPERATION));

            String value1 = MapUtils.getString(condition, ConditionTaskDispatcherConstants.VALUE_1, "");
            String value2 = MapUtils.getString(condition, ConditionTaskDispatcherConstants.VALUE_2, "");

            String replacement1;
            String replacement2;

            if (operandType.equals(ConditionTaskDispatcherConstants.DATE_TIME)) {
                String variableName1 = "dt" + variables.size();
                String variableName2 = "dt" + (variables.size() + 1);

                variables.put(variableName1, LocalDateTime.parse(value1));
                variables.put(variableName2, LocalDateTime.parse(value2));

                replacement1 = "#" + variableName1;
                replacement2 = "#" + variableName2;
            } else if (operandType.equals(ConditionTaskDispatcherConstants.STRING)) {
                replacement1 = EncodingUtils.urlEncode(value1);
                replacement2 = EncodingUtils.urlEncode(value2);
            } else {
                replacement1 = value1;
                replacement2 = value2;
            }

            conditionExpressions.add(
                conditionTemplate
                    .replace("${value1}", replacement1)
                    .replace("${value2}", replacement2));
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
                    "${value1}.isAfter(${value2})"),
                Map.entry(
                    ConditionTaskDispatcherConstants.Operation.BEFORE.name(),
                    "${value1}.isBefore(${value2})")));

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
