/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.ai.tool.util.ToolPropertyUtils;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.util.SchemaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Derives the properties of a workflow that is exposed as an MCP tool: the tool name and description plus the input
 * properties declared by the workflow call trigger.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class McpToolWorkflowProperties {

    private McpToolWorkflowProperties() {
    }

    static List<Property> getToolProperties(Workflow workflow) {
        WorkflowTrigger trigger = getToolCallableTrigger(workflow);

        if (trigger == null) {
            return List.of();
        }

        List<Property> properties = new ArrayList<>();

        properties.add(ToolPropertyUtils.toolNameProperty("workflow"));

        properties.add(ToolPropertyUtils.toolDescriptionProperty("workflow"));

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

    private static WorkflowTrigger getToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.ofEnabled(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}
