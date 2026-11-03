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

package com.bytechef.platform.workflow.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskValidatorTest {

    @Test
    void testValidateTaskParametersEvaluatesNestedArrayDisplayConditionsPerElement() {
        String taskParameters = """
            {
                "conditions": [
                    [
                        {"column": "qty", "operation": "BETWEEN", "value": "1", "valueTo": "5"},
                        {"column": "paid", "operation": "EMPTY", "value": "x"}
                    ],
                    [
                        {"column": "name", "operation": "IN", "values": ["a", "b"]},
                        {"column": "name", "operation": "EQUALS", "value": "c", "valueTo": "d"}
                    ]
                ]
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "conditions", "ARRAY", null, false, true, null,
                List.of(
                    new PropertyInfo(
                        null, "ARRAY", null, false, false, null,
                        List.of(
                            new PropertyInfo(
                                null, "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("column", "STRING", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo(
                                        "value", "STRING", null, false, true,
                                        "!contains({'IN','EMPTY'}, conditions[index][index].operation)", null),
                                    new PropertyInfo(
                                        "valueTo", "STRING", null, false, true,
                                        "contains({'BETWEEN'}, conditions[index][index].operation)", null),
                                    new PropertyInfo(
                                        "values", "ARRAY", null, false, true,
                                        "contains({'IN'}, conditions[index][index].operation)",
                                        List.of(
                                            new PropertyInfo(null, "STRING", null, false, true, null, null))))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals(
            "Property 'conditions[0][1].value' is not defined in task definition\n" +
                "Property 'conditions[1][1].valueTo' is not defined in task definition",
            warnings.toString());
    }
}
