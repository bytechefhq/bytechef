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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ProjectWorkflowLifecycleToolsTest {

    @Test
    void testExposesNoToolThatWritesAWorkflowDefinition() {
        ToolCallback[] toolCallbacks = ToolCallbacks.from(new ProjectWorkflowLifecycleTools(
            mock(ProjectWorkflowTools.class)));

        List<String> toolNames = Arrays.stream(toolCallbacks)
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactlyInAnyOrder(
            "getWorkflow", "listWorkflows", "searchWorkflows", "createProjectWorkflow", "deleteWorkflow",
            "saveWorkflowTestConnection");

        String createInputSchema = Arrays.stream(toolCallbacks)
            .filter(toolCallback -> "createProjectWorkflow".equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow()
            .getToolDefinition()
            .inputSchema();

        assertThat(JsonUtils.readMap(createInputSchema)).extractingByKey("properties")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsKeys("projectId", "name")
            .doesNotContainKey("definition");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateProjectWorkflowDelegatesWithAnEmptyDefinition() {
        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        ToolContext toolContext = new ToolContext(Map.of());

        new ProjectWorkflowLifecycleTools(projectWorkflowTools).createProjectWorkflow(
            7L, "Send welcome email", "Greets new users", toolContext);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(projectWorkflowTools).createProjectWorkflow(eq(7L), definitionCaptor.capture(), eq(toolContext));

        Map<String, Object> definition = new HashMap<>(JsonUtils.readMap(definitionCaptor.getValue()));

        assertThat(definition)
            .containsEntry("label", "Send welcome email")
            .containsEntry("description", "Greets new users")
            .containsEntry("tasks", List.of());
        assertThat((List<Map<String, Object>>) definition.get("triggers"))
            .singleElement()
            .satisfies(trigger -> assertThat(trigger).containsEntry("type", "manual/v1/manual"));
    }
}
