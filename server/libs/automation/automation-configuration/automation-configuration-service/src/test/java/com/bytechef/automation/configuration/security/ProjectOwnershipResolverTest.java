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
 * {@code 'Project'} is the most-used token in the tree, and it is only a check because this resolver is registered for
 * it: {@code PermissionService.hasResourceScope} denies every non-tenant-admin when the registry lookup misses, so a
 * typo in {@link ProjectOwnershipResolver#resourceType()} or a lost {@code @Component} turns every project guard into a
 * lockout while tenant-admin manual testing still passes, because {@code isTenantAdmin()} short-circuits ahead of the
 * lookup.
 *
 * @author Ivica Cardic
 */
class ProjectOwnershipResolverTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectOwnershipResolver resolver = new ProjectOwnershipResolver(projectRepository);

    @Test
    void testResourceTypeMatchesTheTokenTheProjectGuardsName() {
        assertThat(resolver.resourceType()).isEqualTo("Project");
    }

    @Test
    void testResolveOwnerReturnsTheProjectsWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(9L);

        when(projectRepository.findById(3L)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveOwner(3L)).isEqualTo(ResourceOwner.ofWorkspace(9L));
    }

    @Test
    void testResolveOwnerFailsClosedForAnUnknownProject() {
        when(projectRepository.findById(3L)).thenReturn(Optional.empty());

        // Fails closed rather than throwing: the SPI contract requires unknown(), and an exception here would surface a
        // deleted project as an INTERNAL_ERROR instead of a denial.
        assertThat(resolver.resolveOwner(3L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedForAProjectWithNoWorkspace() {
        when(projectRepository.findById(3L)).thenReturn(Optional.of(new Project()));

        assertThat(resolver.resolveOwner(3L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedForANonNumericId() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }
}
