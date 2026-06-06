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

package com.bytechef.platform.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTestConfigurationFacadeTest {

    private static final Workflow REQUIRED_INPUT_WORKFLOW = new Workflow(
        "workflow1",
        """
            {
                "inputs": [
                    {
                        "name": "data",
                        "label": "Data",
                        "type": "string",
                        "required": true
                    }
                ]
            }
            """,
        Format.JSON);

    private static final Workflow OPTIONAL_INPUT_WORKFLOW = new Workflow(
        "workflow2",
        """
            {
                "inputs": [
                    {
                        "name": "data",
                        "label": "Data",
                        "type": "string",
                        "required": false
                    }
                ]
            }
            """,
        Format.JSON);

    @Test
    void testValidateInputsAcceptsNonEmptyString() {
        assertDoesNotThrow(
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(Map.of("data", "value"), REQUIRED_INPUT_WORKFLOW));
    }

    @Test
    void testValidateInputsAcceptsNonEmptyMap() {
        assertDoesNotThrow(
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(
                Map.of("data", Map.of("spreadsheetId", "123")), REQUIRED_INPUT_WORKFLOW));
    }

    @Test
    void testValidateInputsAcceptsNonEmptyCollection() {
        assertDoesNotThrow(
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(
                Map.of("data", List.of("a", "b")), REQUIRED_INPUT_WORKFLOW));
    }

    @Test
    void testValidateInputsRejectsEmptyString() {
        assertThrows(
            IllegalArgumentException.class,
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(Map.of("data", ""), REQUIRED_INPUT_WORKFLOW));
    }

    @Test
    void testValidateInputsRejectsEmptyMap() {
        assertThrows(
            IllegalArgumentException.class,
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(Map.of("data", Map.of()),
                REQUIRED_INPUT_WORKFLOW));
    }

    @Test
    void testValidateInputsRejectsEmptyCollection() {
        assertThrows(
            IllegalArgumentException.class,
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(
                Map.of("data", List.of()), REQUIRED_INPUT_WORKFLOW));
    }

    @Test
    void testValidateInputsRejectsNullValue() {
        Map<String, Object> inputs = new HashMap<>();

        inputs.put("data", null);

        assertThrows(
            IllegalArgumentException.class,
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(inputs, REQUIRED_INPUT_WORKFLOW));
    }

    @Test
    void testValidateInputsRejectsMissingKey() {
        assertThrows(
            IllegalArgumentException.class,
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(Map.of(), REQUIRED_INPUT_WORKFLOW));
    }

    @Test
    void testValidateInputsSkipsEmptyOptionalInput() {
        assertDoesNotThrow(
            () -> WorkflowTestConfigurationFacadeImpl.validateInputs(Map.of("data", ""), OPTIONAL_INPUT_WORKFLOW));
    }
}
