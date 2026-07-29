/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.ai.tool.model.CreatedIntegrationWorkflowInfo;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationWorkflowInfo;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.exception.ExecutionException;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 *
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

    @Test
    void testGetWorkflow() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf-1"))
            .thenReturn(buildDto("wf-1", 55L, 2, 3));

        IntegrationWorkflowInfo result = newTools().getWorkflow("wf-1");

        assertThat(result.id()).isEqualTo("wf-1");
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
            .id()).isEqualTo("wf-1");
    }

    @Test
    void testSearchWorkflowsScopedToIntegration() {
        when(integrationWorkflowFacade.getIntegrationWorkflows(7L))
            .thenReturn(List.of(buildDto("wf-1", 1L, 1, 1)));

        List<IntegrationWorkflowInfo> result = newTools().searchWorkflows("flow", 7L);

        assertThat(result).hasSize(1);

        // The whole-tenant enumeration must never be used when an integration id is supplied.
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

        assertThat(result.id()).isEqualTo(55L);
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

        IntegrationWorkflowInfo result = newTools().updateWorkflow("wf-1", DEFINITION);

        verify(integrationWorkflowFacade).updateWorkflow("wf-1", DEFINITION, 3);
        assertThat(result.version()).isEqualTo(4);
    }

    private IntegrationWorkflowTools newTools() {
        return new IntegrationWorkflowTools(integrationWorkflowFacade);
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
