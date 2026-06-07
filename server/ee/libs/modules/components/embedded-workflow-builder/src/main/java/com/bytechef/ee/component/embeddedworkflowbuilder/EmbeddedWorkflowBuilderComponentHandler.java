/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.embeddedworkflowbuilder;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.embeddedworkflowbuilder.action.CreateConnectedUserWorkflowFromPromptAction;
import com.bytechef.ee.component.embeddedworkflowbuilder.action.DeleteConnectedUserWorkflowAction;
import com.bytechef.ee.component.embeddedworkflowbuilder.action.UpdateConnectedUserWorkflowAction;
import com.bytechef.ee.component.embeddedworkflowbuilder.action.UpdateConnectedUserWorkflowFromPromptAction;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("embeddedWorkflowBuilder_v1_ComponentHandler")
@ConditionalOnEEVersion
public class EmbeddedWorkflowBuilderComponentHandler implements ComponentHandler {

    public static final String EMBEDDED_WORKFLOW_BUILDER = "embeddedWorkflowBuilder";

    private final ComponentDefinition componentDefinition;

    @SuppressFBWarnings("EI2")
    public EmbeddedWorkflowBuilderComponentHandler(ConnectedUserProjectFacade connectedUserProjectFacade) {
        ActionDefinition createConnectedUserWorkflowFromPromptAction = CreateConnectedUserWorkflowFromPromptAction.of(
            connectedUserProjectFacade);
        ActionDefinition updateConnectedUserWorkflowFromPromptAction = UpdateConnectedUserWorkflowFromPromptAction.of(
            connectedUserProjectFacade);
        ActionDefinition updateConnectedUserWorkflowAction = UpdateConnectedUserWorkflowAction.of(
            connectedUserProjectFacade);
        ActionDefinition deleteConnectedUserWorkflowAction = DeleteConnectedUserWorkflowAction.of(
            connectedUserProjectFacade);

        this.componentDefinition = component(EMBEDDED_WORKFLOW_BUILDER)
            .title("Embedded Workflow Builder")
            .description("Create, update, and delete an embedded connected user's workflows via AI Copilot.")
            .icon("path:assets/embedded-workflow-builder.svg")
            .categories(ComponentCategory.HELPERS)
            .actions(
                createConnectedUserWorkflowFromPromptAction,
                updateConnectedUserWorkflowFromPromptAction,
                updateConnectedUserWorkflowAction,
                deleteConnectedUserWorkflowAction)
            .clusterElements(
                tool(createConnectedUserWorkflowFromPromptAction),
                tool(updateConnectedUserWorkflowFromPromptAction),
                tool(updateConnectedUserWorkflowAction),
                tool(deleteConnectedUserWorkflowAction))
            .version(1);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
