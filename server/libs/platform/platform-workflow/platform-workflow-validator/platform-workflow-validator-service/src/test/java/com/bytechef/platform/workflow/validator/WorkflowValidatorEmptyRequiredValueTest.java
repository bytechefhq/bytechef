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
class WorkflowValidatorEmptyRequiredValueTest {

    private static final List<PropertyInfo> TASK_DEFINITION = List.of(
        new PropertyInfo("name", "STRING", null, true, true, null, null));

    @Test
    void validateTaskParametersReportsARequiredPropertyClearedToNull() {
        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "accelo_1", "{\"name\": null}", TASK_DEFINITION, errors, new StringBuilder());

        assertEquals("[accelo_1] Missing required property: name", errors.toString());
    }

    @Test
    void validateTaskParametersReportsARequiredPropertyLeftBlank() {
        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "accelo_1", "{\"name\": \"   \"}", TASK_DEFINITION, errors, new StringBuilder());

        assertEquals("[accelo_1] Missing required property: name", errors.toString());
    }

    @Test
    void validateTaskParametersAcceptsAnOptionalPropertyClearedToNull() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "accelo_1", "{\"name\": \"Acme\", \"note\": null}",
            List.of(
                new PropertyInfo("name", "STRING", null, true, true, null, null),
                new PropertyInfo("note", "STRING", null, false, true, null, null)),
            errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }
}
