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
class WorkflowValidatorFreeFormObjectArrayTest {

    private static final List<PropertyInfo> TASK_DEFINITION = List.of(
        new PropertyInfo(
            "input", "ARRAY", null, true, true, null,
            List.of(new PropertyInfo(null, "OBJECT", null, false, true, null, null))));

    @Test
    void validateTaskParametersFreeFormObjectArrayAcceptsAnyKeys() {
        String taskParameters = """
            {
                "input": [
                    {
                        "field1": "a",
                        "field2": "b"
                    },
                    {
                        "field1": "c"
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, TASK_DEFINITION, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersFreeFormObjectArrayRejectsNonObjectElement() {
        String taskParameters = """
            {
                "input": [
                    {
                        "field1": "a"
                    },
                    "plain"
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters("testTask", taskParameters, TASK_DEFINITION, errors, warnings);

        assertEquals(
            "[testTask] Property 'input[1]' has incorrect type. Expected: object, but got: string", errors.toString());
        assertEquals("", warnings.toString());
    }
}
