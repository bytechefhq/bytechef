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

package com.bytechef.platform.component.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ActionDefinition.OutputFunction;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.LogEntryBufferAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionServiceTest {

    private ActionContext actionContext;
    private com.bytechef.component.definition.ActionDefinition actionDefinition;
    private ActionDefinitionServiceImpl actionDefinitionService;
    private ComponentDefinitionRegistry componentDefinitionRegistry;
    private ContextFactory contextFactory;

    @BeforeEach
    void beforeEach() {
        componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
        contextFactory = mock(ContextFactory.class);

        actionDefinitionService = new ActionDefinitionServiceImpl(componentDefinitionRegistry, contextFactory);

        actionContext = mock(ActionContext.class, withSettings().extraInterfaces(LogEntryBufferAware.class));
        actionDefinition = mock(com.bytechef.component.definition.ActionDefinition.class);

        when(componentDefinitionRegistry.getActionDefinition("example", 1, "perform")).thenReturn(actionDefinition);
        when(actionDefinition.getResumePerform()).thenReturn(Optional.empty());
        when(
            contextFactory.createActionContext(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(actionContext);
    }

    @Disabled
    @Test
    public void testGetComponentActionDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetComponentActionDefinitions() {
        // TODO
    }

    @Test
    void testExecutePerformFlushesBufferedLogEntriesAfterASuccessfulPerform() {
        MultipleConnectionsPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> "ok";

        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        actionDefinitionService.executePerform(
            "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), Map.of(), Map.of(), null, false,
            null, null, null, null);

        verify((LogEntryBufferAware) actionContext).flushLogEntries();
    }

    @Test
    void testExecutePerformFlushesBufferedLogEntriesWhenThePerformThrows() {
        MultipleConnectionsPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> {
                throw new IllegalStateException("boom");
            };

        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        assertThrows(
            RuntimeException.class, () -> actionDefinitionService.executePerform(
                "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), Map.of(), Map.of(), null,
                false, null, null, null, null));

        verify((LogEntryBufferAware) actionContext).flushLogEntries();
    }

    @Test
    void testExecuteOptionsPassesTheWorkflowIdToTheContext() throws Exception {
        stubOptionsFunction();

        actionDefinitionService.executeOptions(
            "dataTable", 1, "getRecord", "table", Map.of(), List.of(), null, (ComponentConnection) null, "workflow-1");

        verify(contextFactory).createActionContext(
            "dataTable", 1, "getRecord", null, null, null, null, "workflow-1", null, null, null, true);
    }

    @Test
    void testExecuteMultipleConnectionsOptionsPassesTheWorkflowIdToTheContext() throws Exception {
        stubOptionsFunction();

        actionDefinitionService.executeOptions(
            "dataTable", 1, "getRecord", "table", Map.of(), List.of(), null, Map.of(), Map.of(), "workflow-1");

        verify(contextFactory).createActionContext(
            "dataTable", 1, "getRecord", null, null, null, null, "workflow-1", null, null, null, true);
    }

    @Test
    void testExecuteOutputPassesTheWorkflowIdToTheContext() {
        com.bytechef.component.definition.ActionDefinition dataTableActionDefinition = mock(
            com.bytechef.component.definition.ActionDefinition.class);
        OutputDefinition outputDefinition = mock(OutputDefinition.class);
        OutputFunction outputFunction = (inputParameters, connectionParameters, context) -> null;

        when(componentDefinitionRegistry.getActionDefinition("dataTable", 1, "getRecord"))
            .thenReturn(dataTableActionDefinition);
        doReturn(Optional.of(outputDefinition)).when(dataTableActionDefinition)
            .getOutputDefinition();
        doReturn(Optional.of(outputFunction)).when(outputDefinition)
            .getOutput();

        actionDefinitionService.executeOutput("dataTable", 1, "getRecord", Map.of(), Map.of(), "workflow-1");

        verify(contextFactory).createActionContext(
            eq("dataTable"), eq(1), eq("getRecord"), isNull(), isNull(), isNull(), isNull(), eq("workflow-1"),
            isNull(), isNull(), isNull(), eq(true));
    }

    private void stubOptionsFunction() throws Exception {
        DynamicOptionsProperty<?> dynamicOptionsProperty = mock(
            DynamicOptionsProperty.class, withSettings().extraInterfaces(Property.class));
        OptionsDataSource<?> optionsDataSource = mock(OptionsDataSource.class);
        OptionsFunction<String> optionsFunction =
            (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> List.of();

        doReturn(dynamicOptionsProperty).when(componentDefinitionRegistry)
            .getActionProperty(eq("dataTable"), eq(1), eq("getRecord"), eq("table"), any(), any(), any(), any());
        doReturn(Optional.of(optionsDataSource)).when(dynamicOptionsProperty)
            .getOptionsDataSource();
        doReturn(optionsFunction).when(optionsDataSource)
            .getOptions();
    }
}
