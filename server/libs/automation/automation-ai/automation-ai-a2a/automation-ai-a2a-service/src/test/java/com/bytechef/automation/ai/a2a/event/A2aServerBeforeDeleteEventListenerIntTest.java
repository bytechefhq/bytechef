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

package com.bytechef.automation.ai.a2a.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.config.A2aIntTestWorkflows;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = A2aIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@A2aIntTestConfigurationSharedMocks
class A2aServerBeforeDeleteEventListenerIntTest {

    @MockitoSpyBean
    private A2aProjectFacade a2aProjectFacade;

    @Autowired
    private A2aProjectRepository a2aProjectRepository;

    @Autowired
    private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

    @Autowired
    private A2aServerRepository a2aServerRepository;

    @Autowired
    private A2aServerService a2aServerService;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private A2aServer a2aServer;
    private Project project;
    private Project secondProject;
    private String workflowId1;
    private String workflowId2;
    private String workflowId3;

    @BeforeEach
    void beforeEach() {
        a2aServer = a2aServerService.create(new A2aServer("test-agent", null, Environment.DEVELOPMENT));

        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        project = createPublishedProject(workspace, "test-project");
        secondProject = createPublishedProject(workspace, "second-test-project");

        workflowId1 = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);
        workflowId2 = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);
        workflowId3 = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId1, UUID.randomUUID()));
        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId2, UUID.randomUUID()));
        projectWorkflowRepository.save(new ProjectWorkflow(secondProject.getId(), 1, workflowId3, UUID.randomUUID()));
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

        A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(workflowId1, workflowId2, workflowId3));
    }

    @Test
    void testDeleteServerDeletesItsProjectsAndSystemDeployments() {
        A2aProject a2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(workflowId1, workflowId2));

        long projectDeploymentId = a2aProject.getProjectDeploymentId();

        assertThat(a2aProjectWorkflowRepository.findAll()).hasSize(2);
        assertThat(projectDeploymentWorkflowRepository.findAll()).hasSize(2);

        a2aServerService.delete(a2aServer.getId());

        assertThat(a2aServerRepository.findById(a2aServer.getId())).isEmpty();
        assertThat(a2aProjectRepository.findById(a2aProject.getId())).isEmpty();
        assertThat(a2aProjectWorkflowRepository.findAll()).isEmpty();
        assertThat(projectDeploymentRepository.findById(projectDeploymentId)).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
        assertThat(projectRepository.findById(project.getId())).isPresent();
    }

    @Test
    void testDeleteServerDeletesEveryProjectOfTheServer() {
        A2aProject firstA2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(workflowId1));
        A2aProject secondA2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), secondProject.getId(), 1, List.of(workflowId3));

        a2aServerService.delete(a2aServer.getId());

        verify(a2aProjectFacade).deleteA2aProject(firstA2aProject.getId());
        verify(a2aProjectFacade).deleteA2aProject(secondA2aProject.getId());

        assertThat(a2aProjectRepository.findAll()).isEmpty();
        assertThat(a2aProjectWorkflowRepository.findAll()).isEmpty();
        assertThat(projectDeploymentRepository.findById(firstA2aProject.getProjectDeploymentId())).isEmpty();
        assertThat(projectDeploymentRepository.findById(secondA2aProject.getProjectDeploymentId())).isEmpty();
    }

    @Test
    void testDeleteServerWithoutProjectsDeletesNoProject() {
        A2aServer otherA2aServer = a2aServerService.create(
            new A2aServer("other-test-agent", null, Environment.DEVELOPMENT));

        A2aProject otherA2aProject = a2aProjectFacade.createA2aProject(
            otherA2aServer.getId(), project.getId(), 1, List.of(workflowId1));

        a2aServerService.delete(a2aServer.getId());

        verify(a2aProjectFacade, never()).deleteA2aProject(anyLong());

        assertThat(a2aServerRepository.findById(a2aServer.getId())).isEmpty();
        assertThat(a2aProjectRepository.findById(otherA2aProject.getId())).isPresent();
        assertThat(a2aProjectWorkflowRepository.findAll()).hasSize(1);
        assertThat(projectDeploymentRepository.findById(otherA2aProject.getProjectDeploymentId())).isPresent();
    }

    private Project createPublishedProject(Workspace workspace, String name) {
        Project newProject = Project.builder()
            .description(name)
            .name(name)
            .workspaceId(workspace.getId())
            .build();

        newProject.publish("v1");

        return projectRepository.save(newProject);
    }
}
