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
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Pins the identity/locator resolvers that route the {@code 'Workspace'}/{@code 'Project'}/{@code 'Workflow'} tokens
 * through {@code hasResourceScope}. None populate {@code ownerUserId}, so CE treats them as shared (permissive).
 *
 * @author Ivica Cardic
 */
class WorkspaceProjectWorkflowOwnershipResolverTest {

    @Test
    void testWorkspaceResolverIsIdentity() {
        ResourceOwner owner = new WorkspaceOwnershipResolver().resolveOwner(42L);

        assertThat(owner.workspaceId()).hasValue(42L);
        assertThat(owner.ownerUserId()).isEmpty();
    }

    @Test
    void testProjectResolverMapsToOwningWorkspace() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        Project project = new Project();

        project.setWorkspaceId(7L);

        when(projectRepository.findById(3L)).thenReturn(Optional.of(project));

        assertThat(new ProjectOwnershipResolver(projectRepository).resolveOwner(3L)
            .workspaceId()).hasValue(7L);
    }

    @Test
    void testProjectResolverFailsClosedForUnknownProject() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);

        when(projectRepository.findById(3L)).thenReturn(Optional.empty());

        assertThat(new ProjectOwnershipResolver(projectRepository).resolveOwner(3L)
            .workspaceId()).isEmpty();
    }

    @Test
    void testWorkflowResolverMapsStringUuidToOwningWorkspace() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        Project project = new Project();

        project.setWorkspaceId(9L);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));

        // The String UUID flows through the Serializable overload; the numeric overload is unreachable for workflows.
        ResourceOwner owner =
            new WorkflowOwnershipResolver(projectRepository).resolveOwner((java.io.Serializable) "wf-uuid");

        assertThat(owner.workspaceId()).hasValue(9L);
        assertThat(owner.ownerUserId()).isEmpty();
    }

    @Test
    void testWorkflowResolverFailsClosedForNonStringId() {
        WorkflowOwnershipResolver resolver = new WorkflowOwnershipResolver(mock(ProjectRepository.class));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).isEmpty();
        assertThat(resolver.resolveOwner((java.io.Serializable) Long.valueOf(1L))
            .workspaceId()).isEmpty();
    }

    @Test
    void testSpiDefaultDelegatesNumericIdToLongOverload() {
        // The default Serializable resolver delegates Number ids to resolveOwner(long).
        ResourceOwner owner = new WorkspaceOwnershipResolver().resolveOwner((java.io.Serializable) Long.valueOf(5L));

        assertThat(owner.workspaceId()).hasValue(5L);
    }
}
