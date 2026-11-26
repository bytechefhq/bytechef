/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.ee.automation.configuration.promotion.deployment.ProjectWorkflowMappingReconciler.MappingPlan;
import com.bytechef.ee.automation.configuration.promotion.deployment.ServerProjectPromoter.ServerProjectStore;
import com.bytechef.ee.automation.configuration.promotion.deployment.ServerProjectPromoter.SourceProjectView;
import com.bytechef.ee.automation.configuration.promotion.deployment.ServerProjectPromoter.TargetProject;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ServerProjectPromoterTest {

    private static final String PROJECT_ASSET_TYPE = "MCP_PROJECT";
    private static final long PROJECT_ID = 7L;
    private static final String PROJECT_NAME = "Billing";
    private static final long SERVER_PROJECT_ID = 300L;
    private static final UUID SOURCE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TARGET_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ProjectDeploymentPromoter projectDeploymentPromoter = mock(ProjectDeploymentPromoter.class);
    private final ProjectWorkflowMappingReconciler projectWorkflowMappingReconciler =
        mock(ProjectWorkflowMappingReconciler.class);
    private final ServerProjectStore serverProjectStore = mock(ServerProjectStore.class);

    private final ServerProjectPromoter serverProjectPromoter = new ServerProjectPromoter(
        projectDeploymentPromoter, projectWorkflowMappingReconciler);

    @Test
    void testVersionOnlyChangeEmitsProjectUpdate() {
        stubNoWorkflowChanges();

        List<PromotionChange> changes = predictProjectChanges(
            projectDeployment(100L, Environment.DEVELOPMENT, 2, SOURCE_UUID),
            projectDeployment(200L, Environment.STAGING, 1, TARGET_UUID));

        assertThat(changes).containsExactly(
            new PromotionChange(
                Kind.UPDATE, PROJECT_ASSET_TYPE, PROJECT_ID + ":" + TARGET_UUID, null, PROJECT_NAME, false));
    }

    @Test
    void testSameVersionEmitsNoProjectChange() {
        stubNoWorkflowChanges();

        List<PromotionChange> changes = predictProjectChanges(
            projectDeployment(100L, Environment.DEVELOPMENT, 2, SOURCE_UUID),
            projectDeployment(200L, Environment.STAGING, 2, TARGET_UUID));

        assertThat(changes).isEmpty();
    }

    private List<PromotionChange> predictProjectChanges(
        ProjectDeployment sourceProjectDeployment, ProjectDeployment targetProjectDeployment) {

        Map<Long, Deque<TargetProject>> targetProjectsByProjectId = ServerProjectPromoter.groupByProjectId(
            List.of(new TargetProject(SERVER_PROJECT_ID, targetProjectDeployment)));

        return serverProjectPromoter.predictProjectChanges(
            List.of(new SourceProjectView(sourceProjectDeployment, Map.of(), List::of)), targetProjectsByProjectId,
            PROJECT_ASSET_TYPE, projectId -> PROJECT_NAME, serverProjectStore);
    }

    private void stubNoWorkflowChanges() {
        MappingPlan mappingPlan = mock(MappingPlan.class);

        when(serverProjectStore.mappings(SERVER_PROJECT_ID)).thenReturn(List.of());
        when(projectDeploymentPromoter.predictWorkflowChanges(any(), any(), anyString())).thenReturn(List.of());
        when(projectWorkflowMappingReconciler.planMappings(any(), any(), anyList(), anyMap())).thenReturn(mappingPlan);
        when(projectWorkflowMappingReconciler.predictChanges(any(), anyString())).thenReturn(List.of());
    }

    private static ProjectDeployment projectDeployment(
        long id, Environment environment, int projectVersion, UUID uuid) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);
        projectDeployment.setId(id);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setUuid(uuid);

        return projectDeployment;
    }
}
