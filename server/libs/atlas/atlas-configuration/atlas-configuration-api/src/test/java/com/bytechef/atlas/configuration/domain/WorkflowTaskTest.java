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

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class WorkflowTaskTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDisabledParsedFromSource() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of("name", "task1", "type", "component/v1/action", "disabled", true));

        Assertions.assertTrue(workflowTask.isDisabled());
        Assertions.assertEquals(Boolean.TRUE, workflowTask.toMap()
            .get("disabled"));
    }

    @Test
    public void testDisabledDefaultsToFalseAndIsOmittedFromMap() {
        WorkflowTask workflowTask = new WorkflowTask(Map.of("name", "task1", "type", "component/v1/action"));

        Assertions.assertFalse(workflowTask.isDisabled());
        Assertions.assertFalse(workflowTask.toMap()
            .containsKey("disabled"));
    }

    @Test
    public void testDisabledPassesReservedWordValidation() {
        String definition = """
            {
                "label": "Test Workflow",
                "tasks": [
                    {
                        "name": "task1",
                        "type": "component/v1/action",
                        "disabled": true
                    }
                ]
            }
            """;

        Workflow workflow = new Workflow(definition, Workflow.Format.JSON);

        Assertions.assertNotNull(workflow);
        Assertions.assertEquals(1, workflow.getTasks()
            .size());
        Assertions.assertTrue(workflow.getTasks()
            .get(0)
            .isDisabled());
    }

    @Test
    public void testJacksonSerializationOmitsFalseDefault() throws Exception {
        WorkflowTask workflowTask = new WorkflowTask(Map.of("name", "task1", "type", "component/v1/action"));

        String json = objectMapper.writeValueAsString(workflowTask);
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

        Assertions.assertFalse(parsed.containsKey("disabled"),
            "disabled key should be omitted from JSON when false (Jackson serialization path)");
    }

    @Test
    public void testJacksonSerializationIncludesTrueValue() throws Exception {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of("name", "task1", "type", "component/v1/action", "disabled", true));

        String json = objectMapper.writeValueAsString(workflowTask);
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

        Assertions.assertTrue(parsed.containsKey("disabled"),
            "disabled key should be present in JSON when true");
        Assertions.assertEquals(true, parsed.get("disabled"));
    }

    @Test
    public void testRoundTripDeserialization() throws Exception {
        String jsonWithoutDisabled = """
            {
                "name": "task1",
                "type": "component/v1/action"
            }
            """;
        String jsonWithDisabledTrue = """
            {
                "name": "task1",
                "type": "component/v1/action",
                "disabled": true
            }
            """;
        String jsonWithDisabledFalse = """
            {
                "name": "task1",
                "type": "component/v1/action",
                "disabled": false
            }
            """;

        Map<String, ?> taskMapWithoutDisabled = objectMapper.readValue(jsonWithoutDisabled, Map.class);
        WorkflowTask taskWithoutDisabled = new WorkflowTask(taskMapWithoutDisabled);
        Assertions.assertFalse(taskWithoutDisabled.isDisabled());

        Map<String, ?> taskMapWithDisabledTrue = objectMapper.readValue(jsonWithDisabledTrue, Map.class);
        WorkflowTask taskWithDisabledTrue = new WorkflowTask(taskMapWithDisabledTrue);
        Assertions.assertTrue(taskWithDisabledTrue.isDisabled());

        Map<String, ?> taskMapWithDisabledFalse = objectMapper.readValue(jsonWithDisabledFalse, Map.class);
        WorkflowTask taskWithDisabledFalse = new WorkflowTask(taskMapWithDisabledFalse);
        Assertions.assertFalse(taskWithDisabledFalse.isDisabled());
    }
}
