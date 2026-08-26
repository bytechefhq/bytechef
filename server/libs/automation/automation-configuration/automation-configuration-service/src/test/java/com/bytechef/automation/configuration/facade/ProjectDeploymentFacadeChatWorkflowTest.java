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
import static org.assertj.core.groups.Tuple.tuple;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade.ChatWorkflow;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the project-visibility half of {@code getWorkspaceChatWorkflows}. The membership half is the
 * {@code @PreAuthorize} expression, pinned next door in {@link ProjectDeploymentFacadeAuthorizationTest}; a direct call
 * cannot fire an annotation.
 *
 * <p>
 * The listing used to be assembled in {@code ProjectDeploymentWorkflowGraphQlController} out of services, past this
 * facade, and carried neither half — so it named the project and labelled the workflow of every hosted-chat deployment
 * in any workspace, {@code PRIVATE} projects included. The behavioural case below is written so that a "returns fewer
 * rows" green cannot be reached by accident: the withheld row is identical to the listed one in every respect the
 * listing filters on — enabled deployment, enabled deployment workflow, hosted chat trigger, resolvable static-webhook
 * execution id — and the control case flips ONLY the resolver's answer and asserts both rows come back. So the omitted
 * row is provably one the caller would otherwise have seen.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ProjectDeploymentFacadeChatWorkflowTest {

    private static final String CHAT_WORKFLOW_DEFINITION =
        "{\"label\":\"%s\",\"triggers\":[{\"name\":\"chat_1\",\"type\":\"chat/v1/newChatMessage\"}],\"tasks\":[]}";

    private static final long WORKSPACE_ID = 1L;

    private final ApplicationProperties applicationProperties = Mockito.mock(ApplicationProperties.class);
    private final EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);
    private final ProjectDeploymentService projectDeploymentService = Mockito.mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        Mockito.mock(ProjectDeploymentWorkflowService.class);
    private final ProjectService projectService = Mockito.mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = Mockito.mock(ProjectWorkflowService.class);
    private final TriggerDefinitionService triggerDefinitionService = Mockito.mock(TriggerDefinitionService.class);
    private final WorkflowService workflowService = Mockito.mock(WorkflowService.class);

    private final Project workspaceVisibleProject = project(1L, "Shared", ResourceVisibility.WORKSPACE);
    private final Project privateProject = project(2L, "Withheld", ResourceVisibility.PRIVATE);

    @Test
    void testWorkspaceChatWorkflowsHidesWorkflowsOfProjectsTheCallerCannotSee() {
        stubOneChatWorkflowPerProject();

        List<ChatWorkflow> chatWorkflows = createProjectDeploymentFacade(ResourceVisibility.WORKSPACE)
            .getWorkspaceChatWorkflows(WORKSPACE_ID, 0L);

        assertThat(chatWorkflows)
            .extracting(ChatWorkflow::projectName, ChatWorkflow::workflowLabel)
            .containsExactly(tuple("Shared", "Shared chat"));
    }

    /**
     * The control the assertion above leans on. Same stubs, same two rows, and the ONLY thing that changes is that the
     * resolver now admits {@code PRIVATE} too — so the row the first test finds missing is one this configuration
     * proves the listing would otherwise have produced, rather than one that some unrelated filter had already dropped.
     */
    @Test
    void testWorkspaceChatWorkflowsListsBothWhenTheResolverAdmitsBoth() {
        stubOneChatWorkflowPerProject();

        List<ChatWorkflow> chatWorkflows = createProjectDeploymentFacade(ResourceVisibility.PRIVATE)
            .getWorkspaceChatWorkflows(WORKSPACE_ID, 0L);

        assertThat(chatWorkflows)
            .extracting(ChatWorkflow::workflowLabel)
            .containsExactlyInAnyOrder("Shared chat", "Withheld chat");
    }

    /**
     * Two enabled deployments, one per project, each with one enabled deployment workflow whose workflow carries a
     * hosted chat trigger and resolves to a static-webhook execution id. The pair is deliberately symmetric: the only
     * difference between them is which project owns them.
     */
    private void stubOneChatWorkflowPerProject() {
        ProjectDeployment visibleProjectDeployment = projectDeployment(10L, 1L);
        ProjectDeployment hiddenProjectDeployment = projectDeployment(11L, 2L);

        Mockito.when(environmentService.getEnvironment(ArgumentMatchers.anyLong()))
            .thenReturn(Environment.PRODUCTION);
        Mockito
            .when(
                projectDeploymentService.getProjectDeployments(
                    ArgumentMatchers.eq(false), ArgumentMatchers.any(), ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(List.of(visibleProjectDeployment, hiddenProjectDeployment));
        Mockito.when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ArgumentMatchers.anyList()))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(100L, 10L, "wf-1"), projectDeploymentWorkflow(101L, 11L, "wf-2")));
        Mockito.when(workflowService.getWorkflows(ArgumentMatchers.anyList()))
            .thenReturn(List.of(chatWorkflow("wf-1", "Shared chat"), chatWorkflow("wf-2", "Withheld chat")));
        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflows(ArgumentMatchers.anyList()))
            .thenReturn(List.of(projectWorkflow(1000L, "wf-1"), projectWorkflow(1001L, "wf-2")));
        Mockito.when(projectService.getProjects(ArgumentMatchers.anyList()))
            .thenReturn(List.of(workspaceVisibleProject, privateProject));

        TriggerDefinition triggerDefinition = Mockito.mock(TriggerDefinition.class);

        Mockito.when(triggerDefinition.getType())
            .thenReturn(TriggerType.STATIC_WEBHOOK);
        Mockito.when(triggerDefinition.getName())
            .thenReturn("newChatMessage");
        Mockito
            .when(
                triggerDefinitionService.getTriggerDefinition(
                    ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyString()))
            .thenReturn(triggerDefinition);
    }

    /**
     * Only the collaborators this listing reaches are stubbed; the rest of the facade's graph is irrelevant to it and
     * is left as bare mocks.
     */
    private ProjectDeploymentFacadeImpl createProjectDeploymentFacade(ResourceVisibility lowestVisibleRung) {
        return new ProjectDeploymentFacadeImpl(
            null, null, null, environmentService, null, null, null, null, List.of(), projectDeploymentService,
            projectDeploymentWorkflowService, projectService, createProjectVisibilityFilter(lowestVisibleRung),
            projectWorkflowService, null, triggerDefinitionService, null, null, applicationProperties, null,
            workflowService);
    }

    /**
     * The real filter over a resolver that admits every project at or above {@code lowestVisibleRung} — the caller owns
     * none of them and holds no grant, so {@code WORKSPACE} is what an EE resolver would answer for a workspace member.
     */
    @SuppressWarnings("unchecked")
    private ProjectVisibilityFilter createProjectVisibilityFilter(ResourceVisibility lowestVisibleRung) {
        ResourceVisibilityResolver resourceVisibilityResolver = (resourceType, workspaceId, candidates) -> {
            Set<Long> visibleIds = new LinkedHashSet<>();

            for (VisibilityRecord candidate : candidates) {
                ResourceVisibility visibility = candidate.visibility();

                if (visibility.isAtLeast(lowestVisibleRung)) {
                    visibleIds.add(candidate.id());
                }
            }

            return visibleIds;
        };

        ObjectProvider<ResourceVisibilityResolver> objectProvider = Mockito.mock(ObjectProvider.class);

        Mockito.when(objectProvider.getIfAvailable())
            .thenReturn(resourceVisibilityResolver);

        return new ProjectVisibilityFilter(objectProvider);
    }

    private static Workflow chatWorkflow(String workflowId, String label) {
        return new Workflow(workflowId, CHAT_WORKFLOW_DEFINITION.formatted(label), Workflow.Format.JSON);
    }

    private static Project project(Long id, String name, ResourceVisibility visibility) {
        Project project = new Project();

        project.setId(id);
        project.setName(name);
        project.setVisibility(visibility);
        project.setWorkspaceId(WORKSPACE_ID);

        return project;
    }

    private static ProjectDeployment projectDeployment(Long id, long projectId) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setId(id);
        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(
        Long id, long projectDeploymentId, String workflowId) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static ProjectWorkflow projectWorkflow(long id, String workflowId) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(id);

        projectWorkflow.setUuid(UUID.randomUUID());
        projectWorkflow.setWorkflowId(workflowId);

        return projectWorkflow;
    }
}
