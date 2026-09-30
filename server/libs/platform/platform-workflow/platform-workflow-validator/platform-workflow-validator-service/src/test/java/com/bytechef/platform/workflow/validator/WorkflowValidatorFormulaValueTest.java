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
}
