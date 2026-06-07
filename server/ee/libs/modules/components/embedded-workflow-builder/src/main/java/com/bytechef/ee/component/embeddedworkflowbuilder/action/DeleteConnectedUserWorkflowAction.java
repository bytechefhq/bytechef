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
public class DeleteConnectedUserWorkflowAction {

    public static final String WORKFLOW_UUID = "workflowUuid";

    @SuppressFBWarnings("EI2")
    private final ConnectedUserProjectFacade connectedUserProjectFacade;

    @SuppressFBWarnings("EI2")
    public static ModifiableActionDefinition of(ConnectedUserProjectFacade connectedUserProjectFacade) {
        return new DeleteConnectedUserWorkflowAction(connectedUserProjectFacade).build();
    }

    DeleteConnectedUserWorkflowAction(ConnectedUserProjectFacade connectedUserProjectFacade) {
        this.connectedUserProjectFacade = connectedUserProjectFacade;
    }

    private ModifiableActionDefinition build() {
        return action("deleteConnectedUserWorkflow")
            .title("Delete Workflow")
            .description("Delete a connected user's workflow. Returns a confirmation message.")
            .properties(
                string(WORKFLOW_UUID)
                    .label("Workflow UUID")
                    .description("The uuid of the workflow to delete.")
                    .required(true))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        String externalUserId = inputParameters.getRequiredString(EmbeddedToolConstants.EXTERNAL_USER_ID);
        Environment environment = EmbeddedWorkflowBuilderUtils.resolveEnvironment(
            inputParameters.getString(EmbeddedToolConstants.ENVIRONMENT));
        String workflowUuid = inputParameters.getRequiredString(WORKFLOW_UUID);

        connectedUserProjectFacade.deleteProjectWorkflow(externalUserId, workflowUuid, environment);

        return "Workflow '" + workflowUuid + "' has been successfully deleted.";
    }
}
