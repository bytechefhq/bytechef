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

package com.bytechef.automation.ai.a2a.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.config.A2aIntTestWorkflows;
import com.bytechef.automation.ai.a2a.config.A2aMethodSecurityIntTestConfiguration;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = A2aMethodSecurityIntTestConfiguration.class)
class A2aProjectFacadeIntTest {

    @Autowired
    private A2aProjectFacade a2aProjectFacade;

    @Autowired
    private A2aProjectService a2aProjectService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAdminCanDeleteAnA2aProject() {
        resetCollaborators();
        authenticate("ROLE_ADMIN");

        A2aProject a2aProject = new A2aProject(100L, 1L, 42L);

        a2aProject.setId(2L);

        when(a2aProjectService.fetchA2aProject(2L)).thenReturn(Optional.of(a2aProject));

        a2aProjectFacade.deleteA2aProject(2L);

        verify(projectDeploymentFacade).deleteProjectDeployment(100L);
    }

    @Test
    void testNonAdminCannotCreateUpdateOrDeleteAnA2aProject() {
        resetCollaborators();
        authenticate("ROLE_USER");

        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> a2aProjectFacade.createA2aProject(1L, 2L, 1, List.of()));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> a2aProjectFacade.updateA2aProject(2L, List.of()));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> a2aProjectFacade.deleteA2aProject(2L));

        verifyNoInteractions(
            a2aProjectService, projectDeploymentFacade, projectDeploymentService, projectDeploymentWorkflowService,
            projectService,
            projectWorkflowService);
    }

    private void resetCollaborators() {
        reset(
            a2aProjectService, projectDeploymentFacade, projectDeploymentService, projectDeploymentWorkflowService,
            projectService,
            projectWorkflowService);
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(classes = A2aIntTestConfiguration.class)
    @Import(PostgreSQLContainerConfiguration.class)
    @A2aIntTestConfigurationSharedMocks
    class SystemDeployment {

        @Autowired
        private A2aProjectRepository a2aProjectRepository;

        @Autowired
        private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

        @Autowired
        private A2aProjectService systemA2aProjectService;

        @Autowired
        private A2aServerRepository a2aServerRepository;

        @Autowired
        private ComponentConnectionFacade componentConnectionFacade;

        @Autowired
        private ConnectionService connectionService;

        @Autowired
        private JobFacade jobFacade;

        @Autowired
        private PrincipalJobService principalJobService;

        @Autowired
        private ProjectDeploymentRepository projectDeploymentRepository;

        @MockitoSpyBean
        private ProjectDeploymentFacade systemProjectDeploymentFacade;

        @MockitoSpyBean
        private ProjectDeploymentWorkflowService systemProjectDeploymentWorkflowService;

        @Autowired
        private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private ProjectWorkflowRepository projectWorkflowRepository;

        @Autowired
        private TriggerExecutionService triggerExecutionService;

        @Autowired
        private TriggerLifecycleFacade triggerLifecycleFacade;

        @Autowired
        private WorkflowService workflowService;

        @Autowired
        private WorkflowTestConfigurationService workflowTestConfigurationService;

        @Autowired
        private WorkspaceRepository workspaceRepository;

        private final List<String> additionalWorkflowIds = new ArrayList<>();
        private A2aServer a2aServer;
        private Project project;
        private String workflowId;
        private UUID workflowUuid;

        @BeforeEach
        void beforeEach() {
            a2aServer = a2aServerRepository.save(new A2aServer("test-agent", null, Environment.DEVELOPMENT));

            Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

            Project newProject = Project.builder()
                .description("test-project")
                .name("test-project")
                .workspaceId(workspace.getId())
                .build();

            newProject.publish("v1");

            project = projectRepository.save(newProject);

            workflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);
            workflowUuid = UUID.randomUUID();

            projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId, workflowUuid));
        }

        @AfterEach
        void afterEach() {
            a2aProjectWorkflowRepository.deleteAll();
            a2aProjectRepository.deleteAll();
            projectDeploymentWorkflowRepository.deleteAll();
            projectDeploymentRepository.deleteAll();
            projectWorkflowRepository.deleteAll();
            projectRepository.deleteAll();
            workspaceRepository.deleteAll();
            a2aServerRepository.deleteAll();

            A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(workflowId));
            A2aIntTestWorkflows.deleteWorkflows(workflowService, additionalWorkflowIds);

            additionalWorkflowIds.clear();
        }

        @Test
        void testCreateA2aProjectEnablesItsSystemDeployment() {
            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThat(projectDeploymentRepository.findById(a2aProject.getProjectDeploymentId()))
                .hasValueSatisfying(projectDeployment -> assertThat(projectDeployment.isEnabled()).isTrue());
        }

        @Test
        void testCreateA2aProjectHidesItsSystemDeploymentFromTheProjectDeploymentList() {
            a2aProjectFacade.createA2aProject(a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThat(projectDeploymentRepository.findAll()).hasSize(1);
            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(null, null, project.getId(), null, null))
                    .isEmpty();
        }

        @Test
        void testCreateA2aProjectRejectsAProjectVersionTheServerAlreadyHas() {
            a2aProjectFacade.createA2aProject(a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                    a2aServer.getId(), project.getId(), 1, List.of(workflowId)))
                .withMessageContaining("already attached");

            assertThat(projectDeploymentRepository.findAll()).hasSize(1);
            assertThat(a2aProjectRepository.findAllByA2aServerId(a2aServer.getId())).hasSize(1);
        }

        @Test
        void testCreateA2aProjectRejectsAnotherVersionOfAProjectTheServerAlreadyHas() {
            String secondVersionWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

            try {
                projectWorkflowRepository.save(
                    new ProjectWorkflow(project.getId(), 2, secondVersionWorkflowId, UUID.randomUUID()));

                project.publish("v2");

                project = projectRepository.save(project);

                a2aProjectFacade.createA2aProject(a2aServer.getId(), project.getId(), 1, List.of(workflowId));

                assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                        a2aServer.getId(), project.getId(), 2, List.of(secondVersionWorkflowId)))
                    .withMessageContaining("already attached");

                assertThat(projectDeploymentRepository.findAll()).hasSize(1);
                assertThat(a2aProjectRepository.findAllByA2aServerId(a2aServer.getId())).hasSize(1);
            } finally {
                A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(secondVersionWorkflowId));
            }
        }

        @Test
        void testCreateA2aProjectRejectsASecondProjectOnTheSameServerEvenWithoutTheReadCheck() {
            ProjectDeployment firstProjectDeployment = projectDeploymentRepository.save(
                createProjectDeployment("first", 1));
            ProjectDeployment secondProjectDeployment = projectDeploymentRepository.save(
                createProjectDeployment("second", 2));

            systemA2aProjectService.create(firstProjectDeployment.getId(), a2aServer.getId(), project.getId());

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> systemA2aProjectService.create(
                    secondProjectDeployment.getId(), a2aServer.getId(), project.getId()))
                .withMessageContaining("already attached");

            assertThat(a2aProjectRepository.findAllByA2aServerId(a2aServer.getId())).hasSize(1);
        }

        @Test
        void testCreateA2aProjectRejectsAnUnpublishedProject() {
            Workspace workspace = workspaceRepository.save(new Workspace("unpublished-workspace"));

            Project unpublishedProject = projectRepository.save(
                Project.builder()
                    .description("unpublished-project")
                    .name("unpublished-project")
                    .workspaceId(workspace.getId())
                    .build());

            String unpublishedWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

            try {
                projectWorkflowRepository.save(
                    new ProjectWorkflow(unpublishedProject.getId(), 1, unpublishedWorkflowId, UUID.randomUUID()));

                assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                        a2aServer.getId(), unpublishedProject.getId(), 1, List.of(unpublishedWorkflowId)))
                    .withMessageContaining("is not published");

                assertThat(projectDeploymentRepository.findAll()).isEmpty();
                assertThat(a2aProjectRepository.findAllByA2aServerId(a2aServer.getId())).isEmpty();
            } finally {
                A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(unpublishedWorkflowId));
            }
        }

        @Test
        void testCreateA2aProjectRejectsTheDraftProjectVersion() {
            String draftWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

            try {
                projectWorkflowRepository.save(
                    new ProjectWorkflow(project.getId(), 2, draftWorkflowId, UUID.randomUUID()));

                assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                        a2aServer.getId(), project.getId(), 2, List.of(draftWorkflowId)))
                    .withMessageContaining("Version 2 of project " + project.getId() + " is not published");

                assertThat(projectDeploymentRepository.findAll()).isEmpty();
                assertThat(a2aProjectRepository.findAllByA2aServerId(a2aServer.getId())).isEmpty();
            } finally {
                A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(draftWorkflowId));
            }
        }

        @Test
        void testCreateA2aProjectAcceptsAPublishedProjectVersion() {
            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThat(a2aProjectRepository.findAllByA2aServerId(a2aServer.getId()))
                .extracting(A2aProject::getId)
                .containsExactly(a2aProject.getId());
            assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(
                a2aProject.getProjectDeploymentId()))
                    .extracting(ProjectDeploymentWorkflow::getWorkflowId)
                    .containsExactly(workflowId);
        }

        @Test
        void testCreateA2aProjectPersistsTheTestConfigurationConnectionOnTheSystemDeploymentWorkflow() {
            String draftWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

            try {
                projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 2, draftWorkflowId, workflowUuid));

                stubSlackConnection(true);
                stubWorkflowTestConfiguration(draftWorkflowId, Environment.DEVELOPMENT, 77L);
                stubConnection(77L, "slack", Environment.DEVELOPMENT);

                A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                    a2aServer.getId(), project.getId(), 1, List.of(workflowId));

                assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(
                    a2aProject.getProjectDeploymentId()))
                        .singleElement()
                        .satisfies(projectDeploymentWorkflow -> assertThat(projectDeploymentWorkflow.getConnections())
                            .singleElement()
                            .satisfies(connection -> {
                                assertThat(connection.getConnectionId()).isEqualTo(77L);
                                assertThat(connection.getWorkflowConnectionKey()).isEqualTo("slack");
                                assertThat(connection.getWorkflowNodeName()).isEqualTo("newWorkflowCall_1");
                            }));
            } finally {
                A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(draftWorkflowId));
            }
        }

        @Test
        void testCreateA2aProjectRejectsAWorkflowWithoutItsRequiredConnection() {
            stubSlackConnection(true);

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                    a2aServer.getId(), project.getId(), 1, List.of(workflowId)))
                .withMessageContaining("requires a slack connection");

            assertThat(projectDeploymentRepository.findAll()).isEmpty();
            assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
            assertThat(a2aProjectRepository.findAllByA2aServerId(a2aServer.getId())).isEmpty();
        }

        @Test
        void testUpdateA2aProjectPersistsTheTestConfigurationConnectionOfAnAddedWorkflow() {
            String addedWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

            try {
                projectWorkflowRepository.save(
                    new ProjectWorkflow(project.getId(), 1, addedWorkflowId, UUID.randomUUID()));

                A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                    a2aServer.getId(), project.getId(), 1, List.of(workflowId));

                stubSlackConnection(true);
                stubWorkflowTestConfiguration(addedWorkflowId, Environment.DEVELOPMENT, 78L);
                stubConnection(78L, "slack", Environment.DEVELOPMENT);

                a2aProjectFacade.updateA2aProject(a2aProject.getId(), List.of(workflowId, addedWorkflowId));

                assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(
                    a2aProject.getProjectDeploymentId()))
                        .filteredOn(projectDeploymentWorkflow -> addedWorkflowId.equals(
                            projectDeploymentWorkflow.getWorkflowId()))
                        .singleElement()
                        .satisfies(projectDeploymentWorkflow -> assertThat(projectDeploymentWorkflow.getConnections())
                            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                            .containsExactly(78L));
            } finally {
                A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(addedWorkflowId));
            }
        }

        @Test
        void testDeleteA2aProjectDeletesTheJobsAndDisablesTheTriggersOfItsSystemDeployment() {
            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            long projectDeploymentId = a2aProject.getProjectDeploymentId();

            when(principalJobService.getJobIds(projectDeploymentId, PlatformType.AUTOMATION))
                .thenReturn(List.of(42L, 43L));

            a2aProjectFacade.deleteA2aProject(a2aProject.getId());

            verify(triggerLifecycleFacade).executeTriggerDisable(eq(workflowId), any(), any(), any(), any());
            verify(triggerExecutionService).deleteJobTriggerExecution(42L);
            verify(triggerExecutionService).deleteJobTriggerExecution(43L);
            verify(principalJobService).deletePrincipalJobs(42L, PlatformType.AUTOMATION);
            verify(principalJobService).deletePrincipalJobs(43L, PlatformType.AUTOMATION);
            verify(jobFacade).deleteJob(42L);
            verify(jobFacade).deleteJob(43L);

            assertThat(a2aProjectRepository.findAllByA2aServerId(a2aServer.getId())).isEmpty();
            assertThat(a2aProjectWorkflowRepository.findAll()).isEmpty();
            assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
            assertThat(projectDeploymentRepository.findById(projectDeploymentId)).isEmpty();
        }

        @Test
        void testCreateA2aProjectAllowsTheSameProjectVersionOnAnotherServer() {
            A2aServer otherA2aServer = a2aServerRepository.save(
                new A2aServer("other-agent", null, Environment.DEVELOPMENT));

            a2aProjectFacade.createA2aProject(a2aServer.getId(), project.getId(), 1, List.of(workflowId));
            a2aProjectFacade.createA2aProject(otherA2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThat(a2aProjectRepository.findAllByA2aServerId(otherA2aServer.getId())).hasSize(1);
        }

        @Test
        void testCreateA2aProjectRejectsAProjectVersionThatDoesNotExist() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.createA2aProject(a2aServer.getId(), project.getId(), 9, List.of()))
                .withMessageContaining("Version 9 of project " + project.getId() + " is not published");

            assertThat(projectDeploymentRepository.findAll()).isEmpty();
        }

        @Test
        void testCreateA2aProjectUsesTheServerEnvironment() {
            A2aServer productionA2aServer = a2aServerRepository.save(
                new A2aServer("prod-agent", null, Environment.PRODUCTION));

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                productionA2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThat(projectDeploymentRepository.findById(a2aProject.getProjectDeploymentId()))
                .hasValueSatisfying(projectDeployment -> {
                    assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
                    assertThat(projectDeployment.getName()).startsWith(
                        SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX);
                });
        }

        @Test
        void testCreateA2aProjectExposesEachDistinctWorkflowOnce() {
            String secondWorkflowId = createProjectWorkflow(workflowDefinition("workflow/v1/newWorkflowCall"));

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId, secondWorkflowId, workflowId));

            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
                projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(a2aProject.getProjectDeploymentId());

            assertThat(projectDeploymentWorkflows)
                .extracting(ProjectDeploymentWorkflow::getWorkflowId)
                .containsExactlyInAnyOrder(workflowId, secondWorkflowId);
            assertThat(a2aProjectWorkflowRepository.findAllByA2aProjectId(a2aProject.getId()))
                .extracting(A2aProjectWorkflow::getProjectDeploymentWorkflowId)
                .containsExactlyInAnyOrderElementsOf(
                    projectDeploymentWorkflows.stream()
                        .map(ProjectDeploymentWorkflow::getId)
                        .toList());
        }

        @Test
        void testCreateA2aProjectRejectsAWorkflowOutsideTheProjectVersion() {
            String foreignWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

            additionalWorkflowIds.add(foreignWorkflowId);

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                    a2aServer.getId(), project.getId(), 1, List.of(workflowId, foreignWorkflowId)))
                .withMessageContaining(foreignWorkflowId);

            assertThat(projectDeploymentRepository.findAll()).isEmpty();
        }

        @Test
        void testCreateA2aProjectRejectsAWorkflowWithoutANewWorkflowCallTrigger() {
            String webhookWorkflowId = createProjectWorkflow(
                workflowDefinition("webhook/v1/autoRespondingWithHTTP200"));

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                    a2aServer.getId(), project.getId(), 1, List.of(workflowId, webhookWorkflowId)))
                .withMessageContaining(webhookWorkflowId)
                .withMessageContaining("New Workflow Call");

            assertThat(projectDeploymentRepository.findAll()).isEmpty();
        }

        @Test
        void testUpdateA2aProjectRejectsAWorkflowWithoutANewWorkflowCallTrigger() {
            String webhookWorkflowId = createProjectWorkflow(
                workflowDefinition("webhook/v1/autoRespondingWithHTTP200"));

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.updateA2aProject(
                    a2aProject.getId(), List.of(workflowId, webhookWorkflowId)))
                .withMessageContaining(webhookWorkflowId);

            assertSkillWorkflowIds(a2aProject, workflowId);
        }

        @Test
        void testUpdateA2aProjectAddsNewRemovesDeselectedAndKeepsUnchangedWorkflows() {
            String removedWorkflowId = createProjectWorkflow(workflowDefinition("workflow/v1/newWorkflowCall"));
            String addedWorkflowId = createProjectWorkflow(workflowDefinition("workflow/v1/newWorkflowCall"));

            stubNoRunningJobs();

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId, removedWorkflowId));

            ProjectDeploymentWorkflow keptProjectDeploymentWorkflow = getProjectDeploymentWorkflow(
                a2aProject, workflowId);
            ProjectDeploymentWorkflow removedProjectDeploymentWorkflow = getProjectDeploymentWorkflow(
                a2aProject, removedWorkflowId);
            List<A2aProjectWorkflow> keptA2aProjectWorkflows =
                a2aProjectWorkflowRepository.findAllByProjectDeploymentWorkflowId(
                    keptProjectDeploymentWorkflow.getId());

            a2aProjectFacade.updateA2aProject(a2aProject.getId(), List.of(workflowId, addedWorkflowId));

            assertSkillWorkflowIds(a2aProject, workflowId, addedWorkflowId);
            assertThat(projectDeploymentWorkflowRepository.findById(keptProjectDeploymentWorkflow.getId()))
                .isPresent();
            assertThat(projectDeploymentWorkflowRepository.findById(removedProjectDeploymentWorkflow.getId()))
                .isEmpty();
            assertThat(a2aProjectWorkflowRepository.findAllByProjectDeploymentWorkflowId(
                keptProjectDeploymentWorkflow.getId()))
                    .extracting(A2aProjectWorkflow::getId)
                    .containsExactlyElementsOf(
                        keptA2aProjectWorkflows.stream()
                            .map(A2aProjectWorkflow::getId)
                            .toList());
        }

        @Test
        void testUpdateA2aProjectDisablesARemovedWorkflowBeforeDeletingIt() {
            String removedWorkflowId = createProjectWorkflow(workflowDefinition("workflow/v1/newWorkflowCall"));

            stubNoRunningJobs();

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId, removedWorkflowId));

            long projectDeploymentId = a2aProject.getProjectDeploymentId();
            ProjectDeploymentWorkflow removedProjectDeploymentWorkflow = getProjectDeploymentWorkflow(
                a2aProject, removedWorkflowId);

            a2aProjectFacade.updateA2aProject(a2aProject.getId(), List.of(workflowId));

            InOrder inOrder = inOrder(systemProjectDeploymentFacade, systemProjectDeploymentWorkflowService);

            inOrder.verify(systemProjectDeploymentFacade)
                .enableProjectDeploymentWorkflow(projectDeploymentId, removedWorkflowId, false);
            inOrder.verify(systemProjectDeploymentWorkflowService)
                .delete(removedProjectDeploymentWorkflow.getId());

            verify(systemProjectDeploymentFacade, never()).enableProjectDeploymentWorkflow(
                projectDeploymentId, workflowId, false);
        }

        @Test
        void testUpdateA2aProjectRejectsAWorkflowOutsideTheProjectVersion() {
            String foreignWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

            additionalWorkflowIds.add(foreignWorkflowId);

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.updateA2aProject(
                    a2aProject.getId(), List.of(workflowId, foreignWorkflowId)))
                .withMessageContaining(foreignWorkflowId);

            assertSkillWorkflowIds(a2aProject, workflowId);
        }

        @Test
        void testDeleteA2aProjectDeletesItsSystemDeploymentThroughTheDeploymentFacade() {
            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            a2aProjectFacade.deleteA2aProject(a2aProject.getId());

            verify(systemProjectDeploymentFacade).deleteProjectDeployment(a2aProject.getProjectDeploymentId());

            assertThat(projectDeploymentRepository.findById(a2aProject.getProjectDeploymentId())).isEmpty();
        }

        @Test
        void testDeleteA2aProjectRejectsAnUnknownProject() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.deleteA2aProject(999_999L))
                .withMessageContaining("999999");

            verify(systemProjectDeploymentFacade, never()).deleteProjectDeployment(anyLong());
        }

        @Test
        void testCreateA2aProjectRejectsARequiredConnectionFromAnotherEnvironment() {
            stubSlackConnection(true);
            stubWorkflowTestConfiguration(workflowId, Environment.DEVELOPMENT, 77L);
            stubConnection(77L, "slack", Environment.PRODUCTION);

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                    a2aServer.getId(), project.getId(), 1, List.of(workflowId)))
                .withMessageContaining(workflowId);

            assertThat(projectDeploymentRepository.findAll()).isEmpty();
        }

        @Test
        void testCreateA2aProjectRejectsARequiredConnectionOfAnotherComponent() {
            stubSlackConnection(true);
            stubWorkflowTestConfiguration(workflowId, Environment.DEVELOPMENT, 77L);
            stubConnection(77L, "github", Environment.DEVELOPMENT);

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.createA2aProject(
                    a2aServer.getId(), project.getId(), 1, List.of(workflowId)))
                .withMessageContaining(workflowId);

            assertThat(projectDeploymentRepository.findAll()).isEmpty();
        }

        @Test
        void testCreateA2aProjectAcceptsAWorkflowWithoutItsOptionalConnection() {
            stubSlackConnection(false);

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            assertThat(getProjectDeploymentWorkflow(a2aProject, workflowId).getConnections()).isEmpty();
        }

        @Test
        void testUpdateA2aProjectRejectsAnAddedWorkflowWithoutItsRequiredConnection() {
            String addedWorkflowId = createProjectWorkflow(workflowDefinition("workflow/v1/newWorkflowCall"));

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                a2aServer.getId(), project.getId(), 1, List.of(workflowId));

            stubSlackConnection(true);

            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> a2aProjectFacade.updateA2aProject(
                    a2aProject.getId(), List.of(workflowId, addedWorkflowId)))
                .withMessageContaining(addedWorkflowId);

            assertSkillWorkflowIds(a2aProject, workflowId);
        }

        @Test
        void testUpdateA2aProjectCopiesTheTestConfigurationConnectionOfTheDeploymentEnvironment() {
            String addedWorkflowId = createProjectWorkflow(workflowDefinition("workflow/v1/newWorkflowCall"));

            A2aServer productionA2aServer = a2aServerRepository.save(
                new A2aServer("prod-agent", null, Environment.PRODUCTION));

            A2aProject a2aProject = a2aProjectFacade.createA2aProject(
                productionA2aServer.getId(), project.getId(), 1, List.of(workflowId));

            ProjectDeployment projectDeployment = projectDeploymentRepository.findById(
                a2aProject.getProjectDeploymentId())
                .orElseThrow();

            projectDeployment.setEnvironment(Environment.STAGING);

            projectDeploymentRepository.save(projectDeployment);

            stubSlackConnection(true);
            stubWorkflowTestConfiguration(addedWorkflowId, Environment.STAGING, 78L);
            stubWorkflowTestConfiguration(addedWorkflowId, Environment.PRODUCTION, 79L);
            stubConnection(78L, "slack", Environment.STAGING);
            stubConnection(79L, "slack", Environment.PRODUCTION);

            a2aProjectFacade.updateA2aProject(a2aProject.getId(), List.of(workflowId, addedWorkflowId));

            assertThat(getProjectDeploymentWorkflow(a2aProject, addedWorkflowId).getConnections())
                .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                .containsExactly(78L);
        }

        private void assertSkillWorkflowIds(A2aProject a2aProject, String... expectedWorkflowIds) {
            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
                projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(a2aProject.getProjectDeploymentId());

            assertThat(projectDeploymentWorkflows)
                .extracting(ProjectDeploymentWorkflow::getWorkflowId)
                .containsExactlyInAnyOrder(expectedWorkflowIds);
            assertThat(a2aProjectWorkflowRepository.findAllByA2aProjectId(a2aProject.getId()))
                .extracting(A2aProjectWorkflow::getProjectDeploymentWorkflowId)
                .containsExactlyInAnyOrderElementsOf(
                    projectDeploymentWorkflows.stream()
                        .map(ProjectDeploymentWorkflow::getId)
                        .toList());
        }

        private String createProjectWorkflow(String definition) {
            Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

            additionalWorkflowIds.add(workflow.getId());

            projectWorkflowRepository
                .save(new ProjectWorkflow(project.getId(), 1, workflow.getId(), UUID.randomUUID()));

            return workflow.getId();
        }

        private ProjectDeploymentWorkflow getProjectDeploymentWorkflow(A2aProject a2aProject, String workflowId) {
            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
                projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(a2aProject.getProjectDeploymentId());

            return projectDeploymentWorkflows.stream()
                .filter(projectDeploymentWorkflow -> workflowId.equals(projectDeploymentWorkflow.getWorkflowId()))
                .findFirst()
                .orElseThrow();
        }

        private void stubConnection(long connectionId, String componentName, Environment environment) {
            Connection connection = new Connection();

            connection.setComponentName(componentName);
            connection.setEnvironmentId(environment.ordinal());
            connection.setId(connectionId);

            when(connectionService.getConnection(connectionId)).thenReturn(connection);
        }

        private void stubNoRunningJobs() {
            when(
                principalJobService.getJobIds(
                    any(Job.Status.class), any(), any(), anyList(), eq(PlatformType.AUTOMATION), anyList(),
                    anyBoolean(), anyInt()))
                        .thenReturn(Page.empty());
        }

        private void stubSlackConnection(boolean required) {
            when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class)))
                .thenReturn(List.of(new ComponentConnection("slack", 1, "newWorkflowCall_1", "slack", required)));
        }

        private void stubWorkflowTestConfiguration(
            String testConfigurationWorkflowId, Environment environment, long connectionId) {

            WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

            workflowTestConfiguration.setConnections(
                List.of(new WorkflowTestConfigurationConnection(connectionId, "slack", "newWorkflowCall_1")));

            when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(
                testConfigurationWorkflowId, environment.ordinal()))
                    .thenReturn(Optional.of(workflowTestConfiguration));
        }

        private static String workflowDefinition(String triggerType) {
            return """
                {
                    "label": "A2A skill",
                    "triggers": [{"name": "newWorkflowCall_1", "type": "%s"}],
                    "tasks": []
                }
                """.formatted(triggerType);
        }

        private ProjectDeployment createProjectDeployment(String name, int projectVersion) {
            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setEnvironment(Environment.DEVELOPMENT);
            projectDeployment.setName(name);
            projectDeployment.setProjectId(project.getId());
            projectDeployment.setProjectVersion(projectVersion);

            return projectDeployment;
        }
    }
}
