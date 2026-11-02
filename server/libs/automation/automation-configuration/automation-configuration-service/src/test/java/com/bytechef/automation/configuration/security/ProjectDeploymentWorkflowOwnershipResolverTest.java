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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The {@code 'ProjectDeploymentWorkflow'} token is only a check because this resolver is registered for it —
 * {@code PermissionService.hasResourceScope} denies every non-tenant-admin when the registry lookup misses, so an
 * unregistered token is a lockout that reads as protection. Both hops must fail closed, since the row can outlive
 * neither its deployment nor its project but a stale id can still arrive from a caller.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentWorkflowOwnershipResolverTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 77L;
    private static final long WORKSPACE_ID = 9L;

    private final ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository =
        mock(ProjectDeploymentWorkflowRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectDeploymentWorkflowOwnershipResolver resolver =
        new ProjectDeploymentWorkflowOwnershipResolver(projectDeploymentWorkflowRepository, projectRepository);

    @Test
    void testResourceTypeMatchesTheTokenTheDeploymentWorkflowGuardNames() {
        assertThat(resolver.resourceType()).isEqualTo("ProjectDeploymentWorkflow");
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceOwningTheRowsProject() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        givenRowBelongingToDeployment();

        when(projectRepository.findByProjectDeploymentId(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveOwner(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .isEqualTo(ResourceOwner.ofWorkspace(WORKSPACE_ID));
    }

    @Test
    void testResolveOwnerFailsClosedForAnUnknownRow() {
        when(projectDeploymentWorkflowRepository.findById(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(PROJECT_DEPLOYMENT_WORKFLOW_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheDeploymentsProjectIsMissing() {
        givenRowBelongingToDeployment();

        when(projectRepository.findByProjectDeploymentId(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(PROJECT_DEPLOYMENT_WORKFLOW_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedForANonNumericId() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }

    private void givenRowBelongingToDeployment() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        when(projectDeploymentWorkflowRepository.findById(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(Optional.of(projectDeploymentWorkflow));
    }
}
