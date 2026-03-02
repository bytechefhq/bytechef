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

package com.bytechef.automation.mcp.web.graphql;

import static com.bytechef.platform.component.constant.WorkflowConstants.NEW_AI_MODEL_CALL;
import static com.bytechef.platform.component.constant.WorkflowConstants.WORKFLOW;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.facade.McpProjectWorkflowFacade;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpProjectWorkflow} entities.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpProjectWorkflowGraphQlController {

    private final McpProjectWorkflowFacade mcpProjectWorkflowFacade;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    McpProjectWorkflowGraphQlController(
        McpProjectWorkflowFacade mcpProjectWorkflowFacade,
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.mcpProjectWorkflowFacade = mcpProjectWorkflowFacade;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @QueryMapping
    McpProjectWorkflow mcpProjectWorkflow(@Argument long id) {
        return mcpProjectWorkflowService.fetchMcpProjectWorkflow(id)
            .orElse(null);
    }

    @QueryMapping
    List<McpProjectWorkflow> mcpProjectWorkflows() {
        return mcpProjectWorkflowService.getMcpProjectWorkflows();
    }

    @QueryMapping
    List<McpProjectWorkflow> mcpProjectWorkflowsByMcpProjectId(@Argument long mcpProjectId) {
        return mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId);
    }

    @QueryMapping
    List<McpProjectWorkflow>
        mcpProjectWorkflowsByProjectDeploymentWorkflowId(@Argument long projectDeploymentWorkflowId) {
        return mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(projectDeploymentWorkflowId);
    }

    @QueryMapping
    List<ProjectWorkflow> toolEligibleProjectVersionWorkflows(@Argument long projectId, @Argument int projectVersion) {
        List<String> workflowIds = projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        List<Workflow> workflows = workflowService.getWorkflows(workflowIds);

        Map<String, Workflow> workflowMap = workflows
            .stream()
            .collect(Collectors.toMap(Workflow::getId, Function.identity()));

        return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
            .stream()
            .filter(projectWorkflow -> {
                Workflow workflow = workflowMap.get(projectWorkflow.getWorkflowId());

                return workflow != null && getToolCallableTrigger(workflow) != null;
            })
            .toList();
    }

    @SuppressFBWarnings("BC_VACUOUS_INSTANCEOF")
    @QueryMapping
    List<Property> mcpProjectWorkflowProperties(@Argument long mcpProjectWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
            .orElse(null);

        if (mcpProjectWorkflow == null) {
            return List.of();
        }

        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                mcpProjectWorkflow.getProjectDeploymentWorkflowId());

        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        WorkflowTrigger trigger = getToolCallableTrigger(workflow);

        if (trigger == null) {
            return List.of();
        }

        String inputSchema = MapUtils.getString(trigger.getParameters(), WorkflowConstants.INPUT_SCHEMA);

        if (inputSchema == null || inputSchema.isEmpty()) {
            return List.of();
        }

        BaseValueProperty<?> inputProperty = SchemaUtils.getJsonSchemaProperty(
            inputSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY);

        if (inputProperty == null) {
            return List.of();
        }

        if (inputProperty instanceof com.bytechef.component.definition.Property.ObjectProperty objectProperty) {
            List<Property> properties = new ArrayList<>();

            for (com.bytechef.component.definition.Property childProperty : objectProperty.getProperties()
                .orElse(List.of())) {

                properties.add(Property.toProperty(childProperty));
            }

            return properties;
        }

        return List.of(Property.toProperty((com.bytechef.component.definition.Property) inputProperty));
    }

    @MutationMapping
    McpProjectWorkflow createMcpProjectWorkflow(@Argument("input") Map<String, Object> input) {
        Long mcpProjectId = Long.valueOf(String.valueOf(input.get("mcpProjectId")));
        Long projectDeploymentWorkflowId = Long.valueOf(String.valueOf(input.get("projectDeploymentWorkflowId")));

        return mcpProjectWorkflowService.create(mcpProjectId, projectDeploymentWorkflowId);
    }

    @SuppressWarnings("unchecked")
    @MutationMapping
    McpProjectWorkflow updateMcpProjectWorkflow(@Argument("id") long id, @Argument("input") Map<String, Object> input) {
        Long mcpProjectId = null;

        if (input.containsKey("mcpProjectId")) {
            mcpProjectId = Long.valueOf(String.valueOf(input.get("mcpProjectId")));
        }

        Long projectDeploymentWorkflowId = null;

        if (input.containsKey("projectDeploymentWorkflowId")) {
            projectDeploymentWorkflowId = Long.valueOf(String.valueOf(input.get("projectDeploymentWorkflowId")));
        }

        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.update(
            id, mcpProjectId, projectDeploymentWorkflowId);

        if (input.containsKey("parameters")) {
            Object parametersObject = input.get("parameters");

            if (parametersObject != null && !(parametersObject instanceof Map)) {
                Class<?> parametersClass = parametersObject.getClass();

                throw new IllegalArgumentException(
                    "Expected parameters to be a Map, but got: " + parametersClass.getName());
            }

            Map<String, ?> parameters = parametersObject != null ? (Map<String, ?>) parametersObject : Map.of();

            mcpProjectWorkflow = mcpProjectWorkflowService.updateParameters(id, parameters);
        }

        return mcpProjectWorkflow;
    }

    @MutationMapping
    boolean deleteMcpProjectWorkflow(@Argument("id") long id) {
        mcpProjectWorkflowFacade.deleteMcpProjectWorkflow(id);

        return true;
    }

    @SchemaMapping
    ProjectDeploymentWorkflow projectDeploymentWorkflow(McpProjectWorkflow mcpProjectWorkflow) {
        return projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
            mcpProjectWorkflow.getProjectDeploymentWorkflowId());
    }

    @SchemaMapping
    WorkflowDTO workflow(McpProjectWorkflow mcpProjectWorkflow) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                mcpProjectWorkflow.getProjectDeploymentWorkflowId());

        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        return new WorkflowDTO(workflow, List.of(), List.of());
    }

    private static WorkflowTrigger getToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), NEW_AI_MODEL_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }

}
