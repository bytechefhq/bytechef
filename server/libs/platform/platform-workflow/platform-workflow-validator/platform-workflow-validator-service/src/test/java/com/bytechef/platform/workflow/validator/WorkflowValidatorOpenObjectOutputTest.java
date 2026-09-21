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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorOpenObjectOutputTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "webhook/v1/awaitWorkflowAndRespond", List.of(
            new PropertyInfo("csrfToken", "STRING", null, true, true, null, null)),
        "mistral/v1/uploadFile", List.of(
            new PropertyInfo("purpose", "STRING", null, true, true, null, null),
            new PropertyInfo("file", "FILE_ENTRY", null, true, true, null, List.of(
                new PropertyInfo("extension", "STRING", null, true, false, null, null),
                new PropertyInfo("mimeType", "STRING", null, true, false, null, null),
                new PropertyInfo("name", "STRING", null, true, false, null, null),
                new PropertyInfo("url", "STRING", null, true, false, null, null)))),
        "mistral/v1/listFiles", List.of(
            new PropertyInfo("page", "INTEGER", null, false, true, null, null)));

    private static final String WORKFLOW = """
        {
            "label": "invoice parsing",
            "description": "",
            "inputs": [],
            "triggers": [
                {
                    "label": "Webhook",
                    "name": "trigger_1",
                    "parameters": {"csrfToken": "token"},
                    "type": "webhook/v1/awaitWorkflowAndRespond"
                }
            ],
            "tasks": [
                {
                    "label": "MistralAI",
                    "name": "mistral_1",
                    "parameters": PARAMETERS,
                    "type": "TASK_TYPE"
                }
            ]
        }
        """;

    @Test
    void testOpenObjectOutputIsAcceptedForFileEntryProperty() {
        PropertyInfo triggerOutput = webhookOutput(new PropertyInfo("body", "OBJECT", null, false, false, null, null));

        assertEquals("", validate(triggerOutput));
    }

    @Test
    void testFileEntryOutputIsAcceptedForFileEntryProperty() {
        PropertyInfo triggerOutput = webhookOutput(
            new PropertyInfo("body", "FILE_ENTRY", null, false, false, null, List.of(
                new PropertyInfo("extension", "STRING", null, false, false, null, null),
                new PropertyInfo("mimeType", "STRING", null, false, false, null, null),
                new PropertyInfo("name", "STRING", null, false, false, null, null),
                new PropertyInfo("url", "STRING", null, false, false, null, null))));

        assertEquals("", validate(triggerOutput));
    }

    @Test
    void testOpenObjectOutputIsRejectedForIntegerProperty() {
        PropertyInfo triggerOutput = webhookOutput(new PropertyInfo("body", "OBJECT", null, false, false, null, null));

        assertEquals(
            "[mistral_1] Property 'trigger_1.body' in output of 'webhook/v1/awaitWorkflowAndRespond' is of type " +
                "object, not integer",
            validate(triggerOutput, "mistral/v1/listFiles", "{\"page\": \"${trigger_1.body}\"}"));
    }

    @Test
    void testOpenObjectOutputWithEmptyNestedPropertiesIsAcceptedForFileEntryProperty() {
        PropertyInfo triggerOutput = webhookOutput(
            new PropertyInfo("body", "OBJECT", null, false, false, null, List.of()));

        assertEquals("", validate(triggerOutput));
    }

    @Test
    void testDescribedObjectOutputIsRejectedForFileEntryProperty() {
        PropertyInfo triggerOutput = webhookOutput(
            new PropertyInfo("body", "OBJECT", null, false, false, null, List.of(
                new PropertyInfo("invoiceId", "STRING", null, false, false, null, null))));

        assertEquals(
            "[mistral_1] Property 'trigger_1.body' in output of 'webhook/v1/awaitWorkflowAndRespond' is of type " +
                "object, not file_entry",
            validate(triggerOutput));
    }

    private static PropertyInfo webhookOutput(PropertyInfo bodyPropertyInfo) {
        return new PropertyInfo(
            null, "OBJECT", null, false, false, null, List.of(
                new PropertyInfo("method", "STRING", null, false, false, null, null),
                new PropertyInfo("headers", "OBJECT", null, false, false, null, null),
                new PropertyInfo("parameters", "OBJECT", null, false, false, null, null),
                bodyPropertyInfo));
    }

    private static String validate(PropertyInfo triggerOutput) {
        return validate(
            triggerOutput, "mistral/v1/uploadFile", "{\"purpose\": \"ocr\", \"file\": \"${trigger_1.body}\"}");
    }

    private static String validate(PropertyInfo triggerOutput, String workflowTaskType, String parameters) {
        String workflow = WORKFLOW
            .replace("TASK_TYPE", workflowTaskType)
            .replace("PARAMETERS", parameters);

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefinitionProvider =
            (taskType, kind) -> TASK_DEFINITION_MAP.getOrDefault(taskType, List.of());
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = taskType -> null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider,
            WorkflowValidator.NO_RESOURCE_REFERENCE_PROVIDER, new HashMap<>(), new HashMap<>(),
            Map.of("trigger_1", triggerOutput), new HashMap<>(), new HashMap<>(), errors, warnings);

        return errors.toString();
    }
}
