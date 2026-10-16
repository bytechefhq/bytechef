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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.config.TestOwnerContributors;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
public class ProjectDeploymentServiceIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestOwnerContributors testOwnerContributors;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Project project;

    @BeforeEach
    public void beforeEach() {
        testOwnerContributors.clear();

        Category category = categoryRepository.save(new Category("name"));
        Workspace workspace = workspaceRepository.save(new Workspace("test"));

        project = projectRepository.save(
            Project.builder()
                .categoryId(category.getId())
                .description("description")
                .name("name")
                .workspaceId(workspace.getId())
                .build());
    }

    @AfterEach
    public void afterEach() {
        testOwnerContributors.clear();

        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    public void testDisableAllProjectDeploymentsAlsoDisablesOwnedDeployments() {
        ProjectDeployment plainDeployment = saveEnabledDeployment("plain");
        ProjectDeployment mcpServerDeployment = saveEnabledDeployment("mcp");
        ProjectDeployment embeddedDeployment = saveEnabledDeployment("embedded");

        testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.MCP_SERVER, mcpServerDeployment.getId());
        testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.EMBEDDED, embeddedDeployment.getId());

        projectDeploymentService.disableAllProjectDeployments();

        assertThat(projectDeploymentRepository.findAllById(
            List.of(plainDeployment.getId(), mcpServerDeployment.getId(), embeddedDeployment.getId())))
                .hasSize(3)
                .noneMatch(ProjectDeployment::isEnabled);
    }

    private ProjectDeployment saveEnabledDeployment(String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        return projectDeploymentRepository.save(projectDeployment);
    }
}
