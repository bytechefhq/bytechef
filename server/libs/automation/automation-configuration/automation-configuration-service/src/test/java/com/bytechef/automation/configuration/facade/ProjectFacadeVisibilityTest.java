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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ProjectVisibilityPolicy;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the facade's two visibility contracts: every project list surface it owns is narrowed by
 * {@link ProjectVisibilityFilter}, so a project the by-id gates already refuse to open is not advertised in a listing
 * either; and the create path force-writes WORKSPACE in CE while validating the requested rung against the registered
 * {@link ProjectVisibilityPolicy} in EE.
 *
 * @author Ivica Cardic
 */
class ProjectFacadeVisibilityTest {

    private static final long WORKSPACE_ID = 1L;

    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final TagService tagService = mock(TagService.class);

    private ProjectFacadeImpl projectFacade;

    @BeforeEach
    void setUp() {
        projectFacade = facade("CE");

        when(projectService.getProjects(null, null, null, null, null, WORKSPACE_ID))
            .thenReturn(List.of(project(1L), project(2L)));
        when(projectService.getProjects(null, null, false, null, null, WORKSPACE_ID))
            .thenReturn(List.of(project(1L), project(2L)));
        when(projectWorkflowService.getProjectWorkflows(anyList())).thenReturn(List.of());
        when(tagService.getTags(anyList())).thenReturn(List.of());
    }

    @Test
    void testWorkspaceProjectsHideWhatTheFilterHides() {
        List<ProjectDTO> projectDTOs =
            projectFacade.getWorkspaceProjects(null, null, false, null, null, null, WORKSPACE_ID);

        assertThat(projectDTOs).extracting(ProjectDTO::id)
            .containsExactly(1L);
    }

    @Test
    void testWorkspaceProjectWorkflowsOnlySpanVisibleProjects() {
        projectFacade.getWorkspaceProjectWorkflows(WORKSPACE_ID);

        verify(projectWorkflowService).getProjectWorkflows(List.of(1L));
    }

    @Test
    void testWorkspaceLatestProjectWorkflowsOnlySpanVisibleProjects() {
        projectFacade.getWorkspaceLatestProjectWorkflows(WORKSPACE_ID);

        verify(projectWorkflowService).getProjectWorkflows(List.of(1L));
    }

