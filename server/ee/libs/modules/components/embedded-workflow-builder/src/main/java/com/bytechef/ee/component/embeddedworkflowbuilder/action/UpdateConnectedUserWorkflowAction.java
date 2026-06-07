/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.embeddedworkflowbuilder.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.component.embeddedworkflowbuilder.util.EmbeddedWorkflowBuilderUtils;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.execution.constant.EmbeddedToolConstants;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class UpdateConnectedUserWorkflowAction {

    public static final String DEFINITION = "definition";
    public static final String WORKFLOW_UUID = "workflowUuid";

    @SuppressFBWarnings("EI2")
    private final ConnectedUserProjectFacade connectedUserProjectFacade;

    @SuppressFBWarnings("EI2")
    public static ModifiableActionDefinition of(ConnectedUserProjectFacade connectedUserProjectFacade) {
        return new UpdateConnectedUserWorkflowAction(connectedUserProjectFacade).build();
    }

    UpdateConnectedUserWorkflowAction(ConnectedUserProjectFacade connectedUserProjectFacade) {
        this.connectedUserProjectFacade = connectedUserProjectFacade;
    }

    private ModifiableActionDefinition build() {
        return action("updateConnectedUserWorkflow")
            .title("Update Workflow Definition")
            .description("Replace the JSON definition of a connected user's workflow. Returns a confirmation message.")
            .properties(
                string(WORKFLOW_UUID)
                    .label("Workflow UUID")
                    .description("The uuid of the workflow to update.")
                    .required(true),
                string(DEFINITION)
                    .label("Definition")
                    .description("The new workflow definition in JSON format.")
                    .required(true))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        String externalUserId = inputParameters.getRequiredString(EmbeddedToolConstants.EXTERNAL_USER_ID);
        Environment environment = EmbeddedWorkflowBuilderUtils.resolveEnvironment(
            inputParameters.getString(EmbeddedToolConstants.ENVIRONMENT));
        String workflowUuid = inputParameters.getRequiredString(WORKFLOW_UUID);
        String definition = inputParameters.getRequiredString(DEFINITION);

        connectedUserProjectFacade.updateProjectWorkflow(externalUserId, workflowUuid, definition, environment);

        return "Workflow '" + workflowUuid + "' has been successfully updated.";
    }
}
