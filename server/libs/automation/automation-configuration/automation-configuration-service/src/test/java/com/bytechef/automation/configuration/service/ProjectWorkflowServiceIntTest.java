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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.apache.commons.lang3.Validate;
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
public class ProjectWorkflowServiceIntTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @AfterEach
    public void afterEach() {
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @BeforeEach
    public void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @Test
    public void testAddWorkflow() {
        Project project = projectRepository.save(getProject());

        projectWorkflowService.addWorkflow(
            Validate.notNull(project.getId(), "id"), project.getLastProjectVersion(), "workflow2");

        assertThat(projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastProjectVersion()))
            .contains("workflow2");
    }

    @Test
    public void testPublishWorkflow() {
        Project project = projectRepository.save(getProject());

        long projectId = Validate.notNull(project.getId(), "id");
        int initialVersion = project.getLastProjectVersion();

        var initialWorkflow = projectWorkflowService.addWorkflow(projectId, initialVersion, "workflow1");

        var workflowUuid = initialWorkflow.getUuid();

        assertThat(workflowUuid).isNotNull();

        var workflowsBeforePublish = projectWorkflowService.getProjectWorkflows(projectId, workflowUuid.toString());

        assertThat(workflowsBeforePublish).hasSize(1);

        ProjectWorkflow firstProjectWorkflow = workflowsBeforePublish.getFirst();

        assertThat(firstProjectWorkflow.getWorkflowId()).isEqualTo("workflow1");

        int newVersion = initialVersion + 1;
        String newWorkflowId = "workflow1_v2";

        var workflowToPublish = projectWorkflowService.getProjectWorkflow(initialWorkflow.getId());

        workflowToPublish.setProjectVersion(newVersion);
        workflowToPublish.setWorkflowId(newWorkflowId);

        projectWorkflowService.publishWorkflow(projectId, initialVersion, "workflow1", workflowToPublish);

        var workflowsAfterPublish = projectWorkflowService.getProjectWorkflows(projectId, workflowUuid.toString());

        assertThat(workflowsAfterPublish).hasSize(2);

        assertThat(workflowsAfterPublish)
            .extracting("uuid")
            .containsOnly(workflowUuid);

        assertThat(workflowsAfterPublish)
            .anyMatch(
                workflow -> workflow.getProjectVersion() == initialVersion &&
                    "workflow1".equals(workflow.getWorkflowId()));

        assertThat(workflowsAfterPublish)
            .anyMatch(
                workflow -> workflow.getProjectVersion() == newVersion &&
                    newWorkflowId.equals(workflow.getWorkflowId()));
    }

    private Project getProject() {
        return Project.builder()
            .description("description")
            .name("name")
            .workspaceId(workspace.getId())
            .build();
    }
}
