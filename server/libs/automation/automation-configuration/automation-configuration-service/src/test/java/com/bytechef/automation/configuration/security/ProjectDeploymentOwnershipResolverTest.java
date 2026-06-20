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
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectDeploymentOwnershipResolverTest {

    private final ProjectDeploymentRepository projectDeploymentRepository = mock(ProjectDeploymentRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectDeploymentOwnershipResolver resolver = new ProjectDeploymentOwnershipResolver(
        projectDeploymentRepository, projectRepository);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("ProjectDeployment");
    }

    @Test
    void testResolvesWorkspaceViaProject() {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);

        when(projectDeployment.getProjectId()).thenReturn(5L);
        when(projectDeploymentRepository.findById(1L)).thenReturn(Optional.of(projectDeployment));

        Project project = mock(Project.class);

        when(project.getWorkspaceId()).thenReturn(42L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
    }

    @Test
    void testUnknownDeploymentIsUnknown() {
        when(projectDeploymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }
}
