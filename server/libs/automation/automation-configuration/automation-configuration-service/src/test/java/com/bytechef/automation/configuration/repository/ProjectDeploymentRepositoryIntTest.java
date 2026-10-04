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

package com.bytechef.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class ProjectDeploymentRepositoryIntTest {

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @AfterEach
    public void afterEach() {
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Nested
    class FindAllProjectDeploymentsTest {

        @Test
        public void testSystemDeploymentPrefixesAreMatchedLiterally() {
            Project project = projectRepository.save(getProject("project"));

            saveProjectDeployment("__A2A_SERVER__1_v1", project);
            saveProjectDeployment("__API_COLLECTION__1_v1", project);
            saveProjectDeployment("__MCP_SERVER__1_v1", project);
            saveProjectDeployment("xxA2A_SERVERyy", project);
            saveProjectDeployment("xxAPI_COLLECTIONyy", project);
            saveProjectDeployment("xxMCP_SERVERyy", project);

            List<ProjectDeployment> projectDeployments = projectDeploymentRepository.findAllProjectDeployments(
                null, null, null, null, workspace.getId());

            assertThat(projectDeployments)
                .extracting(ProjectDeployment::getName)
                .containsExactly("xxA2A_SERVERyy", "xxAPI_COLLECTIONyy", "xxMCP_SERVERyy");
        }

        @Test
        public void testEmbeddedProjectPrefixIsMatchedLiterally() {
            Project embeddedProject = projectRepository.save(getProject("__EMBEDDED__project"));
            Project lookalikeProject = projectRepository.save(getProject("xxEMBEDDEDyyproject"));

            saveProjectDeployment("embedded", embeddedProject);
            saveProjectDeployment("lookalike", lookalikeProject);

            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(true, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactly("embedded");
            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(false, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactly("lookalike");
        }
    }

    private Project getProject(String name) {
        return Project.builder()
            .name(name)
            .workspaceId(workspace.getId())
            .build();
    }

    private void saveProjectDeployment(String name, Project project) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeploymentRepository.save(projectDeployment);
    }
}
