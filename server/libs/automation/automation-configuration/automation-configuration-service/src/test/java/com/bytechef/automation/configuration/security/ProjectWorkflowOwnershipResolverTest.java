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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectWorkflowOwnershipResolverTest {

    private static final long PROJECT_ID = 11L;
    private static final long PROJECT_WORKFLOW_ID = 77L;
    private static final long WORKSPACE_ID = 9L;

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectWorkflowRepository projectWorkflowRepository = mock(ProjectWorkflowRepository.class);
    private final ProjectWorkflowOwnershipResolver resolver =
        new ProjectWorkflowOwnershipResolver(projectRepository, projectWorkflowRepository);

    @Test
    void testResourceTypeMatchesTheTokenTheProjectWorkflowGuardNames() {
        assertThat(resolver.resourceType()).isEqualTo("ProjectWorkflow");
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceOwningTheRowsProject() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        givenRowBelongingToProject();

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveOwner(PROJECT_WORKFLOW_ID)).isEqualTo(ResourceOwner.ofWorkspace(WORKSPACE_ID));
    }

    @Test
    void testResolveOwnerFailsClosedForAnUnknownRow() {
        when(projectWorkflowRepository.findById(PROJECT_WORKFLOW_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(PROJECT_WORKFLOW_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheProjectIsMissing() {
        givenRowBelongingToProject();

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(PROJECT_WORKFLOW_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveProjectIdOfAKnownProjectWorkflow() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(WORKSPACE_ID);

        givenRowBelongingToProject();

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveProjectId(PROJECT_WORKFLOW_ID)).hasValue(PROJECT_ID);
    }

    @Test
    void testResolveProjectIdOfAnUnknownProjectWorkflowIsEmpty() {
        when(projectWorkflowRepository.findById(PROJECT_WORKFLOW_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolveProjectId(PROJECT_WORKFLOW_ID)).isEmpty();
    }

    private void givenRowBelongingToProject() {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(PROJECT_ID, 1, "workflow-1");

        when(projectWorkflowRepository.findById(PROJECT_WORKFLOW_ID)).thenReturn(Optional.of(projectWorkflow));
    }
}
