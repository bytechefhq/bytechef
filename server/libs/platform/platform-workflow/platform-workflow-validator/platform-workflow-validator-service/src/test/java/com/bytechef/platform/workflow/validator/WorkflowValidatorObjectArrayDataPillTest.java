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
class WorkflowValidatorObjectArrayDataPillTest {

    private static final List<PropertyInfo> FILE_ENTRY_PROPERTY_INFOS = List.of(
        new PropertyInfo("extension", "STRING", null, true, true, null, null),
        new PropertyInfo("mimeType", "STRING", null, true, true, null, null),
        new PropertyInfo("name", "STRING", null, true, true, null, null),
        new PropertyInfo("url", "STRING", null, true, true, null, null));

    private static final List<PropertyInfo> FILE_ENTRY_ARRAY_TASK_DEFINITION = List.of(
        new PropertyInfo(
            "attachments", "ARRAY", null, false, true, null,
            List.of(new PropertyInfo(null, "FILE_ENTRY", null, false, true, null, FILE_ENTRY_PROPERTY_INFOS))));

    private static final List<PropertyInfo> UNION_OBJECT_ARRAY_TASK_DEFINITION = List.of(
        new PropertyInfo(
            "items", "ARRAY", null, false, true, null,
            List.of(
                new PropertyInfo(
                    "first", "OBJECT", null, false, true, null,
                    List.of(new PropertyInfo("a", "STRING", null, true, true, null, null))),
                new PropertyInfo(
                    "second", "OBJECT", null, false, true, null,
                    List.of(new PropertyInfo("b", "STRING", null, true, true, null, null))))));

    private static final List<PropertyInfo> ARRAY_OF_ARRAYS_TASK_DEFINITION = List.of(
        new PropertyInfo(
            "rows", "ARRAY", null, false, true, null,
            List.of(
                new PropertyInfo(
                    null, "ARRAY", null, false, true, null,
                    List.of(new PropertyInfo(null, "STRING", null, false, true, null, null))))));

    @Test
    void validateTaskParametersFileEntryArrayAcceptsDataPillElement() {
        String taskParameters = """
            {
                "attachments": [
                    "${fileStorage_1}"
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "testTask", taskParameters, FILE_ENTRY_ARRAY_TASK_DEFINITION, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersFileEntryArrayAcceptsDataPillAfterObjectElement() {
        String taskParameters = """
            {
                "attachments": [
                    {
                        "extension": "txt",
                        "mimeType": "text/plain",
                        "name": "a.txt",
                        "url": "base64:///a"
                    },
                    "${fileStorage_1}"
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "testTask", taskParameters, FILE_ENTRY_ARRAY_TASK_DEFINITION, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersFileEntryArrayRejectsPlainStringElement() {
        String taskParameters = """
            {
                "attachments": [
                    "plain"
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "testTask", taskParameters, FILE_ENTRY_ARRAY_TASK_DEFINITION, errors, warnings);

        assertEquals(
            "[testTask] Property 'attachments[0]' has incorrect type. Expected: object, but got: string",
            errors.toString());
    }

    @Test
    void validateTaskParametersUnionObjectArrayAcceptsDataPillElement() {
        String taskParameters = """
            {
                "items": [
                    {
                        "a": "x"
                    },
                    "${source.item}"
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "testTask", taskParameters, UNION_OBJECT_ARRAY_TASK_DEFINITION, errors, warnings);

        assertEquals("", errors.toString());
    }

    @Test
    void validateTaskParametersArrayOfArraysAcceptsDataPillElement() {
        String taskParameters = """
            {
                "rows": [
                    [
                        "x"
                    ],
                    "${source.row}"
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(
            "testTask", taskParameters, ARRAY_OF_ARRAYS_TASK_DEFINITION, errors, warnings);

        assertEquals("", errors.toString());
    }
}
