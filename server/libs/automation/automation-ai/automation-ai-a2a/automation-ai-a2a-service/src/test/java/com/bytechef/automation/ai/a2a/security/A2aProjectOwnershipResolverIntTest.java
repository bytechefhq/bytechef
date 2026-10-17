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
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
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
class A2aProjectOwnershipResolverIntTest {

    @Autowired
    private A2aProjectOwnershipResolver a2aProjectOwnershipResolver;

    @Autowired
    private A2aProjectRepository a2aProjectRepository;

    @Autowired
    private A2aServerRepository a2aServerRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

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
        a2aProjectRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        a2aServerRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testResourceType() {
        assertThat(a2aProjectOwnershipResolver.resourceType()).isEqualTo("A2aProject");
    }

    @Test
    void testResolvesTheWorkspaceSharedByServerAndProject() {
        Project project = createProject(workspace);

        A2aProject a2aProject = createA2aProject(createA2aServer(workspace), project, project.getId());

        assertThat(a2aProjectOwnershipResolver.resolveOwner(a2aProject.getId())
            .workspaceId()).hasValue(workspace.getId());
    }

    @Test
    void testProjectOfAnotherWorkspaceThanTheServerIsUnknown() {
        Project project = createProject(workspace);

        A2aProject a2aProject = createA2aProject(createA2aServer(otherWorkspace), project, project.getId());

        assertThat(a2aProjectOwnershipResolver.resolveOwner(a2aProject.getId())
            .workspaceId()).isEmpty();
    }

    @Test
    void testMissingProjectIsUnknown() {
        A2aProject a2aProject = createA2aProject(createA2aServer(workspace), createProject(workspace), Long.MAX_VALUE);

        assertThat(a2aProjectOwnershipResolver.resolveOwner(a2aProject.getId())
            .workspaceId()).isEmpty();
    }

    @Test
    void testUnknownIsUnknown() {
        assertThat(a2aProjectOwnershipResolver.resolveOwner(Long.MAX_VALUE)
            .workspaceId()).isEmpty();
    }

    private A2aProject createA2aProject(A2aServer a2aServer, Project deployedProject, long projectId) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(deployedProject.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        return a2aProjectRepository.save(new A2aProject(projectDeployment.getId(), a2aServer.getId(), projectId));
    }

    private A2aServer createA2aServer(Workspace serverWorkspace) {
        A2aServer a2aServer = new A2aServer("test-agent", null, Environment.DEVELOPMENT);

        a2aServer.setWorkspaceId(serverWorkspace.getId());

        return a2aServerRepository.save(a2aServer);
    }

    private Project createProject(Workspace projectWorkspace) {
        Project project = Project.builder()
            .description("test-project")
            .name("test-project")
            .workspaceId(projectWorkspace.getId())
            .build();

        return projectRepository.save(project);
    }
}
