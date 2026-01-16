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

package com.bytechef.ai.mcp.tool.automation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Marko Kriskovic
 */
class ProjectWorkflowToolsImplTest {
    @Mock
    private TaskTools taskTools;

    private ProjectWorkflowToolsImpl projectWorkflowTools;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        projectWorkflowTools = new ProjectWorkflowToolsImpl(null, taskTools);
    }

    @Test
    void getTaskOutputPropertyNoErrors() {
        projectWorkflowTools.getTaskOutputProperty("component/v1/trigger", "trigger", new StringBuilder());
    }

    @Test
    void getTaskOutputPropertyWithTriggerType() {
        PropertyInfo mockPropertyInfo = new PropertyInfo(
            "output", "object", "Output description", false, false, null, null);
        when(taskTools.getTaskOutputProperty(eq("trigger"), eq("trigger"), eq("component"), eq(1)))
            .thenReturn(mockPropertyInfo);

        PropertyInfo result = projectWorkflowTools.getTaskOutputProperty(
            "component/v1/trigger", "trigger", new StringBuilder());

        assertNotNull(result);
        verify(taskTools).getTaskOutputProperty("trigger", "trigger", "component", 1);
    }

    @Test
    void getTaskOutputPropertyWithActionType() {
        PropertyInfo mockPropertyInfo = new PropertyInfo(
            "output", "object", "Output description", false, false, null, null);
        when(taskTools.getTaskOutputProperty(eq("action"), eq("action"), eq("component"), eq(1)))
            .thenReturn(mockPropertyInfo);

        PropertyInfo result = projectWorkflowTools.getTaskOutputProperty(
            "component/v1/action", "action", new StringBuilder());

        assertNotNull(result);
        verify(taskTools).getTaskOutputProperty("action", "action", "component", 1);
    }

    @Test
    void getTaskOutputPropertyWithTaskDispatcherType() {
        PropertyInfo mockPropertyInfo = new PropertyInfo(
            "output", "object", "Output description", false, false, null, null);
        when(taskTools.getTaskOutputProperty(eq("taskDispatcher"), eq("dispatcher"), eq("dispatcher"), eq(1)))
            .thenReturn(mockPropertyInfo);

        PropertyInfo result = projectWorkflowTools.getTaskOutputProperty(
            "dispatcher/v1", "taskDispatcher", new StringBuilder());

        assertNotNull(result);
        verify(taskTools).getTaskOutputProperty("taskDispatcher", "dispatcher", "dispatcher", 1);
    }

    @Test
    void getTaskOutputPropertyWithException() {
        StringBuilder warnings = new StringBuilder();
        when(taskTools.getTaskOutputProperty(anyString(), anyString(), anyString(), anyInt()))
            .thenThrow(new RuntimeException("Task not found"));

        PropertyInfo result = projectWorkflowTools.getTaskOutputProperty(
            "component/v1/action", "action", warnings);

        assertNull(result);
        assertEquals("Task not found", warnings.toString());
    }

    @Test
    void getTaskOutputPropertyWithDifferentVersion() {
        PropertyInfo mockPropertyInfo = new PropertyInfo(
            "output", "object", "Output description", false, false, null, null);
        when(taskTools.getTaskOutputProperty(eq("action"), eq("customAction"), eq("myComponent"), eq(5)))
            .thenReturn(mockPropertyInfo);

        PropertyInfo result = projectWorkflowTools.getTaskOutputProperty(
            "myComponent/v5/customAction", "action", new StringBuilder());

        assertNotNull(result);
        verify(taskTools).getTaskOutputProperty("action", "customAction", "myComponent", 5);
    }
}
