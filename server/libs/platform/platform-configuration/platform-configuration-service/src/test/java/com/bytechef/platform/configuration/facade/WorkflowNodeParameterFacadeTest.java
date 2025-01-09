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

package com.bytechef.platform.configuration.facade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Igor Beslic
 */
public class WorkflowNodeParameterFacadeTest {

    @Test
    public void testEvaluate() {
        Map<String, Object> parametersMap = Map.of(
            "body", Map.of("bodyContentType", "JSON"));

        boolean result = WorkflowNodeParameterFacadeImpl.evaluate(
            "body.bodyContentType == 'JSON'", Map.of(), Map.of(), parametersMap);

        Assertions.assertTrue(result);

        result = WorkflowNodeParameterFacadeImpl.evaluate(
            "body.bodyContentType == 'XML'", Map.of(), Map.of(), parametersMap);

        Assertions.assertFalse(result);
    }

    @Test
    public void testEvaluateArray() {
        Map<String, Object> parametersMap = Map.of(
            "conditions",
            List.of(
                List.of(Map.of("operation", "REGEX"), Map.of("operation", "EMPTY")),
                List.of(Map.of("operation", "REGEX"))));

        Map<String, Boolean> displayConditionMap = new HashMap<>();

        WorkflowNodeParameterFacadeImpl.evaluateArray(
            "conditions[index][index].operation != 'EMPTY'", displayConditionMap, Map.of(), Map.of(), "conditions[0]",
            parametersMap, false);

        Assertions.assertEquals(2, displayConditionMap.size());
        Assertions.assertEquals(
            Map.of("conditions[0][0].operation != 'EMPTY'", true, "conditions[1][0].operation != 'EMPTY'", true),
            displayConditionMap);

        displayConditionMap = new HashMap<>();

        WorkflowNodeParameterFacadeImpl.evaluateArray(
            "conditions[index][index].operation == 'EMPTY'", displayConditionMap, Map.of(), Map.of(), "conditions[0]",
            parametersMap, false);

        Assertions.assertEquals(1, displayConditionMap.size());
        Assertions.assertEquals(Map.of("conditions[0][1].operation == 'EMPTY'", true), displayConditionMap);

        parametersMap = Map.of(
            "conditions",
            List.of(
                List.of(Map.of("operation", "REGEX"), Map.of("operation", "EMPTY")),
                List.of(Map.of("operation", "NOT_CONTAINS"))));

        displayConditionMap = new HashMap<>();

        WorkflowNodeParameterFacadeImpl.evaluateArray(
            "!{'EMPTY','REGEX'}.contains(conditions[index][index].operation)", displayConditionMap, Map.of(), Map.of(),
            "conditions[0]", parametersMap, false);

        Assertions.assertEquals(1, displayConditionMap.size());
        Assertions.assertEquals(
            Map.of("!{'EMPTY','REGEX'}.contains(conditions[1][0].operation)", true), displayConditionMap);
    }

    @Test
    public void testHasExpressionVariable() {
        String[] expressions = {
            "variableName == 45", "'string' == variableName", "prefixVariableName!= newValue1 && !variableName ",
            "variableNameSuffix!= variableValue1 && !variableName", "prefixVariableName == 44 or variableName lt 45"
        };

        for (String expression : expressions) {
            Assertions.assertTrue(
                WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
                    expression, "variableName"),
                expression + " contains variableName");
        }

        String[] noVariableNameExpressions = {
            "prefixVariableName == 45", "'A' == variableNameSuffix", "prefixVariableName!= val && !variableNameSuffix ",
            "variableNameSuffix!= variableValue1 && !prefixVariableName", "prefixVariableName>44 or variableNameS lt 45"
        };

        for (String noVariableExpression : noVariableNameExpressions) {
            Assertions.assertFalse(
                WorkflowNodeParameterFacadeImpl.hasExpressionVariable(
                    noVariableExpression, "variableName"),
                noVariableExpression + " doesn't contain variableName");
        }
    }
}
