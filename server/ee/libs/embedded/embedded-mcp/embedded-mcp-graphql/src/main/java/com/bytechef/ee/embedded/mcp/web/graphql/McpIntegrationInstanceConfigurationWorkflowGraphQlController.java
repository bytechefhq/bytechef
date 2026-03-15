/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.web.graphql;

import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.facade.McpIntegrationInstanceConfigurationWorkflowFacade;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
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
@ConditionalOnCoordinator
class McpIntegrationInstanceConfigurationWorkflowGraphQlController {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final WorkflowService workflowService;

    McpIntegrationInstanceConfigurationWorkflowGraphQlController(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        WorkflowService workflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationInstanceConfigurationWorkflowFacade = mcpIntegrationInstanceConfigurationWorkflowFacade;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.workflowService = workflowService;
    }

    @QueryMapping
    McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow(@Argument long id) {
        return mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(id)
            .orElse(null);
    }

    @QueryMapping
    List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows() {
        return mcpIntegrationInstanceConfigurationWorkflowService.getMcpIntegrationInstanceConfigurationWorkflows();
    }

    @QueryMapping
    List<McpIntegrationInstanceConfigurationWorkflow>
        mcpIntegrationInstanceConfigurationWorkflowsByMcpIntegrationInstanceConfigurationId(
            @Argument long mcpIntegrationInstanceConfigurationId) {
        return mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfigurationId);
    }

    @QueryMapping
    List<IntegrationWorkflowDTO> toolEligibleIntegrationVersionWorkflows(
        @Argument long integrationId, @Argument int integrationVersion) {

        return integrationWorkflowService.getIntegrationWorkflows(integrationId, integrationVersion)
            .stream()
            .map(integrationWorkflow -> {
                Workflow workflow = workflowService.getWorkflow(integrationWorkflow.getWorkflowId());

                return new IntegrationWorkflowDTO(workflow, integrationWorkflow);
            })
            .filter(integrationWorkflowDTO -> getToolCallableTrigger(integrationWorkflowDTO.getWorkflow()) != null)
            .toList();
    }

    @QueryMapping
    List<IntegrationWorkflowDTO> toolEligibleIntegrationInstanceConfigurationWorkflows(
        @Argument long integrationInstanceConfigurationId) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        return integrationWorkflowService.getIntegrationWorkflows(
            integrationInstanceConfiguration.getIntegrationId(),
            integrationInstanceConfiguration.getIntegrationVersion())
            .stream()
            .map(integrationWorkflow -> {
                Workflow workflow = workflowService.getWorkflow(integrationWorkflow.getWorkflowId());

                return new IntegrationWorkflowDTO(workflow, integrationWorkflow);
            })
            .filter(integrationWorkflowDTO -> getToolCallableTrigger(integrationWorkflowDTO.getWorkflow()) != null)
            .toList();
    }

    @SuppressFBWarnings("BC_VACUOUS_INSTANCEOF")
    @QueryMapping
    List<Property> mcpIntegrationInstanceConfigurationWorkflowProperties(
        @Argument long mcpIntegrationInstanceConfigurationWorkflowId) {
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflowService
                .fetchMcpIntegrationInstanceConfigurationWorkflow(mcpIntegrationInstanceConfigurationWorkflowId)
                .orElse(null);

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
            for (com.bytechef.component.definition.Property childProperty : objectProperty.getProperties()
                .orElse(List.of())) {

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

        return mcpIntegrationInstanceConfigurationWorkflowService.create(mcpIntegrationInstanceConfigurationId,
            integrationInstanceConfigurationWorkflowId);
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
            mcpIntegrationInstanceConfigurationWorkflowService.update(
                id, mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);

        if (input.containsKey("parameters")) {
            Object parametersObject = input.get("parameters");

            if (parametersObject != null && !(parametersObject instanceof Map)) {
                throw new IllegalArgumentException(
                    "Expected parameters to be a Map, but got: " + parametersObject.getClass()
                        .getName());
            }

            Map<String, ?> parameters = parametersObject != null ? (Map<String, ?>) parametersObject : Map.of();

            mcpIntegrationInstanceConfigurationWorkflow =
                mcpIntegrationInstanceConfigurationWorkflowService.updateParameters(id, parameters);
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
