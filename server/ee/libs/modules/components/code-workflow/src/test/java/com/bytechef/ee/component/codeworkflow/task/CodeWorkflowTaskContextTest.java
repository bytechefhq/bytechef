/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.workflow.definition.TaskContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
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
            actionContext, actionDefinitionService, componentConnections, componentDefinitionService, Map.of(),
            List.of(), Map.of());

        when(componentDefinitionService.getComponentDefinition("component1", null)).thenReturn(componentDefinition);
        when(
            actionDefinitionService.executePerformForPolyglot(
                "component1", 0, "action1", Map.of("key", "value"), componentConnection, componentConnections,
                Map.of(), null, actionContext))
                    .thenReturn("result");

        Object result = taskContext.component("component1", "action1", Map.of("key", "value"), "connection1");

        assertEquals("result", result);

        verify(actionDefinitionService).executePerformForPolyglot(
            "component1", 0, "action1", Map.of("key", "value"), componentConnection, componentConnections, Map.of(),
            null, actionContext);
    }

    @Test
    void testComponentForwardsEnvironmentIdFromActionContextAware() throws Exception {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getEnvironmentId()).thenReturn(7L);

        ComponentConnection componentConnection = new ComponentConnection("component1", 1, 2L, Map.of(), null);

        when(componentDefinitionService.getComponentDefinition("component1", null)).thenReturn(componentDefinition);

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContextAware, actionDefinitionService, Map.of("connection1", componentConnection),
            componentDefinitionService, Map.of(), List.of(), Map.of());

        taskContext.component("component1", "action1", Map.of(), "connection1");

        verify(actionDefinitionService).executePerformForPolyglot(
            eq("component1"), eq(0), eq("action1"), eq(Map.of()), eq(componentConnection), any(), any(), eq(7L),
            eq(actionContextAware));
    }

    @Test
    void testComponentThrowsWhenConnectionUnresolvedAndComponentRequiresConnection() {
        when(componentDefinitionService.getComponentDefinition("component1", null)).thenReturn(componentDefinition);
        when(actionDefinitionService.actionDefinesConnection("component1", 0, "action1")).thenReturn(true);

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, Map.of(), List.of(),
            Map.of());

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
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, Map.of(), List.of(),
            Map.of());

        taskContext.component("component1", "action1", Map.of(), "missingConnection");

        verify(actionDefinitionService).executePerformForPolyglot(
            eq("component1"), eq(0), eq("action1"), eq(Map.of()), isNull(), any(), any(), isNull(),
            eq(actionContext));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testComponentComposesClusterElementsIntoExtensionsAndWiresTheirConnections() throws Exception {
        ComponentConnection openAiConnection = new ComponentConnection("openAi", 1, 1L, Map.of(), null);

        when(componentDefinitionService.getComponentDefinition("aiAgent", null))
            .thenReturn(new ComponentDefinition("aiAgent"));

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of("openai-prod", openAiConnection),
            componentDefinitionService, Map.of(), List.of(), Map.of());

        taskContext.component(
            "aiAgent", "chat", Map.of(), null,
            Map.of(
                "model",
                Map.of(
                    "type", "openAi/v1/model", "connection", "openai-prod", "parameters",
                    Map.of("model", "gpt-4o"))));

        ArgumentCaptor<Map<String, ?>> extensionsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, ComponentConnection>> connectionsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(actionDefinitionService).executePerformForPolyglot(
            eq("aiAgent"), eq(0), eq("chat"), eq(Map.of()), isNull(), connectionsCaptor.capture(),
            extensionsCaptor.capture(), isNull(), eq(actionContext));

        // The shape a visual agent node carries, so the agent resolves its model through the identical code path.
        assertEquals(
            Map.of(
                "clusterElements",
                Map.of(
                    "model",
                    Map.of("name", "model", "type", "openAi/v1/model", "parameters", Map.of("model", "gpt-4o")))),
            extensionsCaptor.getValue());

        // The agent looks a cluster element's connection up by the element's name, so it is wired under that name
        // alongside the task's own declared connections.
        assertEquals(
            Map.of("openai-prod", openAiConnection, "model", openAiConnection), connectionsCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testComponentNamesAToolSoTheModelCanTellTwoOfThemApart() throws Exception {
        ComponentConnection slackConnection = new ComponentConnection("slack", 1, 1L, Map.of(), null);

        when(componentDefinitionService.getComponentDefinition("aiAgent", null))
            .thenReturn(new ComponentDefinition("aiAgent"));

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of("slack-prod", slackConnection), componentDefinitionService,
            Map.of(), List.of(), Map.of());

        taskContext.component(
            "aiAgent", "chat", Map.of(), null,
            Map.of(
                "tools",
                List.of(
                    Map.of("type", "slack/v1/sendMessage", "connection", "slack-prod", "name", "post_to_slack"))));

        ArgumentCaptor<Map<String, ?>> extensionsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(actionDefinitionService).executePerformForPolyglot(
            eq("aiAgent"), eq(0), eq("chat"), eq(Map.of()), isNull(), any(), extensionsCaptor.capture(), isNull(),
            eq(actionContext));

        assertEquals(
            Map.of(
                "clusterElements",
                Map.of(
                    "tools",
                    List.of(
                        Map.of(
                            "name", "post_to_slack", "type", "slack/v1/sendMessage", "parameters",
                            Map.of("toolName", "post_to_slack"))))),
            extensionsCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testComposedExtensionsParseBackIntoTheClusterElementsThePlatformReads() throws Exception {
        ComponentConnection openAiConnection = new ComponentConnection("openAi", 1, 1L, Map.of(), null);

        when(componentDefinitionService.getComponentDefinition("aiAgent", null))
            .thenReturn(new ComponentDefinition("aiAgent"));

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of("openai-prod", openAiConnection),
            componentDefinitionService, Map.of(), List.of(), Map.of());

        taskContext.component(
            "aiAgent", "chat", Map.of(), null,
            Map.of(
                "model",
                Map.of("type", "openAi/v1/model", "connection", "openai-prod", "parameters",
                    Map.of("model", "gpt-4o"))));

        ArgumentCaptor<Map<String, ?>> extensionsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(actionDefinitionService).executePerformForPolyglot(
            any(), anyInt(), any(), any(), any(), any(), extensionsCaptor.capture(), any(), any());

        // Asserting against the real parser rather than a literal: this is what the agent's perform actually calls,
        // so a shape that only looks right would not survive here.
        ClusterElement clusterElement = ClusterElementMap.of(extensionsCaptor.getValue())
            .getClusterElement(MODEL);

        assertEquals("openAi", clusterElement.getComponentName());
        assertEquals(1, clusterElement.getComponentVersion());
        assertEquals("model", clusterElement.getClusterElementName());
        assertEquals("model", clusterElement.getWorkflowNodeName());
        assertEquals(Map.of("model", "gpt-4o"), clusterElement.getParameters());
    }

    @Test
    void testComponentRejectsAClusterElementConnectionTheTaskNeverDeclared() {
        when(componentDefinitionService.getComponentDefinition("aiAgent", null))
            .thenReturn(new ComponentDefinition("aiAgent"));

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, Map.of(), List.of(),
            Map.of());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> taskContext.component(
                "aiAgent", "chat", Map.of(), null,
                Map.of("model", Map.of("type", "openAi/v1/model", "connection", "openai-prod"))));

        assertEquals("Connection with name openai-prod does not exist", exception.getMessage());
    }

    @Test
    void testComponentRejectsAClusterElementWithoutAType() {
        when(componentDefinitionService.getComponentDefinition("aiAgent", null))
            .thenReturn(new ComponentDefinition("aiAgent"));

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, Map.of(), List.of(),
            Map.of());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> taskContext.component(
                "aiAgent", "chat", Map.of(), null, Map.of("model", Map.of("parameters", Map.of()))));

        assertEquals("Cluster element model requires a type, such as openAi/v1/model", exception.getMessage());
    }

    @Test
    void testInputExposesWorkflowInputsAndPriorTaskOutputs() {
        Map<String, ?> input = Map.of("input", Map.of("email", "a@b.com"), "my-task1", Map.of("id", 3));

        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, input, List.of(), Map.of());

        assertEquals(input, taskContext.input());
        assertEquals(Map.of("id", 3), taskContext.input("my-task1"));
    }

    @Test
    void testInputExplainsAConcurrentTaskRatherThanReportingAnUnknownName() {
        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, Map.of("task1", "value"),
            List.of("sibling"), Map.of());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> taskContext.input("sibling"));

        assertEquals(
            "Task sibling runs at the same time as this task, so its output is not available; move it before the " +
                "group to read it here",
            exception.getMessage());
    }

    @Test
    void testParametersExposeTheTaskOwnDeclaredValues() {
        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, Map.of("task1", "value"),
            List.of(), Map.of("retries", 3));

        // A task's own configuration stays out of input(), so a parameter is never mistaken for a task's output.
        assertEquals(Map.of("retries", 3), taskContext.parameters());
        assertEquals(Map.of("task1", "value"), taskContext.input());
    }

    @Test
    void testInputThrowsForUnknownName() {
        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, Map.of("task1", "value"),
            List.of(), Map.of());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> taskContext.input("task2"));

        assertEquals("No workflow input or task output named task2; available: [task1]", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLogDelegatesToActionContextAtGivenLevel() throws Exception {
        CodeWorkflowTaskContext taskContext = new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, Map.of(), componentDefinitionService, Map.of(), List.of(),
            Map.of());

        taskContext.log(TaskContext.LogLevel.WARN, "a warning");

        ArgumentCaptor<ContextConsumer<Context.Log>> captor = ArgumentCaptor.forClass(ContextConsumer.class);

        verify(actionContext).log(captor.capture());

        Context.Log log = mock(Context.Log.class);

        captor.getValue()
            .accept(log);

        verify(log).warn("a warning");
        verify(log, never()).info(anyString());
    }
}
