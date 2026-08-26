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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class WorkflowNodeTestOutputDeleteCascadeIntTest {

    private static final String WORKFLOW_DEFINITION =
        "{\"label\":\"Test Workflow\",\"description\":\"Test workflow\",\"tasks\":[]}";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @MockitoBean
    private PreBuiltTemplateService preBuiltTemplateService;

    @MockitoBean
    private SharedTemplateFileStorage sharedTemplateFileStorage;

    @MockitoBean
    private SharedTemplateService sharedTemplateService;

    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test-workspace"));
    }

    @AfterEach
    public void afterEach() {
        workflowNodeTestOutputRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();

        workflowCrudRepository.findAll()
            .forEach(workflow -> workflowCrudRepository.deleteById(workflow.getId()));

        workspaceRepository.deleteAll();
    }

    @Test
    public void testDeleteWorkflowDeletesWorkflowNodeTestOutputs() {
        Project project = new Project();

        project.setName("test-project");
        project.setWorkspaceId(workspace.getId());

        project = projectRepository.save(project);

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(project.getId(), WORKFLOW_DEFINITION);
        ProjectWorkflow otherProjectWorkflow = projectWorkflowFacade.addWorkflow(project.getId(), WORKFLOW_DEFINITION);

        String workflowId = projectWorkflow.getWorkflowId();
        String otherWorkflowId = otherProjectWorkflow.getWorkflowId();

        insertWorkflowNodeTestOutput(workflowId, "trigger_1", 0);
        insertWorkflowNodeTestOutput(workflowId, "action_1", 1);
        insertWorkflowNodeTestOutput(otherWorkflowId, "action_1", 0);

        assertThat(workflowNodeTestOutputRepository.findByWorkflowId(workflowId)).hasSize(2);
        assertThat(workflowNodeTestOutputRepository.findByWorkflowId(otherWorkflowId)).hasSize(1);

        projectWorkflowFacade.deleteWorkflow(workflowId);

        assertThat(workflowNodeTestOutputRepository.findByWorkflowId(workflowId)).isEmpty();
        assertThat(workflowNodeTestOutputRepository.findByWorkflowId(otherWorkflowId)).hasSize(1);
    }

    private void insertWorkflowNodeTestOutput(String workflowId, String workflowNodeName, int environmentId) {
        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update(
            "INSERT INTO workflow_node_test_output (workflow_id, workflow_node_name, environment, type_name, " +
                "type_operation_name, type_version, output_schema, sample_output, created_by, created_date, " +
                "last_modified_by, last_modified_date, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            workflowId, workflowNodeName, environmentId, "component", "action", 1, "{}", "{}", "test", now, "test",
            now, 0);
    }
}
