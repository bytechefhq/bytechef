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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings(
    value = {
        "CT_CONSTRUCTOR_THROW", "URF_UNREAD_FIELD"
    },
    justification = "Both are false positives of the @Spy field below. It IS read — by Mockito, reflectively, to "
        + "inject it into @InjectMocks — which SpotBugs cannot see; and its inline initializer is what puts the "
        + "field's construction inside this class's constructor. Nothing but the JUnit runner instantiates a test "
        + "class, so there is no finalizer-attack surface.")
class ProjectDeploymentFacadeTest {

    private static final long VISIBLE_PROJECT_ID = 1L;
    private static final long HIDDEN_PROJECT_ID = 2L;
    private static final long WORKSPACE_ID = 3L;
    private static final long SYSTEM_PROJECT_ID = 4L;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    /**
     * A real filter over a resolver that hides {@link #HIDDEN_PROJECT_ID} and nothing else, so the deployment listing
     * is exercised through the same seam production uses rather than a stubbed answer. Hiding only that one project
     * rather than revealing only {@link #VISIBLE_PROJECT_ID} is what keeps the system-project filter separable: a
     * system project this resolver also hid would be dropped for two reasons at once, and neither case below would pin
     * either. Declared as a spy because {@code @InjectMocks} only injects mock/spy fields.
     */
    @Spy
    private ProjectVisibilityFilter projectVisibilityFilter = new ProjectVisibilityFilter(
        objectProvider(
            (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(VisibilityRecord::id)
                .filter(id -> id != HIDDEN_PROJECT_ID)
                .collect(Collectors.toSet())));

    @Mock
    private TagService tagService;

    @InjectMocks
    private ProjectDeploymentFacadeImpl projectDeploymentFacade;

    @Test
    void testWorkspaceDeploymentsHideDeploymentsOfHiddenProjects() {
        when(projectDeploymentService.getProjectDeployments(false, null, null, null, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    projectDeployment(10L, VISIBLE_PROJECT_ID, Environment.DEVELOPMENT),
                    projectDeployment(11L, HIDDEN_PROJECT_ID, Environment.DEVELOPMENT)));
        when(projectService.getProjects(List.of(VISIBLE_PROJECT_ID, HIDDEN_PROJECT_ID)))
            .thenReturn(List.of(project(VISIBLE_PROJECT_ID), project(HIDDEN_PROJECT_ID)));

        List<ProjectDeploymentDTO> projectDeploymentDTOs =
            projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, null, null, null, false);

        assertThat(projectDeploymentDTOs).extracting(ProjectDeploymentDTO::projectId)
            .containsExactly(VISIBLE_PROJECT_ID);
    }

    @Test
    void testEveryDeploymentOfAHiddenProjectIsHidden() {
        // There is no per-deployment or per-environment opt-out: a PRODUCTION deployment of a private project is as
        // hidden as its DEVELOPMENT sibling. The project itself is returned by the project service and is not a
        // system project, so the only thing that can exclude these three rows is the visibility filter.
        when(projectDeploymentService.getProjectDeployments(false, null, null, null, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    projectDeployment(10L, HIDDEN_PROJECT_ID, Environment.DEVELOPMENT),
                    projectDeployment(11L, HIDDEN_PROJECT_ID, Environment.STAGING),
                    projectDeployment(12L, HIDDEN_PROJECT_ID, Environment.PRODUCTION)));
        when(projectService.getProjects(anyList())).thenReturn(List.of(project(HIDDEN_PROJECT_ID)));

        assertThat(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, null, null, null, false))
            .isEmpty();

        // The three deployments share one project, so the project lookup must ask for that id once. Deployments are
        // per-environment, so undeduplicated the id list grows with the number of environments a project is deployed
        // to rather than with the number of projects it has to load.
        ArgumentCaptor<List<Long>> projectIdsArgumentCaptor = ArgumentCaptor.captor();

        verify(projectService).getProjects(projectIdsArgumentCaptor.capture());

