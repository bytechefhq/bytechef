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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

/**
 * Pins the "complete mapping" definition the enable-guard enforces: every exposed workflow (one with a callable
 * {@code workflow/newWorkflowCall} trigger, attached through an enabled project-deployment-workflow) must carry a
 * non-null {@code toolName} and a value — {@code fromAi(...)} expression or literal — for every input its trigger's
 * {@code inputSchema} marks required. This mirrors exactly what {@code AutomationMcpToolFacade} (the serve path) needs:
 * a missing {@code toolName} is the serve path's own skip condition, and a required input with neither a
 * {@code fromAi(...)} expression nor a literal is never asked of the calling model nor forwarded to the workflow.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class McpProjectWorkflowMappingValidatorTest {

    private static final long MCP_SERVER_ID = 30L;
    private static final long MCP_PROJECT_ID = 10L;
    private static final String NEW_WORKFLOW_CALL_TYPE = "workflow/v1/newWorkflowCall";
    private static final String REQUIRED_INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {"city": {"type": "string"}},
            "required": ["city"]
        }""";

    private final McpProjectService mcpProjectService = mock(McpProjectService.class);
    private final McpProjectWorkflowService mcpProjectWorkflowService = mock(McpProjectWorkflowService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final McpProjectWorkflowMappingValidator validator = new McpProjectWorkflowMappingValidator(
        mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService, workflowService);

    @Test
    void testValidateEnablementThrowsNamingUnmappedWorkflow() {
        McpProjectWorkflow mappedMcpProjectWorkflow = mcpProjectWorkflow(
            21L, Map.of("toolName", "mapped_tool", "city", "fromAi('city', 'STRING', {required: true})"));
        McpProjectWorkflow unmappedMcpProjectWorkflow = mcpProjectWorkflow(22L, Map.of());

        stubMcpServerProjects(mappedMcpProjectWorkflow, unmappedMcpProjectWorkflow);

        stubEnabledDeployment(21L, "wf1", "Mapped Workflow");
        stubEnabledDeployment(22L, "wf2", "Unmapped Workflow");

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(
                workflowTriggerMockedStatic, workflowNodeTypeMockedStatic, REQUIRED_INPUT_SCHEMA);

            assertThatThrownBy(() -> validator.validateEnablement(MCP_SERVER_ID))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Unmapped Workflow")
                .hasMessageNotContaining("Mapped Workflow");
        }
    }

    @Test
    void testValidateEnablementSucceedsWhenAllMapped() {
        McpProjectWorkflow mappedMcpProjectWorkflow = mcpProjectWorkflow(
            21L, Map.of("toolName", "mapped_tool", "city", "fromAi('city', 'STRING', {required: true})"));

        stubMcpServerProjects(mappedMcpProjectWorkflow);

        stubEnabledDeployment(21L, "wf1", "Mapped Workflow");

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(
                workflowTriggerMockedStatic, workflowNodeTypeMockedStatic, REQUIRED_INPUT_SCHEMA);

            assertThatCode(() -> validator.validateEnablement(MCP_SERVER_ID)).doesNotThrowAnyException();
        }
    }

    @Test
    void testValidateEnablementThrowsWhenRequiredInputUnmapped() {
        McpProjectWorkflow partiallyMappedMcpProjectWorkflow = mcpProjectWorkflow(
            21L, Map.of("toolName", "mapped_tool"));

        stubMcpServerProjects(partiallyMappedMcpProjectWorkflow);

        stubEnabledDeployment(21L, "wf1", "Partially Mapped Workflow");

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            stubNewWorkflowCallTrigger(
                workflowTriggerMockedStatic, workflowNodeTypeMockedStatic, REQUIRED_INPUT_SCHEMA);

            assertThatThrownBy(() -> validator.validateEnablement(MCP_SERVER_ID))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Partially Mapped Workflow");
        }
    }

    @Test
    void testValidateEnablementIgnoresDisabledDeployment() {
        McpProjectWorkflow unmappedButDisabledMcpProjectWorkflow = mcpProjectWorkflow(21L, Map.of());

        stubMcpServerProjects(unmappedButDisabledMcpProjectWorkflow);

        ProjectDeploymentWorkflow disabledProjectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(disabledProjectDeploymentWorkflow.isEnabled()).thenReturn(false);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(21L))
            .thenReturn(disabledProjectDeploymentWorkflow);

        assertThatCode(() -> validator.validateEnablement(MCP_SERVER_ID)).doesNotThrowAnyException();
    }

    private void stubMcpServerProjects(McpProjectWorkflow... mcpProjectWorkflows) {
        McpProject mcpProject = mock(McpProject.class);

        when(mcpProject.getId()).thenReturn(MCP_PROJECT_ID);

        when(mcpProjectService.getMcpServerMcpProjects(MCP_SERVER_ID)).thenReturn(List.of(mcpProject));
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(MCP_PROJECT_ID))
            .thenReturn(List.of(mcpProjectWorkflows));
    }

    private static McpProjectWorkflow mcpProjectWorkflow(
        long projectDeploymentWorkflowId, Map<String, Object> parameters) {

        McpProjectWorkflow mcpProjectWorkflow = mock(McpProjectWorkflow.class);

        when(mcpProjectWorkflow.getProjectDeploymentWorkflowId()).thenReturn(projectDeploymentWorkflowId);
        doReturn(parameters).when(mcpProjectWorkflow)
            .getParameters();

        return mcpProjectWorkflow;
    }

    private void stubEnabledDeployment(long projectDeploymentWorkflowId, String workflowId, String workflowLabel) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(projectDeploymentWorkflow.isEnabled()).thenReturn(true);
        when(projectDeploymentWorkflow.getWorkflowId()).thenReturn(workflowId);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId))
            .thenReturn(projectDeploymentWorkflow);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn(workflowLabel);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
    }

    /**
     * Installs a single shared callable {@code workflow/newWorkflowCall} trigger, returned for every {@code Workflow}
     * mock — mirroring {@code AutomationMcpToolFacadeWorkflowToolsTest}'s precedent. Every workflow in this test class
     * uses the same required-input shape, so one shared trigger is sufficient; only
     * {@code McpProjectWorkflow.parameters} differs between the mapped and unmapped fixtures.
     */
    private static void stubNewWorkflowCallTrigger(
        MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic,
        MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic, String inputSchema) {

        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);
        WorkflowNodeType workflowNodeType = mock(WorkflowNodeType.class);

        when(workflowTrigger.getType()).thenReturn(NEW_WORKFLOW_CALL_TYPE);
        when(workflowTrigger.getName()).thenReturn("newWorkflowCall_1");
        doReturn(Map.of(WorkflowConstants.INPUT_SCHEMA, inputSchema)).when(workflowTrigger)
            .getParameters();

        when(workflowNodeType.name()).thenReturn(WorkflowConstants.WORKFLOW);
        when(workflowNodeType.operation()).thenReturn(WorkflowConstants.NEW_WORKFLOW_CALL);

        workflowTriggerMockedStatic.when(() -> WorkflowTrigger.of(any(Workflow.class)))
            .thenReturn(List.of(workflowTrigger));
        workflowNodeTypeMockedStatic.when(() -> WorkflowNodeType.ofType(NEW_WORKFLOW_CALL_TYPE))
            .thenReturn(workflowNodeType);
    }
}
