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
 * The {@code 'ProjectDeployment'} token is only a check because this resolver is registered for it —
 * {@code PermissionService.hasResourceScope} denies every non-tenant-admin when the registry lookup misses, so an
 * unregistered token is a lockout that reads as protection. These assertions pin both the discriminator the guards name
 * and the fail-closed behaviour the SPI contract requires.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentOwnershipResolverTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectDeploymentOwnershipResolver resolver =
        new ProjectDeploymentOwnershipResolver(projectRepository);

    @Test
    void testResourceTypeMatchesTheTokenTheDeploymentGuardsName() {
        assertThat(resolver.resourceType()).isEqualTo("ProjectDeployment");
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceOwningTheDeploymentsProject() {
        Project project = new Project();

        project.setWorkspaceId(9L);

        when(projectRepository.findByProjectDeploymentId(11L)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveOwner(11L)).isEqualTo(ResourceOwner.ofWorkspace(9L));
    }

    @Test
    void testResolveOwnerFailsClosedForAnUnknownDeployment() {
        when(projectRepository.findByProjectDeploymentId(11L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(11L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedForANonNumericId() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }
}
