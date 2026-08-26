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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.service.TagService;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins that the two entity-returning reads the GraphQL project surface delegates to answer the same authorization
 * question as each other.
 *
 * <p>
 * Both used to be assembled inside {@code ProjectGraphQlController} — a {@code hasResourceScope} call plus a
 * hand-thrown {@code AccessDeniedException} for the by-id read, and the whole two-halves narrowing for the listing —
 * and {@code ProjectGraphQlControllerVisibilityTest} pinned them there. They now live on {@link ProjectFacadeImpl},
 * which is where this codebase keeps authorization, so the pins move with them.
 *
 * <p>
 * The {@link PermissionService} stub below implements {@code hasResourceScope} the way the EE implementation does for a
 * project — the visibility precondition, then the workspace scope held in the project's OWN workspace, with no
 * per-project environment resolver in between — and {@code hasWorkspaceScope} as membership plus scope. The listing and
 * the by-id gate are then driven through that same stub, so a disagreement between them is a disagreement in the facade
 * rather than in two differently-mocked oracles.
 *
 * <p>
 * The by-id half is an annotation rather than a call now, so this test drives it the way Spring Security would: through
 * {@link AutomationPermissionEvaluator}, with {@link #assertByIdGateIsTheProjectViewScope()} asserting the annotation
 * literally spells the expression whose evaluation is being simulated. Without that assertion the agreement case would
 * still pass with the guard deleted, which is the failure this whole change is about.
 *
 * <p>
 * The agreement is not total, and {@link #testProjectRowsAgreeWithTheByIdGate()} skips one case rather than asserting
 * it: a feature-owned system project is dropped by the listing and answered by the by-id read. That is a real asymmetry
 * rather than a gap in this test, and it is recorded on both halves of the
 * {@code ProjectFacade.getProjectRow}/{@code getProjectRows} javadoc pair.
 * {@link #testProjectRowsListOnlyProjectsInAWorkspaceTheCallerIsScopedIn()} is what holds the listing side of it in
 * place, since {@code systemProject} sits in the scoped workspace at {@code WORKSPACE} visibility and so is excluded by
 * nothing but the system-project filter.
 *
 * @author Ivica Cardic
 */
class ProjectFacadeRowVisibilityTest {

    private static final String PROJECT_RESOURCE_TYPE = "Project";
    private static final long SCOPED_WORKSPACE_ID = 1L;
    private static final long UNSCOPED_WORKSPACE_ID = 2L;
    private static final String WORKFLOW_VIEW_SCOPE = "WORKFLOW_VIEW";

    private final ProjectService projectService = mock(ProjectService.class);

    private final Project workspaceVisibleProject = project(
        1L, "Shared", SCOPED_WORKSPACE_ID, ResourceVisibility.WORKSPACE);
    private final Project privateProject = project(2L, "Withheld", SCOPED_WORKSPACE_ID, ResourceVisibility.PRIVATE);
    private final Project otherWorkspaceProject = project(
        3L, "Elsewhere", UNSCOPED_WORKSPACE_ID, ResourceVisibility.WORKSPACE);
    private final Project otherWorkspacePrivateProject = project(
        4L, "Elsewhere withheld", UNSCOPED_WORKSPACE_ID, ResourceVisibility.PRIVATE);
    private final Project systemProject = project(
        5L, SystemProjects.AI_AGENT_NAME_PREFIX + "agent", SCOPED_WORKSPACE_ID, ResourceVisibility.WORKSPACE);

    private final List<Project> allProjects = List.of(
        workspaceVisibleProject, privateProject, otherWorkspaceProject, otherWorkspacePrivateProject, systemProject);

    private PermissionService permissionService;
    private ProjectVisibilityFilter projectVisibilityFilter;

    /**
     * Both collaborators are built here rather than in a field initializer: SpotBugs reads a stub-building call in one
     * as a constructor that may throw.
     */
    @BeforeEach
    void setUp() {
        permissionService = createPermissionService();
        projectVisibilityFilter = spy(createProjectVisibilityFilter());
    }

    @Test
    void testProjectRowsListOnlyProjectsInAWorkspaceTheCallerIsScopedIn() {
        when(projectService.getProjects()).thenReturn(allProjects);

        assertThat(createProjectFacade().getProjectRows())
            .extracting(Project::getId)
            .containsExactly(1L);
    }

    @Test
    void testProjectRowsAgreeWithTheByIdGate() {
        when(projectService.getProjects()).thenReturn(allProjects);

        // Every fixture project has to be reachable by id, not just the listing. isByIdReadAnswered runs the real
        // getProjectRow body, and an unstubbed mock returns null -- which SystemProjects.isSystemProject reads as
        // "not a system project", so the system-project filter would never fire and this loop would pass while
        // proving nothing about it.
        for (Project project : allProjects) {
            when(projectService.getProject(Objects.requireNonNull(project.getId(), "id"))).thenReturn(project);
        }

        assertByIdGateIsTheProjectViewScope();

        Set<Long> listedProjectIds = createProjectFacade().getProjectRows()
            .stream()
            .map(Project::getId)
            .collect(Collectors.toSet());

        for (Project project : allProjects) {
            long projectId = Objects.requireNonNull(project.getId(), "id");

            // No exception any more: system projects used to be skipped here, because the listing dropped them and
            // the by-id read answered for them. getProjectRow now filters them too, so every project in the fixture
            // -- system projects included -- must answer alike on both sides.
            assertThat(isByIdReadAnswered(projectId))
                .describedAs("project id=%s: the listing and the by-id read must answer alike", projectId)
                .isEqualTo(listedProjectIds.contains(projectId));
        }

        assertThat(listedProjectIds)
            .describedAs("a run where nothing is listed would satisfy the loop above without proving anything")
            .isNotEmpty();
    }

    @Test
    void testProjectRowsSkipAProjectWithNoWorkspace() {
        when(projectService.getProjects())
            .thenReturn(List.of(project(6L, "Orphan", null, ResourceVisibility.WORKSPACE)));

        assertThat(createProjectFacade().getProjectRows()).isEmpty();
    }

    /**
     * The listing exists to avoid a round trip per project, so the visibility filter must see the whole collection
     * once. A per-project call would answer the same and cost an authorization lookup per row.
     */
    @Test
    void testProjectRowsResolveVisibilityInOneBatchedCall() {
        when(projectService.getProjects()).thenReturn(allProjects);

        createProjectFacade().getProjectRows();

        verify(projectVisibilityFilter, times(1)).visibleProjectIds(anyCollection());
    }

    @Test
    void testProjectRowIsReadThroughTheProjectService() {
        when(projectService.getProject(1L)).thenReturn(workspaceVisibleProject);

        assertThat(createProjectFacade().getProjectRow(1L)).isSameAs(workspaceVisibleProject);
    }

    /**
     * Answers the by-id read the way Spring Security would evaluate the annotation on
     * {@link ProjectFacadeImpl#getProjectRow(long)}: through the same {@link AutomationPermissionEvaluator} the
     * {@code hasPermission} built-in routes to, over the same {@link PermissionService} stub the listing is driven
     * through.
     */
    private boolean isByIdReadPermitted(long projectId) {
        AutomationPermissionEvaluator automationPermissionEvaluator =
            new AutomationPermissionEvaluator(permissionService, mock(ObjectProvider.class));

        return automationPermissionEvaluator.hasPermission(
            null, projectId, PROJECT_RESOURCE_TYPE, WORKFLOW_VIEW_SCOPE);
    }

    /**
     * Whether a by-id read actually yields the project — the gate permitting it AND the method body not filtering it
     * away.
     *
     * <p>
     * The gate alone is not the contract. {@code getProjectRow} also drops feature-owned system projects in its body,
     * and a unit test calling the facade directly crosses no security proxy, so the two halves have to be composed by
     * hand: evaluating only the gate would report a system project as readable and put the disagreement this loop
     * exists to rule out back where it was.
     */
    private boolean isByIdReadAnswered(long projectId) {
        if (!isByIdReadPermitted(projectId)) {
            return false;
        }

        try {
            createProjectFacade().getProjectRow(projectId);

            return true;
        } catch (NoSuchElementException noSuchElementException) {
            return false;
        }
    }

    /**
     * Ties {@link #isByIdReadPermitted(long)} to the production guard. Simulating an evaluation proves nothing unless
     * the annotation actually asks for that evaluation, so this fails both when the guard is deleted and when it is
     * changed to a different resource type or scope.
     */
    private void assertByIdGateIsTheProjectViewScope() {
        Method method;

        try {
            method = ProjectFacadeImpl.class.getMethod("getProjectRow", long.class);
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new AssertionError("ProjectFacadeImpl.getProjectRow(long) no longer exists", noSuchMethodException);
        }

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .describedAs("ProjectFacadeImpl.getProjectRow(long) carries the by-id gate the GraphQL surface relies on")
            .isNotNull();
        assertThat(preAuthorize.value())
            .isEqualTo(
                "hasPermission(#%s, '%s', '%s')".formatted("id", PROJECT_RESOURCE_TYPE, WORKFLOW_VIEW_SCOPE));
    }

    /**
     * Answers {@code hasResourceScope} by composing the same two halves the EE implementation composes for a project:
     * the resource must be visible to the caller, and the caller must hold the scope in the project's own workspace.
     */
    private PermissionService createPermissionService() {
        PermissionService mockPermissionService = mock(PermissionService.class);

        when(mockPermissionService.hasWorkspaceScope(anyLong(), anyString()))
            .thenAnswer(invocation -> (long) invocation.getArgument(0) == SCOPED_WORKSPACE_ID);

        when(mockPermissionService.hasResourceScope(ArgumentMatchers.any(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                Serializable id = invocation.getArgument(0);

                Project project = allProjects.stream()
                    .filter(candidate -> Objects.equals(candidate.getId(), id))
                    .findFirst()
                    .orElse(null);

                if (project == null || project.getWorkspaceId() == null) {
                    return false;
                }

                return isVisible(project) && project.getWorkspaceId() == SCOPED_WORKSPACE_ID;
            });

        return mockPermissionService;
    }

    /**
     * The real filter over a resolver that hides every {@code PRIVATE} project — the caller owns none of them and holds
     * no grant.
     */
    private static ProjectVisibilityFilter createProjectVisibilityFilter() {
        ResourceVisibilityResolver resourceVisibilityResolver = (resourceType, workspaceId, candidates) -> {
            Set<Long> visibleIds = new LinkedHashSet<>();

            for (VisibilityRecord candidate : candidates) {
                ResourceVisibility visibility = candidate.visibility();

                if (visibility.isAtLeast(ResourceVisibility.WORKSPACE)) {
                    visibleIds.add(candidate.id());
                }
            }

            return visibleIds;
        };

        return new ProjectVisibilityFilter(objectProvider(resourceVisibilityResolver));
    }

    private ProjectFacadeImpl createProjectFacade() {
        return new ProjectFacadeImpl(
            "CE", mock(ApplicationProperties.class), mock(CategoryService.class),
            mock(ComponentDefinitionHelper.class), mock(ErrorWorkflowConfigurationValidator.class), permissionService,
            mock(PreBuiltTemplateService.class), codeWorkflowInfoSupplierProvider(),
            mock(ProjectWorkflowService.class), mock(ProjectDeploymentService.class), projectService,
            projectVisibilityFilter, mock(ResourceVisibilityPolicyRegistry.class), mock(ProjectDeploymentFacade.class),
            mock(ProjectWorkflowFacade.class), mock(SharedTemplateFileStorage.class),
            mock(SharedTemplateService.class), mock(TagService.class), mock(WorkflowService.class),
            mock(WorkflowTestConfigurationService.class), mock(WorkflowNodeTestOutputService.class), List.of());
    }

    private static boolean isVisible(Project project) {
        ResourceVisibility visibility = project.getVisibility();

        return visibility.isAtLeast(ResourceVisibility.WORKSPACE);
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

    private static Project project(Long id, String name, Long workspaceId, ResourceVisibility visibility) {
        Project project = new Project();

        project.setId(id);
        project.setName(name);
        project.setVisibility(visibility);
        project.setWorkspaceId(workspaceId);

        return project;
    }
}
