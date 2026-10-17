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

package com.bytechef.automation.configuration.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.validator.WorkflowDeploymentConnectionResolver.WorkflowNodeConnection;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectDeploymentWorkflowConnectionResolverTest {

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectDeploymentWorkflowConnectionResolver projectDeploymentWorkflowConnectionResolver =
        new ProjectDeploymentWorkflowConnectionResolver(projectDeploymentService, projectDeploymentWorkflowService);

    @Test
    void testGetWorkflowNodeConnectionsReturnsTheNodeConnectionsOfTheEnvironmentDeployment() {
        ProjectDeploymentWorkflow developmentProjectDeploymentWorkflow = projectDeploymentWorkflow(
            1L,
            new ProjectDeploymentWorkflowConnection(7L, "openAi_1", "aiAgent_1"),
            new ProjectDeploymentWorkflowConnection(8L, "affinity", "affinity_1"));
        ProjectDeploymentWorkflow productionProjectDeploymentWorkflow = projectDeploymentWorkflow(
            2L, new ProjectDeploymentWorkflowConnection(9L, "openAi_1", "aiAgent_1"));

        when(projectDeploymentWorkflowService.getWorkflowProjectDeploymentWorkflows("wf-1"))
            .thenReturn(List.of(developmentProjectDeploymentWorkflow, productionProjectDeploymentWorkflow));
        when(projectDeploymentService.getProjectDeployment(1L))
            .thenReturn(projectDeployment(Environment.DEVELOPMENT));
        when(projectDeploymentService.getProjectDeployment(2L))
            .thenReturn(projectDeployment(Environment.PRODUCTION));

        List<WorkflowNodeConnection> workflowNodeConnections =
            projectDeploymentWorkflowConnectionResolver.getWorkflowNodeConnections(
                "wf-1", "aiAgent_1", Environment.DEVELOPMENT.ordinal());

        assertThat(workflowNodeConnections).containsExactly(new WorkflowNodeConnection(7L, "openAi_1"));
    }

    @Test
    void testGetWorkflowNodeConnectionsWithoutDeploymentReturnsNothing() {
        when(projectDeploymentWorkflowService.getWorkflowProjectDeploymentWorkflows("wf-1"))
            .thenReturn(List.of());

        assertThat(
            projectDeploymentWorkflowConnectionResolver.getWorkflowNodeConnections(
                "wf-1", "aiAgent_1", Environment.DEVELOPMENT.ordinal()))
                    .isEmpty();
    }

    private static ProjectDeployment projectDeployment(Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(
        long projectDeploymentId, ProjectDeploymentWorkflowConnection... connections) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(List.of(connections));
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);

        return projectDeploymentWorkflow;
    }
}
