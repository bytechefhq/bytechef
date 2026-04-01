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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
}
