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

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorNodeTypeTest {

    private static final Set<String> EXISTING_NODE_TYPES = Set.of(
        "trigger:schedule/v1/everyDay", "task:googleMail/v1/searchEmail", "task:loop/v1");

    @Test
    void validateNodeTypesExistAcceptsDefinedTypes() {
        StringBuilder errors = new StringBuilder();

        WorkflowValidator.validateNodeTypesExist(
            workflow("schedule/v1/everyDay", "googleMail/v1/searchEmail", "googleMail/v1/searchEmail"),
            WorkflowValidatorNodeTypeTest::nodeTypeExists, errors);

        assertEquals("", errors.toString());
    }

    @Test
    void validateNodeTypesExistReportsUndefinedTriggerTaskAndNestedTaskTypes() {
        StringBuilder errors = new StringBuilder();

        WorkflowValidator.validateNodeTypesExist(
            workflow("gmail/v1/newEmail", "gmail/v1/searchEmail", "dataTable/v1/insertRow"),
            WorkflowValidatorNodeTypeTest::nodeTypeExists, errors);

        assertEquals(
            """
                [trigger_1] Field 'type' must name an existing trigger; gmail/v1/newEmail does not exist
                [searchEmail_1] Field 'type' must name an existing action or task dispatcher; gmail/v1/searchEmail \
                does not exist
                [insertRow_1] Field 'type' must name an existing action or task dispatcher; dataTable/v1/insertRow \
                does not exist""",
            errors.toString());
    }

    @Test
    void validateNodeTypesExistLeavesMalformedTypesToTheFormatCheck() {
        List<String> checkedTypes = new ArrayList<>();
        StringBuilder errors = new StringBuilder();

        WorkflowValidator.validateNodeTypesExist(
            workflow("schedule/v1/every-day", "googleMail/v1/searchEmail", "googleMail/v1/searchEmail"),
            (type, kind) -> {
                checkedTypes.add(type);

                return nodeTypeExists(type, kind);
            },
            errors);

        assertEquals("", errors.toString());
        assertEquals(List.of("googleMail/v1/searchEmail", "loop/v1", "googleMail/v1/searchEmail"), checkedTypes);
    }

    private static boolean nodeTypeExists(String type, String kind) {
        return EXISTING_NODE_TYPES.contains(kind + ":" + type);
    }

    private static String workflow(String triggerType, String taskType, String nestedTaskType) {
        return """
            {
                "label": "Invoices",
                "triggers": [
                    {"label": "Trigger", "name": "trigger_1", "type": "{TRIGGER_TYPE}", "parameters": {}}
                ],
                "tasks": [
                    {"label": "Search", "name": "searchEmail_1", "type": "{TASK_TYPE}", "parameters": {}},
                    {
                        "label": "Loop",
                        "name": "loop_1",
                        "type": "loop/v1",
                        "parameters": {
                            "items": [],
                            "iteratee": [
                                {"label": "Insert", "name": "insertRow_1", "type": "{NESTED_TASK_TYPE}", "parameters": {}}
                            ]
                        }
                    }
                ]
            }
            """
            .replace("{TRIGGER_TYPE}", triggerType)
            .replace("{TASK_TYPE}", taskType)
            .replace("{NESTED_TASK_TYPE}", nestedTaskType);
    }
}
