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

package com.bytechef.atlas.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.Input;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowInputParsingTest {

    @Test
    void testParseInputWithoutExtensions() {
        String definition =
            """
                {
                    "inputs": [
                        {
                            "name": "email",
                            "label": "Email",
                            "type": "string",
                            "required": true
                        }
                    ]
                }
                """;

        Workflow workflow = new Workflow("workflow1", definition, Format.JSON);

        Input input = workflow.getInputs()
            .getFirst();

        assertEquals("email", input.name());
        assertTrue(input.extensions()
            .isEmpty());
    }

    @Test
    void testParseInputCapturesUnknownKeysAsExtensions() {
        String definition =
            """
                {
                    "inputs": [
                        {
                            "name": "dateRange",
                            "label": "Date Range",
                            "componentName": "googleSheets",
                            "componentVersion": 2,
                            "groupName": "sheetSelection"
                        }
                    ]
                }
                """;

        Workflow workflow = new Workflow("workflow1", definition, Format.JSON);

        List<Input> inputs = workflow.getInputs();

        assertEquals(1, inputs.size());

        Input input = inputs.getFirst();

        assertEquals("dateRange", input.name());
        assertEquals("Date Range", input.label());
        assertEquals("googleSheets", input.getExtension("componentName", String.class, null));
        assertEquals(2, input.getExtension("componentVersion", Integer.class, null));
        assertEquals("sheetSelection", input.getExtension("groupName", String.class, null));
    }
}
