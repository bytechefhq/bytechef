/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.promotion.handler.ProjectWorkflowMappingReconciler.Mapping;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectWorkflowMappingReconcilerTest {

    private static final long PROJECT_ID = 42L;
    private static final long SOURCE_PROJECT_DEPLOYMENT_ID = 200L;
    private static final int SOURCE_PROJECT_VERSION = 3;
    private static final long TARGET_PROJECT_DEPLOYMENT_ID = 300L;
    private static final int TARGET_PROJECT_VERSION = 2;
    private static final String KEPT_WORKFLOW_UUID = "9a8b7c6d-2222-4e3f-8a1b-0c9d8e7f6a5b";
    private static final String DROPPED_WORKFLOW_UUID = "1b2c3d4e-3333-4f5a-9b8c-7d6e5f4a3b2c";

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Test
    void testDeleteStaleMappingsKeepsMappingsWhoseWorkflowTheSourceStillExposes() {
        stubSource(List.of(projectDeploymentWorkflow(210L, "source-kept")));
        stubTarget(List.of(projectDeploymentWorkflow(310L, "target-kept")));
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, Map.of("source-kept", KEPT_WORKFLOW_UUID));
        stubProjectWorkflows(TARGET_PROJECT_VERSION, Map.of("target-kept", KEPT_WORKFLOW_UUID));

        assertThat(deletedMappingIds(List.of(new Mapping(320L, 310L)))).isEmpty();
    }

    @Test
    void testDeleteStaleMappingsDeletesMappingsWhoseWorkflowTheSourceNoLongerExposes() {
        stubSource(List.of(projectDeploymentWorkflow(210L, "source-kept")));
        stubTarget(
            List.of(projectDeploymentWorkflow(310L, "target-kept"), projectDeploymentWorkflow(311L, "target-dropped")));
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, Map.of("source-kept", KEPT_WORKFLOW_UUID));
        stubProjectWorkflows(
            TARGET_PROJECT_VERSION,
            Map.of("target-kept", KEPT_WORKFLOW_UUID, "target-dropped", DROPPED_WORKFLOW_UUID));

        assertThat(deletedMappingIds(List.of(new Mapping(320L, 310L), new Mapping(321L, 311L))))
            .containsExactly(321L);
    }

    /**
     * A target row whose workflow left the target's own project version resolves to no lineage uuid, so the sync cannot
     * match it either and will delete it — its mapping row has to go first regardless.
     */
    @Test
    void testDeleteStaleMappingsDeletesMappingsWhoseWorkflowIsAbsentFromTheTargetVersion() {
        stubSource(List.of(projectDeploymentWorkflow(210L, "source-kept")));
        stubTarget(List.of(projectDeploymentWorkflow(310L, "target-unknown")));
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, Map.of("source-kept", KEPT_WORKFLOW_UUID));
        stubProjectWorkflows(TARGET_PROJECT_VERSION, Map.of());

        assertThat(deletedMappingIds(List.of(new Mapping(320L, 310L)))).containsExactly(320L);
    }

    @Test
    void testDeleteStaleMappingsDeletesMappingsPointingAtNoDeploymentWorkflowAtAll() {
        stubSource(List.of(projectDeploymentWorkflow(210L, "source-kept")));
        stubTarget(List.of());
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, Map.of("source-kept", KEPT_WORKFLOW_UUID));
        stubProjectWorkflows(TARGET_PROJECT_VERSION, Map.of());

        assertThat(deletedMappingIds(List.of(new Mapping(320L, 999L)))).containsExactly(320L);
    }

    @Test
    void testSyncMappingsOverwritesParametersOfAnExistingMapping() {
        Map<Long, Map<String, ?>> updatedParameters = new LinkedHashMap<>();

        reconciler().syncMappings(
            List.of(new Mapping(320L, 310L)), Map.of(210L, 310L), Map.of(210L, Map.of("toolName", "getOrders")),
            projectDeploymentWorkflowId -> {
                throw new AssertionError("no mapping should be created");
            },
            updatedParameters::put);

        assertThat(updatedParameters).containsExactly(Map.entry(320L, Map.of("toolName", "getOrders")));
    }

    @Test
    void testSyncMappingsCreatesAMappingTheTargetDoesNotHaveYet() {
        List<Long> createdForProjectDeploymentWorkflowIds = new ArrayList<>();
        Map<Long, Map<String, ?>> updatedParameters = new LinkedHashMap<>();

        reconciler().syncMappings(
            List.of(), Map.of(210L, 310L), Map.of(210L, Map.of("toolName", "getOrders")),
            projectDeploymentWorkflowId -> {
                createdForProjectDeploymentWorkflowIds.add(projectDeploymentWorkflowId);

                return 999L;
            },
            updatedParameters::put);

        assertThat(createdForProjectDeploymentWorkflowIds).containsExactly(310L);
        assertThat(updatedParameters).containsExactly(Map.entry(999L, Map.of("toolName", "getOrders")));
    }

    /**
     * A source workflow the sync dropped has no target counterpart, so there is no id to create a mapping against.
     */
    @Test
    void testSyncMappingsSkipsASourceMappingWithNoTargetCounterpart() {
        Map<Long, Map<String, ?>> updatedParameters = new LinkedHashMap<>();

        reconciler().syncMappings(
            List.of(), Map.of(), Map.of(210L, Map.of("toolName", "getOrders")),
            projectDeploymentWorkflowId -> {
                throw new AssertionError("no mapping should be created");
            },
            updatedParameters::put);

        assertThat(updatedParameters).isEmpty();
    }

    /**
     * A target deployment workflow with no source mapping is left without one: the mapping rows are what expose a
     * workflow as a tool, and the sync's id mapping covers every workflow of the deployment, not just the exposed ones.
     */
    @Test
    void testSyncMappingsDoesNotExposeATargetWorkflowTheSourceDoesNotMap() {
        Map<Long, Map<String, ?>> updatedParameters = new LinkedHashMap<>();

        reconciler().syncMappings(
            List.of(), Map.of(210L, 310L, 211L, 311L), Map.of(210L, Map.of()),
            projectDeploymentWorkflowId -> 999L, updatedParameters::put);

        assertThat(updatedParameters).containsExactly(Map.entry(999L, Map.of()));
    }

    private List<Long> deletedMappingIds(List<Mapping> mappings) {
        List<Long> deletedMappingIds = new ArrayList<>();

        reconciler().deleteStaleMappings(
            projectDeployment(SOURCE_PROJECT_DEPLOYMENT_ID, SOURCE_PROJECT_VERSION),
            projectDeployment(TARGET_PROJECT_DEPLOYMENT_ID, TARGET_PROJECT_VERSION), mappings,
            deletedMappingIds::add);

        return deletedMappingIds;
    }

    private ProjectWorkflowMappingReconciler reconciler() {
        return new ProjectWorkflowMappingReconciler(projectDeploymentWorkflowService, projectWorkflowService);
    }

    private void stubProjectWorkflows(int projectVersion, Map<String, String> uuidsByWorkflowId) {
        List<ProjectWorkflow> projectWorkflows = new ArrayList<>();

        for (Map.Entry<String, String> entry : uuidsByWorkflowId.entrySet()) {
            projectWorkflows.add(projectWorkflow(projectVersion, entry.getKey(), entry.getValue()));
        }

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, projectVersion)).thenReturn(projectWorkflows);
    }

    private void stubSource(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(SOURCE_PROJECT_DEPLOYMENT_ID))
            .thenReturn(projectDeploymentWorkflows);
    }

    private void stubTarget(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(TARGET_PROJECT_DEPLOYMENT_ID))
            .thenReturn(projectDeploymentWorkflows);
    }

    private static ProjectDeployment projectDeployment(long id, int projectVersion) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(projectVersion);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(long id, String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static ProjectWorkflow projectWorkflow(int projectVersion, String workflowId, String workflowUuid) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(PROJECT_ID, projectVersion, workflowId);

        projectWorkflow.setUuid(UUID.fromString(workflowUuid));

        return projectWorkflow;
    }
}
