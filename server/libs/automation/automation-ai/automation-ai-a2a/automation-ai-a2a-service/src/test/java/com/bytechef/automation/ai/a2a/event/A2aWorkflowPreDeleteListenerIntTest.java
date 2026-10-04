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
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
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
class A2aWorkflowPreDeleteListenerIntTest {

    @Autowired
    private A2aProjectFacade a2aProjectFacade;

    @Autowired
    private A2aProjectRepository a2aProjectRepository;

    @Autowired
    private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

    @MockitoSpyBean
    private A2aProjectWorkflowService a2aProjectWorkflowService;

    @Autowired
    private A2aServerRepository a2aServerRepository;

    @Autowired
    private A2aWorkflowPreDeleteListener a2aWorkflowPreDeleteListener;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @MockitoSpyBean
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private A2aServer a2aServer;
    private String deletedWorkflowId;
    private String keptWorkflowId;
    private Project project;

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

        deletedWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);
        keptWorkflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

        projectWorkflowRepository.save(
            new ProjectWorkflow(project.getId(), 1, deletedWorkflowId, UUID.randomUUID()));
        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, keptWorkflowId, UUID.randomUUID()));
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

        A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(deletedWorkflowId, keptWorkflowId));
    }

    @Test
    void testDeleteWorkflowRemovesOnlyItsA2aSkillAndKeepsTheProject() {
        A2aProject a2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(deletedWorkflowId, keptWorkflowId));

        assertThat(a2aProjectWorkflowRepository.findAllByA2aProjectId(a2aProject.getId())).hasSize(2);

        ProjectDeploymentWorkflow deletedProjectDeploymentWorkflow = projectDeploymentWorkflowRepository
            .findAllByWorkflowId(deletedWorkflowId)
            .getFirst();
        A2aProjectWorkflow deletedA2aProjectWorkflow = a2aProjectWorkflowRepository
            .findAllByProjectDeploymentWorkflowId(deletedProjectDeploymentWorkflow.getId())
            .getFirst();

        projectWorkflowFacade.deleteWorkflow(deletedWorkflowId);

        InOrder inOrder = inOrder(a2aProjectWorkflowService, projectDeploymentWorkflowService);

        inOrder.verify(a2aProjectWorkflowService)
            .delete(deletedA2aProjectWorkflow.getId());
        inOrder.verify(projectDeploymentWorkflowService)
            .delete(deletedProjectDeploymentWorkflow.getId());

        List<A2aProjectWorkflow> remainingA2aProjectWorkflows =
            a2aProjectWorkflowRepository.findAllByA2aProjectId(a2aProject.getId());

        assertThat(remainingA2aProjectWorkflows).hasSize(1);
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(deletedWorkflowId)).isEmpty();

        List<ProjectDeploymentWorkflow> keptProjectDeploymentWorkflows =
            projectDeploymentWorkflowRepository.findAllByWorkflowId(keptWorkflowId);

        assertThat(keptProjectDeploymentWorkflows).hasSize(1);
        assertThat(remainingA2aProjectWorkflows.getFirst()
            .getProjectDeploymentWorkflowId()).isEqualTo(keptProjectDeploymentWorkflows.getFirst()
                .getId());
        assertThat(a2aProjectRepository.findById(a2aProject.getId())).isPresent();
    }

    @Test
    void testPreDeleteLeavesDeploymentWorkflowsThatNoA2aSkillOwns() {
        A2aProject a2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(keptWorkflowId));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("other-deployment");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(deletedWorkflowId);

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        a2aWorkflowPreDeleteListener.onWorkflowPreDelete(deletedWorkflowId);

        verify(a2aProjectWorkflowService, never()).delete(anyLong());
        verify(projectDeploymentWorkflowService, never()).delete(anyLong());

        assertThat(projectDeploymentWorkflowRepository.findById(projectDeploymentWorkflow.getId())).isPresent();
        assertThat(a2aProjectWorkflowRepository.findAllByA2aProjectId(a2aProject.getId())).hasSize(1);
    }
}
