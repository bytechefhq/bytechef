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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ComponentToolsTest {

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private TriggerDefinitionFacade triggerDefinitionFacade;

    private ComponentTools componentTools;
    @Mock
    private ConnectionService connectionService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        componentTools = new ComponentTools(
            componentDefinitionService, actionDefinitionFacade, triggerDefinitionFacade, connectionService);
    }

    @Test
    void testGetOutputPropertyForTriggerWithOutputSchema() {
        String componentName = "testComponent";
        String triggerName = "testTrigger";
        Integer version = 1;

        StringProperty outputProperty = mock(StringProperty.class);
        when(outputProperty.getName()).thenReturn("result");

        OutputResponse outputResponse = new OutputResponse(outputProperty, null);

        com.bytechef.platform.component.domain.TriggerDefinition trigger = mock(
            com.bytechef.platform.component.domain.TriggerDefinition.class);
        when(trigger.getName()).thenReturn(triggerName);
        when(trigger.isOutputDefined()).thenReturn(true);
        when(trigger.isOutputSchemaDefined()).thenReturn(true);
        when(trigger.getOutputResponse()).thenReturn(outputResponse);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of(trigger));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        PropertyInfo result = componentTools.getOutputProperty(componentName, triggerName, version);

        assertNotNull(result);
        assertEquals("result", result.name());
        verify(componentDefinitionService).getComponentDefinition(componentName, version);
        verify(triggerDefinitionFacade, never()).executeTrigger(
            anyString(), anyInt(), anyString(), any(), any(), anyMap(), any(), any(), any(), any(), any(),
            anyBoolean());
    }

    @Test
    void testGetOutputPropertyForTriggerWithOutputFunction() {
        String componentName = "testComponent";
        String triggerName = "testTrigger";
        Integer version = 1;

        StringProperty outputProperty = mock(StringProperty.class);
        when(outputProperty.getName()).thenReturn("dynamicResult");

        OutputResponse outputResponse = new OutputResponse(outputProperty, null);

        com.bytechef.platform.component.domain.TriggerDefinition trigger = mock(
            com.bytechef.platform.component.domain.TriggerDefinition.class);
        when(trigger.getName()).thenReturn(triggerName);
        when(trigger.isOutputDefined()).thenReturn(true);
        when(trigger.isOutputFunctionDefined()).thenReturn(true);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of(trigger));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        when(triggerDefinitionFacade.executeOutput(
            eq(componentName), eq(version), eq(triggerName), anyMap(), isNull()))
                .thenReturn(outputResponse);

        PropertyInfo result = componentTools.getOutputProperty(componentName, triggerName, version);

        assertNotNull(result);
        assertEquals("dynamicResult", result.name());
        verify(triggerDefinitionFacade).executeOutput(eq(componentName), eq(version), eq(triggerName), anyMap(),
            isNull());
    }

    @Test
    void testGetOutputPropertyForTriggerWithExecuteTriggerFallback() {
        String componentName = "testComponent";
        String triggerName = "testTrigger";
        Integer version = 1;

        com.bytechef.platform.component.domain.TriggerDefinition trigger = mock(
            com.bytechef.platform.component.domain.TriggerDefinition.class);
        when(trigger.getName()).thenReturn(triggerName);
        when(trigger.isOutputDefined()).thenReturn(true);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of(trigger));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        TriggerOutput triggerOutput = mock(TriggerOutput.class);
        when(
            triggerDefinitionFacade
                .executeTrigger(
                    eq(componentName), eq(version), eq(triggerName), isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), eq(true)))
                        .thenReturn(triggerOutput);

        PropertyInfo result = componentTools.getOutputProperty(componentName, triggerName, version);

        assertNotNull(result);
        verify(triggerDefinitionFacade).executeTrigger(
            eq(componentName), eq(version), eq(triggerName), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), eq(true));
    }

    @Test
    void testGetOutputPropertyForTriggerWithExecuteTriggerThrowsException() {
        String componentName = "testComponent";
        String triggerName = "testTrigger";
        Integer version = 1;

        com.bytechef.platform.component.domain.TriggerDefinition trigger = mock(
            com.bytechef.platform.component.domain.TriggerDefinition.class);
        when(trigger.getName()).thenReturn(triggerName);
        when(trigger.isOutputDefined()).thenReturn(true);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of(trigger));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        when(triggerDefinitionFacade.executeTrigger(
            eq(componentName), eq(version), eq(triggerName), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), eq(true)))
                .thenThrow(new RuntimeException("Connection error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> componentTools.getOutputProperty(componentName, triggerName, version));

        assertEquals("Failed to get output properties: Please make a " + componentName + " connector",
            exception.getMessage());
    }

    @Test
    void testGetOutputPropertyForActionWithOutputSchema() {
        String componentName = "testComponent";
        String actionName = "testAction";
        Integer version = 1;

        StringProperty outputProperty = mock(StringProperty.class);
        when(outputProperty.getName()).thenReturn("actionResult");

        OutputResponse outputResponse = new OutputResponse(outputProperty, null);

        ActionDefinition action = mock(ActionDefinition.class);
        when(action.getName()).thenReturn(actionName);
        when(action.isOutputDefined()).thenReturn(true);
        when(action.isOutputSchemaDefined()).thenReturn(true);
        when(action.getOutputResponse()).thenReturn(outputResponse);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of());
        when(componentDefinition.getActions()).thenReturn(List.of(action));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        PropertyInfo result = componentTools.getOutputProperty(componentName, actionName, version);

        assertNotNull(result);
        assertEquals("actionResult", result.name());
        verify(componentDefinitionService).getComponentDefinition(componentName, version);
        verify(actionDefinitionFacade, never()).executePerform(
            anyString(), anyInt(), anyString(), any(), any(), any(), anyString(), anyMap(), anyMap(), anyMap(), any(),
            any(), anyBoolean());
    }

    @Test
    void testGetOutputPropertyForActionWithOutputFunction() {
        String componentName = "testComponent";
        String actionName = "testAction";
        Integer version = 1;

        StringProperty outputProperty = mock(StringProperty.class);
        when(outputProperty.getName()).thenReturn("dynamicActionResult");

        OutputResponse outputResponse = new OutputResponse(outputProperty, null);

        ActionDefinition action = mock(ActionDefinition.class);
        when(action.getName()).thenReturn(actionName);
        when(action.isOutputDefined()).thenReturn(true);
        when(action.isOutputSchemaDefined()).thenReturn(false);
        when(action.isOutputFunctionDefined()).thenReturn(true);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of());
        when(componentDefinition.getActions()).thenReturn(List.of(action));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        when(actionDefinitionFacade.executeOutput(
            eq(componentName), eq(version), eq(actionName), anyMap(), anyMap()))
                .thenReturn(outputResponse);

        PropertyInfo result = componentTools.getOutputProperty(componentName, actionName, version);

        assertNotNull(result);
        assertEquals("dynamicActionResult", result.name());
        verify(actionDefinitionFacade).executeOutput(eq(componentName), eq(version), eq(actionName), anyMap(),
            anyMap());
    }

    @Test
    void testGetOutputPropertyForActionWithExecutePerformFallback() {
        String componentName = "testComponent";
        String actionName = "testAction";
        Integer version = 1;

        ActionDefinition action = mock(ActionDefinition.class);
        when(action.getName()).thenReturn(actionName);
        when(action.isOutputDefined()).thenReturn(true);
        when(action.isOutputSchemaDefined()).thenReturn(false);
        when(action.isOutputFunctionDefined()).thenReturn(true);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of());
        when(componentDefinition.getActions()).thenReturn(List.of(action));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        when(actionDefinitionFacade.executeOutput(
            eq(componentName), eq(version), eq(actionName), anyMap(), anyMap()))
                .thenThrow(new RuntimeException("Output function failed"));

        Connection connection = mock(Connection.class);
        when(connection.getId()).thenReturn(123L);
        when(connectionService.getConnections(componentName, version, ModeType.AUTOMATION))
            .thenReturn(List.of(connection));

        Map<String, Object> performResult = Map.of("data", "test");
        when(
            actionDefinitionFacade
                .executePerform(
                    eq(componentName), eq(version), eq(actionName), isNull(), isNull(), isNull(), isNull(), isNull(),
                    anyMap(), isNull(), isNull(), isNull(), eq(true)))
                        .thenReturn(performResult);

        PropertyInfo result = componentTools.getOutputProperty(componentName, actionName, version);

        assertNotNull(result);
        verify(actionDefinitionFacade).executeOutput(eq(componentName), eq(version), eq(actionName), anyMap(),
            anyMap());
        verify(actionDefinitionFacade).executePerform(
            eq(componentName), eq(version), eq(actionName), isNull(), isNull(), isNull(), isNull(), isNull(), anyMap(),
            isNull(), isNull(), isNull(), eq(true));
    }

    @Test
    void testGetOutputPropertyForActionWithExecutePerformThrowsException() {
        String componentName = "testComponent";
        String actionName = "testAction";
        Integer version = 1;

        ActionDefinition action = mock(ActionDefinition.class);
        when(action.getName()).thenReturn(actionName);
        when(action.isOutputDefined()).thenReturn(true);
        when(action.isOutputSchemaDefined()).thenReturn(false);
        when(action.isOutputFunctionDefined()).thenReturn(true);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of());
        when(componentDefinition.getActions()).thenReturn(List.of(action));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        when(actionDefinitionFacade.executeOutput(
            eq(componentName), eq(version), eq(actionName), anyMap(), anyMap()))
                .thenThrow(new RuntimeException("Output function failed"));

        when(connectionService.getConnections(componentName, version, ModeType.AUTOMATION))
            .thenThrow(new RuntimeException("Connection required"));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> componentTools.getOutputProperty(componentName, actionName, version));

        assertEquals("Failed to get output properties: Please make a " + componentName + " connection",
            exception.getMessage());
    }

    @Test
    void testGetOutputPropertyForOperationNotFound() {
        // Given
        String componentName = "testComponent";
        String operationName = "nonExistentOperation";
        Integer version = 1;

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of());
        when(componentDefinition.getActions()).thenReturn(List.of());
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> componentTools.getOutputProperty(componentName, operationName, version));

        assertEquals("Failed to get output properties: Operation '" + operationName + "' not found in component '"
            + componentName + "'", exception.getMessage());
    }

    @Test
    void testGetOutputPropertyReturnsNullWhenOutputResponseIsNull() {
        String componentName = "testComponent";
        String actionName = "testAction";
        Integer version = 1;

        ActionDefinition action = mock(ActionDefinition.class);
        when(action.getName()).thenReturn(actionName);
        when(action.isOutputDefined()).thenReturn(false);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of());
        when(componentDefinition.getActions()).thenReturn(List.of(action));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        PropertyInfo result = componentTools.getOutputProperty(componentName, actionName, version);

        assertNull(result);
    }

    @Test
    void testGetOutputPropertyReturnsNullWhenOutputSchemaIsNull() {
        String componentName = "testComponent";
        String actionName = "testAction";
        Integer version = 1;

        OutputResponse outputResponse = new OutputResponse(null, null);

        ActionDefinition action = mock(ActionDefinition.class);
        when(action.getName()).thenReturn(actionName);
        when(action.isOutputDefined()).thenReturn(true);
        when(action.isOutputSchemaDefined()).thenReturn(true);
        when(action.getOutputResponse()).thenReturn(outputResponse);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinition.getTriggers()).thenReturn(List.of());
        when(componentDefinition.getActions()).thenReturn(List.of(action));
        when(componentDefinitionService.getComponentDefinition(componentName, version))
            .thenReturn(componentDefinition);

        PropertyInfo result = componentTools.getOutputProperty(componentName, actionName, version);

        assertNull(result);
    }

    @Test
    void testGetOutputPropertyWithNullVersion() {
        String componentName = "testComponent";
        String actionName = "testAction";

        StringProperty outputProperty = mock(StringProperty.class);
        when(outputProperty.getName()).thenReturn("result");

        OutputResponse outputResponse = new OutputResponse(outputProperty, null);

        ActionDefinition action = mock(ActionDefinition.class);
        when(action.getName()).thenReturn(actionName);
        when(action.isOutputDefined()).thenReturn(true);
        when(action.isOutputSchemaDefined()).thenReturn(true);
        when(action.getOutputResponse()).thenReturn(outputResponse);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        when(componentDefinition.getName()).thenReturn(componentName);
        when(componentDefinition.getTriggers()).thenReturn(List.of());
        when(componentDefinition.getActions()).thenReturn(List.of(action));
        when(componentDefinitionService.getComponentDefinition(componentName, null))
            .thenReturn(componentDefinition);

        PropertyInfo result = componentTools.getOutputProperty(componentName, actionName, null);

        assertNotNull(result);
        assertEquals("result", result.name());
        verify(componentDefinitionService).getComponentDefinition(componentName, null);
    }

}
