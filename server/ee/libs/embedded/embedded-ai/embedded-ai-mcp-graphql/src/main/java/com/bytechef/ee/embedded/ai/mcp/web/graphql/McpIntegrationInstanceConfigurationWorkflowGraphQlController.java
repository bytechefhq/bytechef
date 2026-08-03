/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.platform.ai.tool.constant.ToolConstants.TOOL_NAME;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceConfigurationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpIntegrationInstanceConfigurationWorkflow} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class McpIntegrationInstanceConfigurationWorkflowGraphQlController {

    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;
    private final WorkflowService workflowService;

    McpIntegrationInstanceConfigurationWorkflowGraphQlController(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade,
        WorkflowService workflowService) {

        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceConfigurationWorkflowFacade = mcpIntegrationInstanceConfigurationWorkflowFacade;
        this.workflowService = workflowService;
    }

    @QueryMapping
    McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow(@Argument long id) {
        return mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflow(id);
    }

    @QueryMapping
    List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows() {
        return mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflows();
    }

    @QueryMapping
    List<McpIntegrationInstanceConfigurationWorkflow>
        mcpIntegrationInstanceConfigurationWorkflowsByMcpIntegrationInstanceConfigurationId(
            @Argument long mcpIntegrationInstanceConfigurationId) {
        return mcpIntegrationInstanceConfigurationWorkflowFacade
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfigurationId);
    }

    @QueryMapping
    List<IntegrationWorkflowDTO> toolEligibleIntegrationVersionWorkflows(
        @Argument long integrationId, @Argument int integrationVersion) {

        return mcpIntegrationInstanceConfigurationWorkflowFacade.getToolEligibleIntegrationVersionWorkflows(
            integrationId, integrationVersion);
    }

    @QueryMapping
    List<IntegrationWorkflowDTO> toolEligibleIntegrationInstanceConfigurationWorkflows(
        @Argument long integrationInstanceConfigurationId) {

        return mcpIntegrationInstanceConfigurationWorkflowFacade
            .getToolEligibleIntegrationInstanceConfigurationWorkflows(integrationInstanceConfigurationId);
    }

    @SuppressFBWarnings("BC_VACUOUS_INSTANCEOF")
    @QueryMapping
    List<Property> mcpIntegrationInstanceConfigurationWorkflowProperties(
        @Argument long mcpIntegrationInstanceConfigurationWorkflowId) {
        // Gate the read by fetching its subject through the tenant-admin-gated facade; the rest of this method only
        // builds UI Property metadata from the already-authorized workflow.
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflow(
                mcpIntegrationInstanceConfigurationWorkflowId);

        if (mcpIntegrationInstanceConfigurationWorkflow == null) {
            return List.of();
        }

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

        WorkflowTrigger trigger = getToolCallableTrigger(workflow);

        if (trigger == null) {
            return List.of();
        }

        List<Property> properties = new ArrayList<>();

        properties.add(
            Property.toProperty(
                string(TOOL_NAME)
                    .label("Name")
                    .description("The tool name exposed to the AI model.")
                    .expressionEnabled(false)
                    .required(true)));

        properties.add(
            Property.toProperty(
                string(TOOL_DESCRIPTION)
                    .label("Description")
                    .description("The tool description exposed to the AI model.")
                    .controlType(ControlType.TEXT_AREA)
                    .expressionEnabled(false)
                    .required(true)));

        String inputSchema = MapUtils.getString(trigger.getParameters(), WorkflowConstants.INPUT_SCHEMA);

        if (inputSchema == null || inputSchema.isEmpty()) {
            return properties;
        }

        BaseValueProperty<?> inputProperty = SchemaUtils.getJsonSchemaProperty(
            inputSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY);

        if (inputProperty == null) {
            return properties;
        }

        if (inputProperty instanceof com.bytechef.component.definition.Property.ObjectProperty objectProperty) {
            for (com.bytechef.component.definition.Property childProperty : objectProperty.getProperties()) {
                properties.add(Property.toProperty(childProperty));
            }

            return properties;
        }

        properties.add(Property.toProperty((com.bytechef.component.definition.Property) inputProperty));

        return properties;
    }

    @MutationMapping
    McpIntegrationInstanceConfigurationWorkflow
        createMcpIntegrationInstanceConfigurationWorkflow(@Argument("input") Map<String, Object> input) {
        Long mcpIntegrationInstanceConfigurationId =
            Long.valueOf(String.valueOf(input.get("mcpIntegrationInstanceConfigurationId")));
        Long integrationInstanceConfigurationWorkflowId = Long.valueOf(
            String.valueOf(input.get("integrationInstanceConfigurationWorkflowId")));

        return mcpIntegrationInstanceConfigurationWorkflowFacade.createMcpIntegrationInstanceConfigurationWorkflow(
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);
    }

    @SuppressWarnings("unchecked")
    @MutationMapping
    McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflow(
        @Argument("id") long id, @Argument("input") Map<String, Object> input) {

        Long mcpIntegrationInstanceConfigurationId = null;

        if (input.containsKey("mcpIntegrationInstanceConfigurationId")) {
            mcpIntegrationInstanceConfigurationId =
                Long.valueOf(String.valueOf(input.get("mcpIntegrationInstanceConfigurationId")));
        }

        Long integrationInstanceConfigurationWorkflowId = null;

        if (input.containsKey("integrationInstanceConfigurationWorkflowId")) {
            integrationInstanceConfigurationWorkflowId = Long.valueOf(
                String.valueOf(input.get("integrationInstanceConfigurationWorkflowId")));
        }

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflowFacade.updateMcpIntegrationInstanceConfigurationWorkflow(
                id, mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);

        if (input.containsKey("parameters")) {
            Object parametersObject = input.get("parameters");

            if (parametersObject != null && !(parametersObject instanceof Map)) {
                throw new IllegalArgumentException(
                    "Expected parameters to be a Map, but got: " + parametersObject.getClass()
                        .getName());
            }

            Map<String, ?> parameters = parametersObject != null ? (Map<String, ?>) parametersObject : Map.of();

            mcpIntegrationInstanceConfigurationWorkflow = mcpIntegrationInstanceConfigurationWorkflowFacade
                .updateMcpIntegrationInstanceConfigurationWorkflowParameters(id, parameters);
        }

        return mcpIntegrationInstanceConfigurationWorkflow;
    }

    @MutationMapping
    boolean deleteMcpIntegrationInstanceConfigurationWorkflow(@Argument("id") long id) {
        mcpIntegrationInstanceConfigurationWorkflowFacade.deleteMcpIntegrationInstanceConfigurationWorkflow(id);

        return true;
    }

    @SchemaMapping
    IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow(
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow) {

        return integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
            mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
    }

    @SchemaMapping
    WorkflowDTO workflow(McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

        return new WorkflowDTO(workflow, List.of(), List.of());
    }

    private static WorkflowTrigger getToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }

}
