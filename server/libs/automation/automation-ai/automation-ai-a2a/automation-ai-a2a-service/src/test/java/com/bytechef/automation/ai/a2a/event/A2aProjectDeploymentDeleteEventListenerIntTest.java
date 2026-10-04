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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.config.A2aIntTestWorkflows;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
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
import org.mockito.InOrder;
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
class A2aProjectDeploymentDeleteEventListenerIntTest {

    @Autowired
    private A2aProjectFacade a2aProjectFacade;

    @Autowired
    private A2aProjectRepository a2aProjectRepository;

    @MockitoSpyBean
    private A2aProjectService a2aProjectService;

    @Autowired
    private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

    @MockitoSpyBean
    private A2aProjectWorkflowService a2aProjectWorkflowService;

    @Autowired
    private A2aServerRepository a2aServerRepository;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

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
    private String workflowId;

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

        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId, UUID.randomUUID()));
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
    }

    @Test
    void testDeleteProjectDeploymentRemovesTheA2aProjectAndItsSkills() {
        A2aProject a2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(workflowId));

        long projectDeploymentId = a2aProject.getProjectDeploymentId();
        List<A2aProjectWorkflow> a2aProjectWorkflows = a2aProjectWorkflowRepository.findAllByA2aProjectId(
            a2aProject.getId());

        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentId);

        InOrder inOrder = inOrder(a2aProjectWorkflowService, a2aProjectService);

        assertThat(a2aProjectWorkflows).singleElement()
            .satisfies(a2aProjectWorkflow -> inOrder.verify(a2aProjectWorkflowService)
                .delete(a2aProjectWorkflow.getId()));

        inOrder.verify(a2aProjectService)
            .delete(a2aProject.getId());

        assertThat(projectDeploymentRepository.findById(projectDeploymentId)).isEmpty();
        assertThat(a2aProjectRepository.findById(a2aProject.getId())).isEmpty();
        assertThat(a2aProjectWorkflowRepository.findAll()).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
        assertThat(a2aServerRepository.findById(a2aServer.getId())).isPresent();
    }

    @Test
    void testDeleteProjectDeploymentThatNoA2aProjectOwnsLeavesA2aProjectsUntouched() {
        A2aProject a2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(workflowId));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("other-deployment");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        ProjectDeployment otherProjectDeployment = projectDeploymentRepository.save(projectDeployment);

        projectDeploymentFacade.deleteProjectDeployment(otherProjectDeployment.getId());

        assertThat(projectDeploymentRepository.findById(otherProjectDeployment.getId())).isEmpty();
        assertThat(a2aProjectRepository.findById(a2aProject.getId())).isPresent();
        assertThat(a2aProjectWorkflowRepository.findAllByA2aProjectId(a2aProject.getId())).hasSize(1);

        verify(a2aProjectService, never()).delete(anyLong());
        verify(a2aProjectWorkflowService, never()).delete(anyLong());
    }
}
