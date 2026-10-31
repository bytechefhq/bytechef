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

package com.bytechef.automation.ai.mcp.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.config.McpIntTestWorkflows;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.ai.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpProjectIntTestConfiguration.class, properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
class McpProjectWorkflowMappingValidatorIntTest {

    private static final String REQUIRED_INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {"city": {"type": "string"}},
            "required": ["city"]
        }""";

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private McpProjectRepository mcpProjectRepository;

    @Autowired
    private McpProjectWorkflowMappingValidator mcpProjectWorkflowMappingValidator;

    @Autowired
    private McpProjectWorkflowRepository mcpProjectWorkflowRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceMcpServerRepository workspaceMcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private McpProject mcpProject;
    private McpServer mcpServer;
    private ProjectDeployment projectDeployment;
    private final List<String> workflowIds = new ArrayList<>();

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("mcp-project-workflow-mapping-validator"));

        mcpServer = mcpServerRepository.save(
            new McpServer("mcp-project-workflow-mapping-validator", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        workspaceMcpServerRepository.save(new WorkspaceMcpServer(mcpServer.getId(), workspace.getId()));

        Category category = categoryRepository.save(new Category("mcp-project-workflow-mapping-validator"));

        Project project = projectRepository.save(
            Project.builder()
                .categoryId(category.getId())
                .name("mcp-project-workflow-mapping-validator")
                .workspaceId(workspace.getId())
                .build());

        ProjectDeployment newProjectDeployment = new ProjectDeployment();

        newProjectDeployment.setEnabled(true);
        newProjectDeployment.setEnvironment(Environment.DEVELOPMENT);
        newProjectDeployment.setName("mcp-project-workflow-mapping-validator");
        newProjectDeployment.setProjectId(project.getId());
        newProjectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(newProjectDeployment);

        mcpProject = mcpProjectRepository.save(new McpProject(projectDeployment.getId(), mcpServer.getId()));
    }

    @AfterEach
    void afterEach() {
        mcpProjectWorkflowRepository.deleteAll();
        mcpProjectRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        categoryRepository.deleteAll();
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceRepository.deleteAll();

        McpIntTestWorkflows.deleteWorkflows(workflowService, workflowIds);

        workflowIds.clear();
    }

    @Test
    void testValidateEnablementThrowsNamingUnmappedWorkflow() {
        saveMcpProjectWorkflow(
            saveDeployment("Mapped Workflow", true),
            Map.of("toolName", "mapped_tool", "city", "fromAi('city', 'STRING', {required: true})"));
        saveMcpProjectWorkflow(saveDeployment("Unmapped Workflow", true), Map.of());

        assertThatThrownBy(() -> mcpProjectWorkflowMappingValidator.validateEnablement(mcpServer.getId()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Unmapped Workflow")
            .hasMessageNotContaining("Mapped Workflow");
    }

    @Test
    void testValidateEnablementSucceedsWhenAllMapped() {
        saveMcpProjectWorkflow(
            saveDeployment("Mapped Workflow", true),
            Map.of("toolName", "mapped_tool", "city", "fromAi('city', 'STRING', {required: true})"));

        assertThatCode(() -> mcpProjectWorkflowMappingValidator.validateEnablement(mcpServer.getId()))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateEnablementFallsBackToWorkflowLabelForToolName() {
        saveMcpProjectWorkflow(
            saveDeployment("workflow1", true), Map.of("city", "fromAi('city', 'STRING', {required: true})"));

        assertThatCode(() -> mcpProjectWorkflowMappingValidator.validateEnablement(mcpServer.getId()))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateEnablementThrowsWhenToolNameCannotBeDerived() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = saveDeployment(null, true);

        saveMcpProjectWorkflow(
            projectDeploymentWorkflow, Map.of("city", "fromAi('city', 'STRING', {required: true})"));

        assertThatThrownBy(() -> mcpProjectWorkflowMappingValidator.validateEnablement(mcpServer.getId()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining(projectDeploymentWorkflow.getWorkflowId());
    }

    @Test
    void testValidateEnablementThrowsWhenRequiredInputUnmapped() {
        saveMcpProjectWorkflow(saveDeployment("Partially Mapped Workflow", true), Map.of("toolName", "mapped_tool"));

        assertThatThrownBy(() -> mcpProjectWorkflowMappingValidator.validateEnablement(mcpServer.getId()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Partially Mapped Workflow");
    }

    @Test
    void testValidateEnablementIgnoresDisabledDeployment() {
        saveMcpProjectWorkflow(saveDeployment("Unmapped But Disabled Workflow", false), Map.of());

        assertThatCode(() -> mcpProjectWorkflowMappingValidator.validateEnablement(mcpServer.getId()))
            .doesNotThrowAnyException();
    }

    private ProjectDeploymentWorkflow saveDeployment(@Nullable String workflowLabel, boolean enabled) {
        Map<String, Object> definition = new LinkedHashMap<>();

        if (workflowLabel != null) {
            definition.put("label", workflowLabel);
        }

        definition.put(
            "triggers",
            List.of(
                Map.of(
                    "name", "newWorkflowCall_1",
                    "type", "workflow/v1/newWorkflowCall",
                    "parameters", Map.of("inputSchema", REQUIRED_INPUT_SCHEMA))));
        definition.put("tasks", List.of());

        Workflow workflow = workflowService.create(
            JsonUtils.write(definition), Workflow.Format.JSON, Workflow.SourceType.JDBC);

        workflowIds.add(workflow.getId());

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(enabled);
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(workflow.getId());

        return projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);
    }

    private void saveMcpProjectWorkflow(
        ProjectDeploymentWorkflow projectDeploymentWorkflow, Map<String, Object> parameters) {

        McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow(
            mcpProject.getId(), projectDeploymentWorkflow.getId());

        mcpProjectWorkflow.setParameters(parameters);

        mcpProjectWorkflowRepository.save(mcpProjectWorkflow);
    }
}
