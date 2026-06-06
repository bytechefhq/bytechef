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

package com.bytechef.ai.mcp.tool.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.tool.platform.ComponentTools.ActionMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.ComponentTools.TriggerMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.TaskDispatcherTools.TaskDispatcherMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.TaskTools.TaskMinimalInfo;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Marko Kriskovic
 */
class TaskToolsTest {

    @Mock
    private ComponentTools componentTools;

    @Mock
    private TaskDispatcherTools taskDispatcherTools;

    private TaskTools taskTools;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        taskTools = new TaskTools(componentTools, taskDispatcherTools);
    }

    @Test
    void getTaskOutputPropertyNoErrors() {
        taskTools.getTaskOutputProperty("trigger", "trigger", "component", 1);
    }

    @Test
    void getTaskOutputPropertyWithTriggerType() {
        PropertyInfo mockPropertyInfo = new PropertyInfo(
            "output", "object", "Output description", false, false, null, null);

        when(componentTools.getOutputProperty(eq("component"), eq("trigger"), eq(1)))
            .thenReturn(mockPropertyInfo);

        PropertyInfo result = taskTools.getTaskOutputProperty("trigger", "trigger", "component", 1);

        assertNotNull(result);
        verify(componentTools).getOutputProperty("component", "trigger", 1);
    }

    @Test
    void getTaskOutputPropertyWithActionType() {
        PropertyInfo mockPropertyInfo = new PropertyInfo(
            "output", "object", "Output description", false, false, null, null);

        when(componentTools.getOutputProperty(eq("component"), eq("action"), eq(1)))
            .thenReturn(mockPropertyInfo);

        PropertyInfo result = taskTools.getTaskOutputProperty("action", "action", "component", 1);

        assertNotNull(result);
        verify(componentTools).getOutputProperty("component", "action", 1);
    }

    @Test
    void getTaskOutputPropertyWithTaskDispatcherType() {
        PropertyInfo mockPropertyInfo = new PropertyInfo(
            "output", "object", "Output description", false, false, null, null);

        when(taskDispatcherTools.getTaskDispatcherOutput(eq("dispatcher"), eq(1)))
            .thenReturn(mockPropertyInfo);

        PropertyInfo result = taskTools.getTaskOutputProperty("taskDispatcher", "dispatcher", "dispatcher", 1);

        assertNotNull(result);
        verify(taskDispatcherTools).getTaskDispatcherOutput("dispatcher", 1);
    }

    @Test
    void getTaskOutputPropertyWithException() {
        when(componentTools.getOutputProperty(eq("component"), eq("action"), eq(1)))
            .thenThrow(new RuntimeException("Task not found"));

        assertThrows(
            RuntimeException.class,
            () -> taskTools.getTaskOutputProperty("action", "action", "component", 1));
    }

    @Test
    void getTaskOutputPropertyWithDifferentVersion() {
        PropertyInfo mockPropertyInfo = new PropertyInfo(
            "output", "object", "Output description", false, false, null, null);

        when(componentTools.getOutputProperty(eq("myComponent"), eq("customAction"), eq(5)))
            .thenReturn(mockPropertyInfo);

        PropertyInfo result = taskTools.getTaskOutputProperty("action", "customAction", "myComponent", 5);

        assertNotNull(result);
        verify(componentTools).getOutputProperty("myComponent", "customAction", 5);
    }

    @Test
    void testListTasksFiltersDisallowedComponentsButKeepsTaskDispatchers() {
        when(componentTools.listActions()).thenReturn(List.of(
            new ActionMinimalInfo("sendMessage", "Send a message", "slack"),
            new ActionMinimalInfo("createRow", "Create a row", "googleSheets")));
        when(componentTools.listTriggers()).thenReturn(List.of(
            new TriggerMinimalInfo("newMessage", "New message", "slack")));
        when(taskDispatcherTools.listTaskDispatchers()).thenReturn(List.of(
            new TaskDispatcherMinimalInfo("condition", "Branch on a condition", 1)));

        ToolContext toolContext = new ToolContext(Map.of(
            TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack")));

        List<TaskMinimalInfo> result = taskTools.listTasks(null, null, toolContext);

        assertThat(result)
            .extracting(TaskMinimalInfo::componentName)
            .containsExactlyInAnyOrder("slack", "slack", null);
    }

    @Test
    void testListTasksWithoutToolContextReturnsEverything() {
        when(componentTools.listActions()).thenReturn(List.of(
            new ActionMinimalInfo("sendMessage", "Send a message", "slack"),
            new ActionMinimalInfo("createRow", "Create a row", "googleSheets")));
        when(componentTools.listTriggers()).thenReturn(List.of());
        when(taskDispatcherTools.listTaskDispatchers()).thenReturn(List.of());

        List<TaskMinimalInfo> result = taskTools.listTasks(null, null, null);

        assertThat(result)
            .extracting(TaskMinimalInfo::componentName)
            .containsExactlyInAnyOrder("slack", "googleSheets");
    }

    @Test
    void testSearchTasksFiltersDisallowedComponentsButKeepsTaskDispatchers() {
        when(componentTools.searchActions("row")).thenReturn(List.of(
            new ActionMinimalInfo("createRow", "Create a row", "googleSheets"),
            new ActionMinimalInfo("addRow", "Add a row", "airtable")));
        when(componentTools.searchTriggers("row")).thenReturn(List.of());
        when(taskDispatcherTools.searchTaskDispatchers("row")).thenReturn(List.of(
            new TaskDispatcherMinimalInfo("loop", "Loop over rows", 1)));

        ToolContext toolContext = new ToolContext(Map.of(
            TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("googleSheets")));

        List<TaskMinimalInfo> result = taskTools.searchTasks("row", null, null, toolContext);

        assertThat(result)
            .extracting(TaskMinimalInfo::componentName)
            .containsExactlyInAnyOrder("googleSheets", null);
    }

    @Test
    void testListTasksAppliesAllowListFilterBeforeLimit() {
        when(componentTools.listActions()).thenReturn(List.of(
            new ActionMinimalInfo("createRow", "Create a row", "googleSheets"),
            new ActionMinimalInfo("addRow", "Add a row", "googleSheets"),
            new ActionMinimalInfo("sendMessage", "Send a message", "slack"),
            new ActionMinimalInfo("updateMessage", "Update a message", "slack")));
        when(componentTools.listTriggers()).thenReturn(List.of());
        when(taskDispatcherTools.listTaskDispatchers()).thenReturn(List.of());

        ToolContext toolContext = new ToolContext(Map.of(
            TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack")));

        // The two disallowed googleSheets actions come first; a limit-first implementation would
        // truncate to them and then filter to an empty list. Filtering before the limit keeps both slack actions.
        List<TaskMinimalInfo> result = taskTools.listTasks(null, 2, toolContext);

        assertThat(result)
            .extracting(TaskMinimalInfo::componentName)
            .containsExactly("slack", "slack");
    }

    @Test
    void testListTasksWithEmptyAllowListReturnsEverything() {
        when(componentTools.listActions()).thenReturn(List.of(
            new ActionMinimalInfo("sendMessage", "Send a message", "slack"),
            new ActionMinimalInfo("createRow", "Create a row", "googleSheets")));
        when(componentTools.listTriggers()).thenReturn(List.of());
        when(taskDispatcherTools.listTaskDispatchers()).thenReturn(List.of());

        ToolContext toolContext = new ToolContext(Map.of(
            TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of()));

        List<TaskMinimalInfo> result = taskTools.listTasks(null, null, toolContext);

        assertThat(result)
            .extracting(TaskMinimalInfo::componentName)
            .containsExactlyInAnyOrder("slack", "googleSheets");
    }

    @Test
    void testListTasksWithNonSetAllowListValueReturnsEverything() {
        when(componentTools.listActions()).thenReturn(List.of(
            new ActionMinimalInfo("sendMessage", "Send a message", "slack"),
            new ActionMinimalInfo("createRow", "Create a row", "googleSheets")));
        when(componentTools.listTriggers()).thenReturn(List.of());
        when(taskDispatcherTools.listTaskDispatchers()).thenReturn(List.of());

        ToolContext toolContext = new ToolContext(Map.of(
            TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, List.of("slack")));

        List<TaskMinimalInfo> result = taskTools.listTasks(null, null, toolContext);

        assertThat(result)
            .extracting(TaskMinimalInfo::componentName)
            .containsExactlyInAnyOrder("slack", "googleSheets");
    }
}