    /**
     * Duplicating a PRIVATE project produces a WORKSPACE one — spec §13 claimed this was pinned and it was not. It
     * matters because duplication is a disclosure route: a grantee who can see a withheld project could otherwise copy
     * its workflows into a copy that keeps the source's reach, and the copy would then be a private project owned by
     * the grantee rather than by the person who withheld it (spec §16, decision 2).
     *
     * <p>
     * The branch that produces WORKSPACE here is {@code duplicateProject} building a fresh {@code new Project()} and
     * never reading the source's reach — it does not go through {@code applyCreateVisibility}, so the value comes from
     * the entity-field default (pinned against the policy default by {@code ProjectVisibilityPolicyTest}). The
     * assertion on the SOURCE is what keeps this from being vacuous: without it a setup that quietly seeded a WORKSPACE
     * source would produce the same green.
     */
    @Test
    void testDuplicateOfAPrivateProjectIsWorkspaceVisible() {
        Project sourceProject = project(1L);

        sourceProject.setVisibility(ResourceVisibility.PRIVATE);

        when(projectService.getProject(1L)).thenReturn(sourceProject);
        when(projectWorkflowService.getProjectWorkflowIds(1L, sourceProject.getLastProjectVersion()))
            .thenReturn(List.of());

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        when(projectService.create(captor.capture())).thenAnswer(invocation -> withId(invocation.getArgument(0), 2L));

        projectFacade.duplicateProject(1L);

        assertThat(sourceProject.getVisibility())
            .as("the source must actually be withheld, or the assertion below proves nothing")
            .isEqualTo(ResourceVisibility.PRIVATE);

        Project duplicatedProject = captor.getValue();

        assertThat(duplicatedProject.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testCeForcesWorkspaceVisibilityOnCreate() {
        ProjectFacadeImpl facade = facade("CE");
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        when(projectService.create(captor.capture())).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        facade.createProject(
            ProjectDTO.builder()
                .name("p")
                .workspaceId(WORKSPACE_ID)
                .visibility(ResourceVisibility.PRIVATE)
                .build());

        Project createdProject = captor.getValue();

        assertThat(createdProject.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testEeHonoursRequestedVisibilityOnCreate() {
        ProjectFacadeImpl facade = facade("EE");
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        when(projectService.create(captor.capture())).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        facade.createProject(
            ProjectDTO.builder()
                .name("p")
                .workspaceId(WORKSPACE_ID)
                .visibility(ResourceVisibility.PRIVATE)
                .build());

        Project createdProject = captor.getValue();

        assertThat(createdProject.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
    }

    @Test
    void testEeRejectsUnsupportedVisibilityOnCreate() {
        ProjectFacadeImpl facade = facade("EE");

        assertThatThrownBy(
            () -> facade.createProject(
                ProjectDTO.builder()
                    .name("p")
                    .workspaceId(WORKSPACE_ID)
                    .visibility(ResourceVisibility.ORGANIZATION)
                    .build()))
                        .isInstanceOf(ConfigurationException.class);
    }

    /**
     * A REST create that omits the field maps to a null {@code ProjectDTO.visibility()}, so EE falls back to the policy
     * default. The spy is over the REAL registry: {@code ProjectDTO.toProject()} independently coerces null to
     * WORKSPACE, so asserting the persisted value alone could not tell the fallback apart from that coercion — the
     * verify is what pins the registry lookup itself, and with it the resource-type token the lookup uses.
     */
    @Test
    void testEeDefaultsUnsetVisibilityFromThePolicyOnCreate() {
        ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry =
            spy(new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy())));
        ProjectFacadeImpl facade = facade("EE", resourceVisibilityPolicyRegistry);
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        when(projectService.create(captor.capture())).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        facade.createProject(
            ProjectDTO.builder()
                .name("p")
                .workspaceId(WORKSPACE_ID)
                .visibility(null)
                .build());

        Project createdProject = captor.getValue();

        assertThat(createdProject.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);

        verify(resourceVisibilityPolicyRegistry).defaultVisibility(ProjectVisibilityFilter.PROJECT);
    }

    /**
     * Builds the facade over the REAL {@link ResourceVisibilityPolicyRegistry} and {@link ProjectVisibilityPolicy}, so
     * the create-path assertions exercise the same rung table production wires rather than a stub that could agree with
     * the assertion by accident.
     */
    private ProjectFacadeImpl facade(String edition) {
        return facade(edition, new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy())));
    }

    private ProjectFacadeImpl facade(
        String edition, ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry) {

        // Only project 1 is visible; the resolver is the real seam every list surface shares.
        ResourceVisibilityResolver resourceVisibilityResolver =
            (resourceType, workspaceId, candidates) -> Set.of(1L);

        return new ProjectFacadeImpl(
            edition, mock(ApplicationProperties.class), mock(CategoryService.class),
            mock(ComponentDefinitionHelper.class), mock(ErrorWorkflowConfigurationValidator.class),
            mock(PermissionService.class), mock(PreBuiltTemplateService.class),
            codeWorkflowInfoSupplierProvider(), projectWorkflowService,
            mock(ProjectDeploymentService.class), projectService,
            new ProjectVisibilityFilter(objectProvider(resourceVisibilityResolver)),
            resourceVisibilityPolicyRegistry,
            mock(ProjectDeploymentFacade.class), mock(ProjectWorkflowFacade.class),
            mock(SharedTemplateFileStorage.class), mock(SharedTemplateService.class), tagService,
            mock(WorkflowService.class), mock(WorkflowTestConfigurationService.class),
            mock(WorkflowNodeTestOutputService.class), List.of());
    }

    private static Project withId(Project project, long id) {
        project.setId(id);

        return project;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ProjectCodeWorkflowInfoSupplier> codeWorkflowInfoSupplierProvider() {
        ObjectProvider<ProjectCodeWorkflowInfoSupplier> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(null);

        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResourceVisibilityResolver> objectProvider(
        ResourceVisibilityResolver resourceVisibilityResolver) {

        ObjectProvider<ResourceVisibilityResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resourceVisibilityResolver);

        return objectProvider;
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);
        project.setName("project-" + id);
        project.setWorkspaceId(WORKSPACE_ID);
        project.setVisibility(ResourceVisibility.WORKSPACE);

        return project;
    }
}
