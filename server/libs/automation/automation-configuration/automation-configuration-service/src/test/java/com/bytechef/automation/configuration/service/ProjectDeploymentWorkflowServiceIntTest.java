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
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
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
class ProjectDeploymentWorkflowServiceIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ProjectDeployment otherProjectDeployment;
    private ProjectDeployment projectDeployment;

    @BeforeEach
    void beforeEach() {
        Category category = categoryRepository.save(new Category("project-deployment-workflow"));
        Workspace workspace = workspaceRepository.save(new Workspace("project-deployment-workflow"));

        Project project = projectRepository.save(
            Project.builder()
                .categoryId(category.getId())
                .name("project-deployment-workflow")
                .workspaceId(workspace.getId())
                .build());

        projectDeployment = saveProjectDeployment(project, "deployment");
        otherProjectDeployment = saveProjectDeployment(project, "other-deployment");
    }

    @AfterEach
    void afterEach() {
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testCreate() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService.create(
            newProjectDeploymentWorkflow(projectDeployment, "workflow-1", Map.of("input", "value")));

        assertThat(projectDeploymentWorkflow.getId()).isNotNull();
        assertThat(projectDeploymentWorkflowRepository.findById(projectDeploymentWorkflow.getId()))
            .hasValueSatisfying(savedProjectDeploymentWorkflow -> {
                assertThat(savedProjectDeploymentWorkflow.getProjectDeploymentId())
                    .isEqualTo(projectDeployment.getId());
                assertThat(savedProjectDeploymentWorkflow.getWorkflowId()).isEqualTo("workflow-1");
                assertThat(savedProjectDeploymentWorkflow.isEnabled()).isTrue();
                assertThat(savedProjectDeploymentWorkflow.getInputs()).isEqualTo(Map.of("input", "value"));
            });
    }

    @Test
    void testGetProjectDeploymentWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(
            newProjectDeploymentWorkflow(projectDeployment, "workflow-1", Map.of()));

        projectDeploymentWorkflowRepository.save(
            newProjectDeploymentWorkflow(otherProjectDeployment, "workflow-1", Map.of()));

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflow.getId()))
            .isEqualTo(projectDeploymentWorkflow);
        assertThat(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeployment.getId(), "workflow-1"))
                .isEqualTo(projectDeploymentWorkflow);
        assertThat(
            projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(projectDeployment.getId(), "missing"))
                .isEmpty();
    }

    @Test
    void testGetProjectDeploymentWorkflows() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow1 = projectDeploymentWorkflowRepository.save(
            newProjectDeploymentWorkflow(projectDeployment, "workflow-1", Map.of()));
        ProjectDeploymentWorkflow projectDeploymentWorkflow2 = projectDeploymentWorkflowRepository.save(
            newProjectDeploymentWorkflow(projectDeployment, "workflow-2", Map.of()));
        ProjectDeploymentWorkflow otherProjectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(
            newProjectDeploymentWorkflow(otherProjectDeployment, "workflow-1", Map.of()));

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeployment.getId()))
            .containsExactlyInAnyOrder(projectDeploymentWorkflow1, projectDeploymentWorkflow2);
        assertThat(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                List.of(projectDeployment.getId(), otherProjectDeployment.getId())))
                    .containsExactlyInAnyOrder(
                        projectDeploymentWorkflow1, projectDeploymentWorkflow2, otherProjectDeploymentWorkflow);
    }

    @Test
    void testUpdate() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(
            newProjectDeploymentWorkflow(projectDeployment, "workflow-1", Map.of("input", "old")));

        projectDeploymentWorkflow.setEnabled(false);
        projectDeploymentWorkflow.setInputs(Map.of("input", "new"));
        projectDeploymentWorkflow.setWorkflowId("workflow-2");

        projectDeploymentWorkflowService.update(projectDeploymentWorkflow);

        assertThat(projectDeploymentWorkflowRepository.findById(projectDeploymentWorkflow.getId()))
            .hasValueSatisfying(updatedProjectDeploymentWorkflow -> {
                assertThat(updatedProjectDeploymentWorkflow.isEnabled()).isFalse();
                assertThat(updatedProjectDeploymentWorkflow.getInputs()).isEqualTo(Map.of("input", "new"));
                assertThat(updatedProjectDeploymentWorkflow.getWorkflowId()).isEqualTo("workflow-2");
                assertThat(updatedProjectDeploymentWorkflow.getProjectDeploymentId())
                    .isEqualTo(projectDeployment.getId());
            });
    }

    private static ProjectDeploymentWorkflow newProjectDeploymentWorkflow(
        ProjectDeployment projectDeployment, String workflowId, Map<String, ?> inputs) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(inputs);
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private ProjectDeployment saveProjectDeployment(Project project, String name) {
        ProjectDeployment newProjectDeployment = new ProjectDeployment();

        newProjectDeployment.setEnabled(true);
        newProjectDeployment.setEnvironment(Environment.DEVELOPMENT);
        newProjectDeployment.setName(name);
        newProjectDeployment.setProjectId(project.getId());
        newProjectDeployment.setProjectVersion(1);

        return projectDeploymentRepository.save(newProjectDeployment);
    }
}
