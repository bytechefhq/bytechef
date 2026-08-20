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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The {@code "Project"} literals below are deliberately hardcoded rather than read from
 * {@link ProjectVisibilityFilter#PROJECT}: they are what pins the discriminator's VALUE, which dozens of other sites
 * spell out by hand. The registry fails closed on an unknown type, so renaming the constant without renaming those
 * literals would leave projects with no policy at all — and these assertions red.
 *
 * <p>
 * The hand-spelled sites are of two kinds. Those that <em>could</em> use the constant and do not:
 * {@code ProjectOwnershipResolver.resourceType()}, the {@code hasWorkspaceScopeForProject} overloads in both
 * {@code PermissionServiceImpl}s, and {@code ProjectWorkflowExecutionFacadeImpl.requireResourceScope}. And those that
 * <em>cannot</em>, which is the larger half: every {@code hasPermission(#id, 'Project', …)} and
 * {@code isResourceOwner('Project', …)} in a {@code @PreAuthorize}, since a SpEL string literal in an annotation cannot
 * reference a constant. The five visibility providers are in neither group: {@code ProjectVisibilityProvider},
 * {@code ProjectWorkflowVisibilityProvider}, {@code ProjectDeploymentVisibilityProvider},
 * {@code WorkflowVisibilityProvider} and {@code JobVisibilityProvider} all return
 * {@link ProjectVisibilityFilter#PROJECT} and would follow a rename on their own.
 */
class ProjectVisibilityPolicyTest {

    private final ResourceVisibilityPolicyRegistry registry =
        new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy()));

    @Test
    void testDefaultIsWorkspace() {
        assertThat(registry.defaultVisibility("Project")).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    /**
     * There are two defaults, and only two write paths consult the policy one ({@code applyCreateVisibility} and
     * {@code setProjectVisibility}). Everything else that creates a project — {@code duplicateProject}, the AI
     * {@code createProject} tool, the three system-project creators, the embedded bridge, the demo seed — reaches
     * {@code projectService.create} directly and lands on the entity-field default instead. Harmless while the two
     * agree; nothing pinned that they do, so a change to either one alone would silently give those paths a different
     * reach from the ones that ask.
     */
    @Test
    void testTheEntityFieldDefaultAgreesWithThePolicyDefault() {
        Project project = new Project();

        assertThat(project.getVisibility()).isEqualTo(registry.defaultVisibility("Project"));
    }

    @Test
    void testOrganizationIsNotSupported() {
        assertThat(registry.supports("Project", ResourceVisibility.PRIVATE)).isTrue();
        assertThat(registry.supports("Project", ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(registry.supports("Project", ResourceVisibility.ORGANIZATION))
            .as("a project belongs to one workspace; there is no representation for it outside that workspace")
            .isFalse();
    }
}
