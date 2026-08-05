/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextConsumer;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeWorkflowTaskContextTest {

    private final ActionContext actionContext = mock(ActionContext.class);
    private final ActionDefinitionService actionDefinitionService = mock(ActionDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ComponentDefinition componentDefinition = new ComponentDefinition("component1");

    @Test
    void testComponentDispatchesWithConnectionResolvedFromMap() throws Exception {
        ComponentConnection componentConnection = new ComponentConnection("component1", 1, 1L, Map.of(), null);

        Map<String, ComponentConnection> componentConnections = Map.of("connection1", componentConnection);

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, componentConnections, componentDefinitionService);

        when(componentDefinitionService.getComponentDefinition("component1", null)).thenReturn(componentDefinition);
        when(
            actionDefinitionService.executePerformForPolyglot(
                "component1", 0, "action1", Map.of("key", "value"), componentConnection, null, actionContext))
                    .thenReturn("result");

        Object result = taskContext.component("component1", "action1", Map.of("key", "value"), "connection1");

        assertEquals("result", result);

        verify(actionDefinitionService).executePerformForPolyglot(
            "component1", 0, "action1", Map.of("key", "value"), componentConnection, null, actionContext);
    }

    @Test
    void testComponentForwardsEnvironmentIdFromActionContextAware() throws Exception {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getEnvironmentId()).thenReturn(7L);

        ComponentConnection componentConnection = new ComponentConnection("component1", 1, 2L, Map.of(), null);

        when(componentDefinitionService.getComponentDefinition("component1", null)).thenReturn(componentDefinition);

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContextAware, actionDefinitionService, Map.of("connection1", componentConnection),
            componentDefinitionService);

        taskContext.component("component1", "action1", Map.of(), "connection1");

        verify(actionDefinitionService).executePerformForPolyglot(
            eq("component1"), eq(0), eq("action1"), eq(Map.of()), eq(componentConnection), eq(7L),
            eq(actionContextAware));
    }

    @Test
    void testComponentThrowsWhenConnectionUnresolvedAndComponentRequiresConnection() {
        when(componentDefinitionService.getComponentDefinition("component1", null)).thenReturn(componentDefinition);
        when(actionDefinitionService.actionDefinesConnection("component1", 0, "action1")).thenReturn(true);

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> taskContext.component("component1", "action1", Map.of(), "missingConnection"));

        assertEquals("Connection with name missingConnection does not exist", exception.getMessage());
    }

    @Test
    void testComponentDispatchesWithoutConnectionWhenComponentDoesNotRequireConnection() throws Exception {
        when(componentDefinitionService.getComponentDefinition("component1", null)).thenReturn(componentDefinition);
        when(actionDefinitionService.actionDefinesConnection("component1", 0, "action1")).thenReturn(false);

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService);

        taskContext.component("component1", "action1", Map.of(), "missingConnection");

        verify(actionDefinitionService).executePerformForPolyglot(
            eq("component1"), eq(0), eq("action1"), eq(Map.of()), isNull(), isNull(), eq(actionContext));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLogDelegatesToActionContextAtGivenLevel() throws Exception {
        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService);

        taskContext.log("warn", "a warning");

        ArgumentCaptor<ContextConsumer<Context.Log>> captor = ArgumentCaptor.forClass(ContextConsumer.class);

        verify(actionContext).log(captor.capture());

        Context.Log log = mock(Context.Log.class);

        captor.getValue()
            .accept(log);

        verify(log).warn("a warning");
        verify(log, never()).info(anyString());
    }
}
