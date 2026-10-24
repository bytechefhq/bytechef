/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.ai.tool.model.CreatedIntegrationWorkflowInfo;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationWorkflowInfo;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationWorkflowSaveInfo;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class IntegrationWorkflowToolsTest {

    private static final String DEFINITION = """
        {"label": "My Flow", "triggers": [], "tasks": []}""";

    private static final String WORKFLOW_DTO_DEFINITION = """
        {"label": "My Flow", "tasks": []}""";

    @Mock
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @Mock
    private WorkflowValidatorFacade workflowValidatorFacade;

    @Test
    void testGetWorkflow() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf-1"))
            .thenReturn(buildDto("wf-1", 55L, 2, 3));

        IntegrationWorkflowInfo result = newTools().getWorkflow("wf-1");

        assertThat(result.workflowId()).isEqualTo("wf-1");
        assertThat(result.integrationWorkflowId()).isEqualTo(55L);
        assertThat(result.name()).isEqualTo("My Flow");
        assertThat(result.version()).isEqualTo(3);
    }

    @Test
    void testGetWorkflowThrowsOnFailure() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf-1"))
            .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> newTools().getWorkflow("wf-1"))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Failed to get workflow");
    }

    @Test
    void testListWorkflows() {
        when(integrationWorkflowFacade.getIntegrationWorkflows(7L))
            .thenReturn(List.of(buildDto("wf-1", 1L, 1, 1), buildDto("wf-2", 2L, 1, 1)));

        List<IntegrationWorkflowInfo> result = newTools().listWorkflows(7L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)
            .workflowId()).isEqualTo("wf-1");
    }

    @Test
    void testSearchWorkflowsScopedToIntegration() {
        when(integrationWorkflowFacade.getIntegrationWorkflows(7L))
            .thenReturn(List.of(buildDto("wf-1", 1L, 1, 1)));

        List<IntegrationWorkflowInfo> result = newTools().searchWorkflows("flow", 7L);

        assertThat(result).hasSize(1);

        verify(integrationWorkflowFacade, never()).getIntegrationWorkflows();
    }

    @Test
    void testSearchWorkflowsAcrossTenantWhenNoIntegrationId() {
        when(integrationWorkflowFacade.getIntegrationWorkflows())
            .thenReturn(List.of(buildDto("wf-1", 1L, 1, 1), buildDto("wf-2", 2L, 1, 1)));

        List<IntegrationWorkflowInfo> result = newTools().searchWorkflows("my flow", null);

        assertThat(result).hasSize(2);
    }

    @Test
    void testSearchWorkflowsFiltersByQuery() {
        when(integrationWorkflowFacade.getIntegrationWorkflows())
            .thenReturn(List.of(buildDto("wf-1", 1L, 1, 1)));

        List<IntegrationWorkflowInfo> result = newTools().searchWorkflows("nomatch", null);

        assertThat(result).isEmpty();
    }

    @Test
    void testCreateIntegrationWorkflow() {
        when(integrationWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(55L);
        when(integrationWorkflowFacade.getIntegrationWorkflow(55L)).thenReturn(buildDto("wf-1", 55L, 2, 1));

        CreatedIntegrationWorkflowInfo result = newTools().createIntegrationWorkflow(7L, DEFINITION);

        assertThat(result.integrationWorkflowId()).isEqualTo(55L);
        assertThat(result.integrationId()).isEqualTo(7L);
        assertThat(result.integrationVersion()).isEqualTo(2);
        assertThat(result.workflowId()).isEqualTo("wf-1");
    }

    @Test
    void testCreateIntegrationWorkflowThrowsOnFailure() {
        when(integrationWorkflowFacade.addWorkflow(7L, DEFINITION)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> newTools().createIntegrationWorkflow(7L, DEFINITION))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Failed to create integration workflow");
    }

    @Test
    void testDeleteWorkflow() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf-1")).thenReturn(buildDto("wf-1", 55L, 1, 1));

        String result = newTools().deleteWorkflow("wf-1");

        verify(integrationWorkflowFacade).deleteWorkflow("wf-1");
        assertThat(result).contains("My Flow", "wf-1");
    }

    @Test
    void testUpdateWorkflow() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf-1")).thenReturn(buildDto("wf-1", 55L, 1, 3));
        when(integrationWorkflowFacade.updateWorkflow("wf-1", DEFINITION, 3))
            .thenReturn(buildDto("wf-1", 55L, 1, 4));

        IntegrationWorkflowSaveInfo result = newTools().updateWorkflow("wf-1", DEFINITION, 3);

        verify(integrationWorkflowFacade).updateWorkflow("wf-1", DEFINITION, 3);
        assertThat(result.version()).isEqualTo(4);
    }

    @Test
    void testUpdateWorkflowSavesNothingWhenTheDefinitionHasErrors() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf-1")).thenReturn(buildDto("wf-1", 55L, 1, 3));
        when(workflowValidatorFacade.validateWorkflow(eq(DEFINITION), eq("wf-1"), anyLong())).thenReturn(
            new WorkflowValidatorFacade.WorkflowValidationResult(
                List.of("[slack_1] Field 'channel' is required"), List.of()));

        assertThatThrownBy(() -> newTools(workflowValidatorFacade).updateWorkflow("wf-1", DEFINITION, 3))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Nothing was saved")
            .hasMessageContaining("[slack_1] Field 'channel' is required");

        verify(integrationWorkflowFacade, never()).updateWorkflow("wf-1", DEFINITION, 3);
    }

    @Test
    void testCreateIntegrationWorkflowSavesNothingWhenTheDefinitionHasErrors() {
        when(workflowValidatorFacade.validateWorkflow(eq(DEFINITION), isNull(), anyLong())).thenReturn(
            new WorkflowValidatorFacade.WorkflowValidationResult(
                List.of("[slack_1] Field 'channel' is required"), List.of()));

        assertThatThrownBy(() -> newTools(workflowValidatorFacade).createIntegrationWorkflow(7L, DEFINITION))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Nothing was saved");

        verify(integrationWorkflowFacade, never()).addWorkflow(7L, DEFINITION);
    }

    @Test
    void testUpdateWorkflowRejectsStaleVersion() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf-1")).thenReturn(buildDto("wf-1", 55L, 1, 5));

        assertThatThrownBy(() -> newTools().updateWorkflow("wf-1", DEFINITION, 3))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("was modified after version 3 was read")
            .hasMessageContaining("getWorkflow");

        verify(integrationWorkflowFacade, never()).updateWorkflow("wf-1", DEFINITION, 3);
        verify(integrationWorkflowFacade, never()).updateWorkflow("wf-1", DEFINITION, 5);
    }

    @Test
    void testUpdateWorkflowReportsConcurrentModification() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf-1")).thenReturn(buildDto("wf-1", 55L, 1, 3));
        when(integrationWorkflowFacade.updateWorkflow("wf-1", DEFINITION, 3))
            .thenThrow(new OptimisticLockingFailureException("stale"));

        assertThatThrownBy(() -> newTools().updateWorkflow("wf-1", DEFINITION, 3))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("was modified after version 3 was read")
            .hasCauseInstanceOf(OptimisticLockingFailureException.class);
    }

    private IntegrationWorkflowTools newTools() {
        return newTools(null);
    }

    @SuppressWarnings("unchecked")
    private IntegrationWorkflowTools newTools(WorkflowValidatorFacade workflowValidatorFacade) {
        ObjectProvider<WorkflowValidatorFacade> workflowValidatorFacadeProvider = mock(ObjectProvider.class);

        if (workflowValidatorFacade != null) {
            when(workflowValidatorFacadeProvider.getIfAvailable()).thenReturn(workflowValidatorFacade);
        }

        return new IntegrationWorkflowTools(integrationWorkflowFacade, workflowValidatorFacadeProvider);
    }

    private static IntegrationWorkflowDTO buildDto(
        String workflowId, long integrationWorkflowId, int integrationVersion, int version) {

        Workflow workflow = new Workflow(workflowId, WORKFLOW_DTO_DEFINITION, Workflow.Format.JSON);

        workflow.setVersion(version);

        IntegrationWorkflow integrationWorkflow =
            new IntegrationWorkflow(1L, integrationVersion, workflowId, UUID.randomUUID());

        ReflectionTestUtils.setField(integrationWorkflow, "id", integrationWorkflowId);

        return new IntegrationWorkflowDTO(workflow, integrationWorkflow);
    }
}
