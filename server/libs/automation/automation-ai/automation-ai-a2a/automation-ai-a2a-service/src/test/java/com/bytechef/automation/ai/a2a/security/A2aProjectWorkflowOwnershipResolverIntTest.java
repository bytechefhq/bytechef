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

package com.bytechef.automation.ai.a2a.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = A2aIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@A2aIntTestConfigurationSharedMocks
class A2aProjectWorkflowOwnershipResolverIntTest {

    @Autowired
    private A2aProjectRepository a2aProjectRepository;

    @Autowired
    private A2aProjectWorkflowOwnershipResolver a2aProjectWorkflowOwnershipResolver;

    @Autowired
    private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

    @Autowired
    private A2aServerRepository a2aServerRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace otherWorkspace;
    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test-workspace"));
        otherWorkspace = workspaceRepository.save(new Workspace("other-test-workspace"));
    }

    @AfterEach
    void afterEach() {
        a2aProjectWorkflowRepository.deleteAll();
        a2aProjectRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        a2aServerRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testResourceType() {
        assertThat(a2aProjectWorkflowOwnershipResolver.resourceType()).isEqualTo("A2aProjectWorkflow");
    }

    @Test
    void testResolvesTheWorkspaceSharedByServerAndProject() {
        A2aProjectWorkflow a2aProjectWorkflow = createA2aProjectWorkflow(workspace, workspace);

        assertThat(a2aProjectWorkflowOwnershipResolver.resolveOwner(a2aProjectWorkflow.getId())
            .workspaceId()).hasValue(workspace.getId());
    }

    @Test
    void testSkillOfAProjectOfAnotherWorkspaceThanTheServerIsUnknown() {
        A2aProjectWorkflow a2aProjectWorkflow = createA2aProjectWorkflow(otherWorkspace, workspace);

        assertThat(a2aProjectWorkflowOwnershipResolver.resolveOwner(a2aProjectWorkflow.getId())
            .workspaceId()).isEmpty();
    }

    @Test
    void testUnknownIsUnknown() {
        assertThat(a2aProjectWorkflowOwnershipResolver.resolveOwner(Long.MAX_VALUE)
            .workspaceId()).isEmpty();
    }

    private A2aProjectWorkflow createA2aProjectWorkflow(Workspace serverWorkspace, Workspace projectWorkspace) {
        A2aServer a2aServer = new A2aServer("test-agent", null, Environment.DEVELOPMENT);

        a2aServer.setWorkspaceId(serverWorkspace.getId());

        a2aServer = a2aServerRepository.save(a2aServer);

        Project project = Project.builder()
            .description("test-project")
            .name("test-project")
            .workspaceId(projectWorkspace.getId())
            .build();

        project = projectRepository.save(project);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId("test-workflow");

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        A2aProject a2aProject = a2aProjectRepository.save(
            new A2aProject(projectDeployment.getId(), a2aServer.getId(), project.getId()));

        return a2aProjectWorkflowRepository.save(
            new A2aProjectWorkflow(a2aProject.getId(), projectDeploymentWorkflow.getId()));
    }
}