        assertThat(projectIdsArgumentCaptor.getValue()).containsExactly(HIDDEN_PROJECT_ID);
    }

    /**
     * The entity-returning overload, which the GraphQL {@code workspaceProjectDeployments} query delegates to. Its two
     * filters used to live in {@code ProjectDeploymentGraphQlController}, which read the rows off
     * {@code ProjectDeploymentService} directly and so had neither them nor a gate.
     *
     * <p>
     * The system-project half of this is a second line of defence, and the stub is what makes that visible: the row for
     * {@code SYSTEM_PROJECT_ID} is one the real query never returns, because
     * {@code CustomProjectDeploymentRepositoryImpl} appends
     * {@code SystemProjects.projectNameNotLikePredicates("project.name")} to every listing unconditionally, and
     * {@code ProjectDeploymentServiceSystemProjectIntTest} pins that against real rows. So this asserts that the facade
     * filter would still hold if the SQL one were removed or narrowed — not that a system-project deployment reaches
     * the facade today. The hidden-project half is the opposite: nothing below the facade applies visibility, so there
     * the filter under test is the only one there is.
     */
    @Test
    void testWorkspaceProjectDeploymentRowsHideSystemProjectsAndHiddenProjects() {
        when(environmentService.getEnvironment(0L)).thenReturn(Environment.DEVELOPMENT);
        when(
            projectDeploymentService.getProjectDeployments(
                false, Environment.DEVELOPMENT, null, null, WORKSPACE_ID))
                    .thenReturn(
                        List.of(
                            projectDeployment(10L, VISIBLE_PROJECT_ID, Environment.DEVELOPMENT),
                            projectDeployment(11L, HIDDEN_PROJECT_ID, Environment.DEVELOPMENT),
                            projectDeployment(12L, SYSTEM_PROJECT_ID, Environment.DEVELOPMENT)));
        when(projectService.getProjects(anyList()))
            .thenReturn(List.of(project(VISIBLE_PROJECT_ID), project(HIDDEN_PROJECT_ID), systemProject()));

        assertThat(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, 0L, null, null))
            .extracting(ProjectDeployment::getId)
            .containsExactly(10L);
    }

    @Test
    void testGetProjectDeploymentTags() {
        stubOneTaggedDeploymentPerProject();

        when(tagService.getTags(List.of(20L, 21L))).thenReturn(List.of(new Tag("x"), new Tag("y")));

        List<Tag> tags = projectDeploymentFacade.getProjectDeploymentTags(WORKSPACE_ID);

        assertThat(tags).hasSize(2);
    }

    /**
     * This listing feeds the tag dropdown over {@link ProjectDeploymentFacadeImpl#getWorkspaceProjectDeployments}, and
     * until it shared that method's {@code filterOutSystemProjectDeployments} the two disagreed: the dropdown offered
     * names aggregated off withheld and system projects, each of them selecting nothing in the list beside it.
     *
     * <p>
     * The three deployments carry DIFFERENT tag ids and the assertion is on the ids handed to {@code TagService}, so a
     * facade that stopped filtering asks for the other two and fails here — where asserting the returned tags would
     * only pin what the stub was told to return.
     */
    @Test
    void testProjectDeploymentTagsDropTheTagsOfHiddenAndSystemProjects() {
        stubOneTaggedDeploymentPerProject();

        projectDeploymentFacade.getProjectDeploymentTags(WORKSPACE_ID);

        verify(tagService).getTags(List.of(20L, 21L));
    }

    /**
     * One tagged deployment per project — visible, hidden, system — differing only in which project owns them and which
     * tag ids they carry, so each exclusion above is attributable to exactly one filter.
     */
    private void stubOneTaggedDeploymentPerProject() {
        when(projectDeploymentService.getProjectDeployments(null, null, null, null, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    taggedProjectDeployment(10L, VISIBLE_PROJECT_ID, List.of(20L, 21L)),
                    taggedProjectDeployment(11L, HIDDEN_PROJECT_ID, List.of(22L)),
                    taggedProjectDeployment(12L, SYSTEM_PROJECT_ID, List.of(23L))));
        when(projectService.getProjects(anyList()))
            .thenReturn(List.of(project(VISIBLE_PROJECT_ID), project(HIDDEN_PROJECT_ID), systemProject()));
    }

    private static ProjectDeployment taggedProjectDeployment(long id, long projectId, List<Long> tagIds) {
        ProjectDeployment projectDeployment = projectDeployment(id, projectId, Environment.DEVELOPMENT);

        projectDeployment.setTagIds(tagIds);

        return projectDeployment;
    }

    @Test
    void testValidateInputsAcceptsNonStringValuesForRequiredInputs() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(
            new Workflow.Input("hourToRun", "Hour", "integer", true),
            new Workflow.Input("minutesToRun", "Minute", "integer", true),
            new Workflow.Input("serviceProviderEmail", "Email", "string", true)));

        Map<String, Object> inputs = Map.of("hourToRun", 11, "minutesToRun", 50, "serviceProviderEmail", "a@b.c");

        assertThatCode(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(inputs, workflow))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateInputsRejectsMissingBlankAndNullRequiredValues() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(new Workflow.Input("name", "Name", "string", true)));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(Map.of(), workflow))
            .withMessageContaining("Missing required param: name");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(
                Map.of("name", "   "), workflow))
            .withMessageContaining("Missing required param: name");

        Map<String, Object> nullValueInputs = new HashMap<>();

        nullValueInputs.put("name", null);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(
                nullValueInputs, workflow))
            .withMessageContaining("Missing required param: name");
    }

    @Test
    void testValidateInputsIgnoresAbsentOptionalInputs() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(
            new Workflow.Input("destinationFolderName", "Folder", "string", false)));

        assertThatCode(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(Map.of(), workflow))
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResourceVisibilityResolver> objectProvider(
        ResourceVisibilityResolver resourceVisibilityResolver) {

        ObjectProvider<ResourceVisibilityResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resourceVisibilityResolver);

        return objectProvider;
    }

    private static Project systemProject() {
        Project systemProject = project(SYSTEM_PROJECT_ID);

        systemProject.setName(SystemProjects.AI_AGENT_NAME_PREFIX + "agent");

        return systemProject;
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);
        project.setName("project-" + id);
        project.setVisibility(ResourceVisibility.WORKSPACE);

        return project;
    }

    private static ProjectDeployment projectDeployment(long id, long projectId, Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }
}
