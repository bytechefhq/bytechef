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
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
class ProjectOwnershipResolverTest {

    @Test
    void testResourceType() {
        assertThat(new ProjectOwnershipResolver(mock(ProjectRepository.class), mock(UserService.class)).resourceType())
            .isEqualTo("Project");
    }

    @Test
    void testResolvesWorkspaceAndOwnerFromCreatedBy() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        UserService userService = mock(UserService.class);
        Project project = new Project();

        project.setId(1L);
        project.setWorkspaceId(4L);

        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        User user = new User();

        user.setId(77L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userService.fetchUserByLogin("ivica")).thenReturn(Optional.of(user));

        ResourceOwner owner = new ProjectOwnershipResolver(projectRepository, userService).resolveOwner(1L);

        assertThat(owner.workspaceId()).hasValue(4L);
        assertThat(owner.ownerUserId()).hasValue(77L);
    }

    @Test
    void testUnknownCreatorLeavesOwnerUnresolved() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        UserService userService = mock(UserService.class);
        Project project = new Project();

        project.setId(2L);
        project.setWorkspaceId(4L);

        ReflectionTestUtils.setField(project, "createdBy", "departed");

        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(userService.fetchUserByLogin("departed")).thenReturn(Optional.empty());

        ResourceOwner owner = new ProjectOwnershipResolver(projectRepository, userService).resolveOwner(2L);

        assertThat(owner.workspaceId())
            .as("a deleted creator must not cost the project its workspace")
            .hasValue(4L);
        assertThat(owner.ownerUserId()).isEmpty();
    }

    @Test
    void testUnknownProjectIsUnknownOwner() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);

        when(projectRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(new ProjectOwnershipResolver(projectRepository, mock(UserService.class)).resolveOwner(9L))
            .isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testOwnerIsResolvedRegardlessOfVisibility() {
        ResourceOwner sharedProjectOwner = resolveOwnerOfProjectStoredAs(ResourceVisibility.WORKSPACE);
        ResourceOwner withheldProjectOwner = resolveOwnerOfProjectStoredAs(ResourceVisibility.PRIVATE);

        assertThat(sharedProjectOwner.ownerUserId())
            .as("a WORKSPACE-visible project must still resolve an owner")
            .hasValue(77L);

        // The owner-user lookup is a third query on the authorization path, and skipping it for WORKSPACE-visible
        // projects is the obvious optimisation. It was deliberately rejected: an owner must be able to WITHHOLD a
        // project that is currently shared, and setProjectVisibility's guard is
        // isResourceOwner('Project', #projectId), which this resolver answers. Take the shortcut and that path stops
        // working for exactly the projects it exists for. Comparing the two resolutions pins it as one fact rather
        // than as two assertions that could drift apart.
        assertThat(sharedProjectOwner)
            .as("visibility must not influence owner resolution")
            .isEqualTo(withheldProjectOwner);
    }

    private static ResourceOwner resolveOwnerOfProjectStoredAs(ResourceVisibility visibility) {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        UserService userService = mock(UserService.class);
        Project project = new Project();

        project.setId(1L);
        project.setVisibility(visibility);
        project.setWorkspaceId(4L);

        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        User user = new User();

        user.setId(77L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userService.fetchUserByLogin("ivica")).thenReturn(Optional.of(user));

        return new ProjectOwnershipResolver(projectRepository, userService).resolveOwner(1L);
    }
}
