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
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorFormulaValueTest {

    private static final List<PropertyInfo> DATE_RANGE_DEFINITION = List.of(
        new PropertyInfo(
            "dateRange", "OBJECT", null, true, true, null, null,
            List.of(
                new PropertyInfo("from", "DATE_TIME", null, true, true, null, null),
                new PropertyInfo("to", "DATE_TIME", null, true, true, null, null)),
            null));

    @Test
    void validateTaskParametersAcceptsFromAiValuesForNestedDateTimeProperties() {
        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "googleCalendar_1",
            """
                {"dateRange": {
                    "from": "=fromAi('dateRange.from', 'DATE_TIME', {'required': true})",
                    "to": "=fromAi('dateRange.to', 'DATE_TIME', {'required': true})"
                }}
                """,
            DATE_RANGE_DEFINITION, errors, new StringBuilder());

        assertEquals("", errors.toString());
    }

    @Test
    void validateTaskParametersAcceptsFormulaValuesForScalarAndObjectProperties() {
        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "googleCalendar_1",
            """
                {"count": "=fromAi('count', 'INTEGER')", "start": "=now()",
                 "dateRange": "=fromAi('dateRange', 'OBJECT')"}
                """,
            List.of(
                new PropertyInfo("count", "INTEGER", null, true, true, null, null),
                new PropertyInfo("start", "DATE_TIME", null, true, true, null, null),
                DATE_RANGE_DEFINITION.getFirst()),
            errors, new StringBuilder());

        assertEquals("", errors.toString());
    }

    @Test
    void validateTaskParametersStillReportsALiteralMalformedDateTime() {
        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "googleCalendar_1", "{\"dateRange\": {\"from\": \"tomorrow\", \"to\": \"2026-09-30T10:00:00\"}}",
            DATE_RANGE_DEFINITION, errors, new StringBuilder());

        assertEquals(
            "[googleCalendar_1] Property 'dateRange.from' has incorrect type. Format should be in: " +
                "'yyyy-MM-ddThh:mm:ss'",
            errors.toString());
    }

    @Test
    void validateTaskParametersBooleanFormulaValueSkipsTypeValidation() {
        String taskParameters = """
            {
                "active": "=true"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("active", "BOOLEAN", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersIntegerFormulaValueSkipsTypeValidation() {
        String taskParameters = """
            {
                "age": "=1 + 2"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("age", "INTEGER", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersPlainStringOnBooleanStillAddsError() {
        String taskParameters = """
            {
                "active": "abc"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("active", "BOOLEAN", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals(
            "[testTask] Property 'active' has incorrect type. Expected: boolean, but got: string",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersArrayItemFormulaValueSkipsTypeValidation() {
        String taskParameters = """
            {
                "items": [
                    {
                        "active": "=true"
                    }
                ]
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "items", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo("active", "BOOLEAN", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersArrayItemPlainStringOnBooleanStillAddsError() {
        String taskParameters = """
            {
                "items": [
                    {
                        "active": "abc"
                    }
                ]
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "items", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo("active", "BOOLEAN", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals(
            "[testTask] Property 'items[0].active' has incorrect type. Expected: boolean, but got: string",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersBooleanArrayFormulaElementSkipsTypeValidation() {
        String taskParameters = """
            {
                "flags": ["=true"]
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "flags", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo(null, "BOOLEAN", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersBooleanArrayPlainStringStillAddsError() {
        String taskParameters = """
            {
                "flags": ["abc"]
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "flags", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo(null, "BOOLEAN", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals(
            "[testTask] Value 'abc' has incorrect type in property 'flags'. Expected: boolean, but got: string",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersObjectArrayFormulaElementSkipsTypeValidation() {
        String taskParameters = """
            {
                "items": [
                    {
                        "name": "John"
                    },
                    "=someExpr"
                ]
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "items", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo("name", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersUnionTypeObjectArrayFormulaElementSkipsTypeValidation() {
        String taskParameters = """
            {
                "items": [
                    {
                        "name": "John"
                    },
                    "=someExpr"
                ]
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "items", "ARRAY", null, false, true, null,
                List.of(
                    new PropertyInfo(
                        "shapeA", "OBJECT", null, false, true, null,
                        List.of(new PropertyInfo("name", "STRING", null, false, true, null, null))),
                    new PropertyInfo(
                        "shapeB", "OBJECT", null, false, true, null,
                        List.of(new PropertyInfo("age", "INTEGER", null, false, true, null, null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }
}
