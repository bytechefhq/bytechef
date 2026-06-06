/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedWorkflowInputOptionFacadeImplTest {

    @Test
    void testGetWorkflowInputOptionsForSingleProperty() {
        IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);
        ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectionId()).thenReturn(55L);
        when(integrationInstanceService.getIntegrationInstance(7L)).thenReturn(integrationInstance);

        when(integrationWorkflowService.getWorkflowId(7L, "wf-uuid")).thenReturn("wf-id");

        Workflow.Input input = new Workflow.Input(
            "channel", "Channel", "STRING", true /* , new Workflow.ComponentInputReference("slack", 1, "channel") */);

        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getType()).thenReturn("slack/v1/sendMessage");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(input));
        when(workflow.getTasks(false)).thenReturn(List.of(workflowTask));
        when(workflowService.getWorkflow("wf-id")).thenReturn(workflow);

        Option option = mock(Option.class);

        when(option.getValue()).thenReturn("C123");
        when(actionDefinitionFacade.executeOptions(
            eq("slack"), anyInt(), eq("sendMessage"), eq("channelId"), any(), any(), any(), eq(55L)))
                .thenReturn(List.of(option));

        EmbeddedWorkflowInputOptionFacadeImpl facade = new EmbeddedWorkflowInputOptionFacadeImpl(
            actionDefinitionFacade, integrationInstanceService, integrationWorkflowService, triggerDefinitionFacade,
            workflowService);

        List<Option> options = facade.getWorkflowInputOptions(7L, "wf-uuid", "channel", "channelId", Map.of(), null);

        assertEquals(1, options.size());
        assertEquals("C123", options.get(0)
            .getValue());

        verify(actionDefinitionFacade).executeOptions(
            eq("slack"), anyInt(), eq("sendMessage"), eq("channelId"), any(), any(), any(), eq(55L));
    }

    @Test
    void testGetWorkflowInputOptionsForGroupMember() {
        IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);
        ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectionId()).thenReturn(55L);
        when(integrationInstanceService.getIntegrationInstance(7L)).thenReturn(integrationInstance);

        when(integrationWorkflowService.getWorkflowId(7L, "wf-uuid")).thenReturn("wf-id");

        Workflow.Input input = new Workflow.Input(
            "sheet", "Sheet", "OBJECT", true
        /* , new Workflow.ComponentInputReference("googleSheets", 1, "sheetSelection") */);

        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getType()).thenReturn("googleSheets/v1/insertRow");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(input));
        when(workflow.getTasks(false)).thenReturn(List.of(workflowTask));
        when(workflowService.getWorkflow("wf-id")).thenReturn(workflow);

        Option option = mock(Option.class);

        when(option.getValue()).thenReturn("Sheet1");
        when(actionDefinitionFacade.executeOptions(
            eq("googleSheets"), anyInt(), eq("insertRow"), eq("sheetName"), any(), any(), any(), eq(55L)))
                .thenReturn(List.of(option));

        EmbeddedWorkflowInputOptionFacadeImpl facade = new EmbeddedWorkflowInputOptionFacadeImpl(
            actionDefinitionFacade, integrationInstanceService, integrationWorkflowService, triggerDefinitionFacade,
            workflowService);

        List<Option> options = facade.getWorkflowInputOptions(7L, "wf-uuid", "sheet", "sheetName", Map.of(), null);

        assertEquals(1, options.size());
        assertEquals("Sheet1", options.get(0)
            .getValue());

        verify(actionDefinitionFacade).executeOptions(
            eq("googleSheets"), anyInt(), eq("insertRow"), eq("sheetName"), any(), any(), any(), eq(55L));
    }

    @Test
    void testGetWorkflowInputOptionsForTrigger() {
        IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);
        ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectionId()).thenReturn(55L);
        when(integrationInstanceService.getIntegrationInstance(7L)).thenReturn(integrationInstance);

        when(integrationWorkflowService.getWorkflowId(7L, "wf-uuid")).thenReturn("wf-id");

        Workflow.Input input = new Workflow.Input(
            "channel", "Channel", "STRING", true /* , new Workflow.ComponentInputReference("slack", 1, "channel") */);

        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);

        when(workflowTrigger.getType()).thenReturn("slack/v1/newMessage");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(input));
        when(workflow.getTasks(false)).thenReturn(List.of());
        when(workflow.getExtensions(any(), eq(WorkflowTrigger.class), any())).thenReturn(List.of(workflowTrigger));
        when(workflowService.getWorkflow("wf-id")).thenReturn(workflow);

        Option option = mock(Option.class);

        when(option.getValue()).thenReturn("C123");
        when(triggerDefinitionFacade.executeOptions(
            eq("slack"), anyInt(), eq("newMessage"), eq("channelId"), any(), any(), any(), eq(55L)))
                .thenReturn(List.of(option));

        EmbeddedWorkflowInputOptionFacadeImpl facade = new EmbeddedWorkflowInputOptionFacadeImpl(
            actionDefinitionFacade, integrationInstanceService, integrationWorkflowService, triggerDefinitionFacade,
            workflowService);

        List<Option> options = facade.getWorkflowInputOptions(7L, "wf-uuid", "channel", "channelId", Map.of(), null);

        assertEquals(1, options.size());
        assertEquals("C123", options.get(0)
            .getValue());

        verify(triggerDefinitionFacade).executeOptions(
            eq("slack"), anyInt(), eq("newMessage"), eq("channelId"), any(), any(), any(), eq(55L));
    }

    @Test
    void testGetWorkflowInputOptionsPropagatesDependencyValuesAndPaths() {
        IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);
        ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectionId()).thenReturn(55L);
        when(integrationInstanceService.getIntegrationInstance(7L)).thenReturn(integrationInstance);

        when(integrationWorkflowService.getWorkflowId(7L, "wf-uuid")).thenReturn("wf-id");

        Workflow.Input input = new Workflow.Input(
            "channel", "Channel", "STRING", true /* , new Workflow.ComponentInputReference("slack", 1, "channel") */);

        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getType()).thenReturn("slack/v1/sendMessage");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(input));
        when(workflow.getTasks(false)).thenReturn(List.of(workflowTask));
        when(workflowService.getWorkflow("wf-id")).thenReturn(workflow);

        Map<String, Object> lookupDependsOnValues = Map.of("teamId", "T1");

        EmbeddedWorkflowInputOptionFacadeImpl facade = new EmbeddedWorkflowInputOptionFacadeImpl(
            actionDefinitionFacade, integrationInstanceService, integrationWorkflowService, triggerDefinitionFacade,
            workflowService);

        facade.getWorkflowInputOptions(7L, "wf-uuid", "channel", "channelId", lookupDependsOnValues, "search");

        verify(actionDefinitionFacade).executeOptions(
            eq("slack"), anyInt(), eq("sendMessage"), eq("channelId"), eq(lookupDependsOnValues),
            eq(List.of("teamId")), eq("search"), eq(55L));
    }

    @Test
    void testGetWorkflowInputOptionsThrowsForUnknownInput() {
        IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);
        ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstanceService.getIntegrationInstance(7L)).thenReturn(integrationInstance);

        when(integrationWorkflowService.getWorkflowId(7L, "wf-uuid")).thenReturn("wf-id");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of());
        when(workflowService.getWorkflow("wf-id")).thenReturn(workflow);

        EmbeddedWorkflowInputOptionFacadeImpl facade = new EmbeddedWorkflowInputOptionFacadeImpl(
            actionDefinitionFacade, integrationInstanceService, integrationWorkflowService, triggerDefinitionFacade,
            workflowService);

        assertThrows(
            IllegalArgumentException.class,
            () -> facade.getWorkflowInputOptions(7L, "wf-uuid", "unknown", "channelId", Map.of(), null));
    }

    @Test
    void testGetWorkflowInputOptionsThrowsWhenNoNodeUsesComponent() {
        IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);
        ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstanceService.getIntegrationInstance(7L)).thenReturn(integrationInstance);

        when(integrationWorkflowService.getWorkflowId(7L, "wf-uuid")).thenReturn("wf-id");

        Workflow.Input input = new Workflow.Input(
            "channel", "Channel", "STRING", true /* ,new Workflow.ComponentInputReference("slack", 1, "channel") */);

        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getType()).thenReturn("googleSheets/v1/insertRow");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(input));
        when(workflow.getTasks(false)).thenReturn(List.of(workflowTask));
        when(workflowService.getWorkflow("wf-id")).thenReturn(workflow);

        EmbeddedWorkflowInputOptionFacadeImpl facade = new EmbeddedWorkflowInputOptionFacadeImpl(
            actionDefinitionFacade, integrationInstanceService, integrationWorkflowService, triggerDefinitionFacade,
            workflowService);

        assertThrows(
            IllegalArgumentException.class,
            () -> facade.getWorkflowInputOptions(7L, "wf-uuid", "channel", "channelId", Map.of(), null));
    }
}
