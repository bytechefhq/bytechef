/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.web.graphql;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationWorkflowService;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.util.SchemaUtils;
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
 * GraphQL controller for managing {@link McpIntegrationWorkflow} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnCoordinator
class McpIntegrationWorkflowGraphQlController {

    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationWorkflowService mcpIntegrationWorkflowService;
    private final WorkflowService workflowService;

    McpIntegrationWorkflowGraphQlController(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationWorkflowService mcpIntegrationWorkflowService, WorkflowService workflowService) {

        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationWorkflowService = mcpIntegrationWorkflowService;
        this.workflowService = workflowService;
    }

    @QueryMapping
    McpIntegrationWorkflow mcpIntegrationWorkflow(@Argument long id) {
        return mcpIntegrationWorkflowService.fetchMcpIntegrationWorkflow(id)
            .orElse(null);
    }

    @QueryMapping
    List<McpIntegrationWorkflow> mcpIntegrationWorkflows() {
        return mcpIntegrationWorkflowService.getMcpIntegrationWorkflows();
    }

    @QueryMapping
    List<McpIntegrationWorkflow> mcpIntegrationWorkflowsByMcpIntegrationId(@Argument long mcpIntegrationId) {
        return mcpIntegrationWorkflowService.getMcpIntegrationMcpIntegrationWorkflows(mcpIntegrationId);
    }

    @QueryMapping
    List<IntegrationWorkflowDTO> toolEligibleIntegrationVersionWorkflows(
        @Argument long integrationId, @Argument int integrationVersion) {

        return integrationWorkflowService.getIntegrationWorkflows(integrationId, integrationVersion)
            .stream()
            .filter(integrationWorkflow -> hasToolCallableTrigger(integrationWorkflow.getWorkflowId()))
            .map(integrationWorkflow -> {
                Workflow workflow = workflowService.getWorkflow(integrationWorkflow.getWorkflowId());

                return new IntegrationWorkflowDTO(workflow, integrationWorkflow);
            })
            .toList();
    }

    @QueryMapping
    List<Property> mcpIntegrationWorkflowProperties(@Argument long mcpIntegrationWorkflowId) {
        McpIntegrationWorkflow mcpIntegrationWorkflow =
            mcpIntegrationWorkflowService.fetchMcpIntegrationWorkflow(mcpIntegrationWorkflowId)
                .orElse(null);

        if (mcpIntegrationWorkflow == null) {
            return List.of();
        }

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

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
    McpIntegrationWorkflow createMcpIntegrationWorkflow(@Argument("input") Map<String, Object> input) {
        Long mcpIntegrationId = Long.valueOf(String.valueOf(input.get("mcpIntegrationId")));
        Long integrationInstanceConfigurationWorkflowId = Long.valueOf(
            String.valueOf(input.get("integrationInstanceConfigurationWorkflowId")));

        return mcpIntegrationWorkflowService.create(mcpIntegrationId, integrationInstanceConfigurationWorkflowId);
    }

    @SuppressWarnings("unchecked")
    @MutationMapping
    McpIntegrationWorkflow updateMcpIntegrationWorkflow(
        @Argument("id") long id, @Argument("input") Map<String, Object> input) {

        Long mcpIntegrationId = null;

        if (input.containsKey("mcpIntegrationId")) {
            mcpIntegrationId = Long.valueOf(String.valueOf(input.get("mcpIntegrationId")));
        }

        Long integrationInstanceConfigurationWorkflowId = null;

        if (input.containsKey("integrationInstanceConfigurationWorkflowId")) {
            integrationInstanceConfigurationWorkflowId = Long.valueOf(
                String.valueOf(input.get("integrationInstanceConfigurationWorkflowId")));
        }

        McpIntegrationWorkflow mcpIntegrationWorkflow = mcpIntegrationWorkflowService.update(
            id, mcpIntegrationId, integrationInstanceConfigurationWorkflowId);

        if (input.containsKey("parameters")) {
            Object parametersObject = input.get("parameters");

            Map<String, ?> parameters = parametersObject instanceof Map
                ? (Map<String, ?>) parametersObject : Map.of();

            mcpIntegrationWorkflow = mcpIntegrationWorkflowService.updateParameters(id, parameters);
        }

        return mcpIntegrationWorkflow;
    }

    @MutationMapping
    boolean deleteMcpIntegrationWorkflow(@Argument("id") long id) {
        mcpIntegrationWorkflowService.delete(id);

        return true;
    }

    @SchemaMapping
    IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow(
        McpIntegrationWorkflow mcpIntegrationWorkflow) {

        return integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
            mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
    }

    @SchemaMapping
    WorkflowDTO workflow(McpIntegrationWorkflow mcpIntegrationWorkflow) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

        return new WorkflowDTO(workflow, List.of(), List.of());
    }

    private static WorkflowTrigger getToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.TOOL_CALLABLE)) {

                return workflowTrigger;
            }
        }

        return null;
    }

    private boolean hasToolCallableTrigger(String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return getToolCallableTrigger(workflow) != null;
    }
}
