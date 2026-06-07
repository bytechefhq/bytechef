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
public class CreateConnectedUserWorkflowFromPromptAction {

    public static final String PROMPT = "prompt";

    private final ConnectedUserProjectFacade connectedUserProjectFacade;

    @SuppressFBWarnings("EI2")
    public static ModifiableActionDefinition of(ConnectedUserProjectFacade connectedUserProjectFacade) {
        return new CreateConnectedUserWorkflowFromPromptAction(connectedUserProjectFacade).build();
    }

    CreateConnectedUserWorkflowFromPromptAction(ConnectedUserProjectFacade connectedUserProjectFacade) {
        this.connectedUserProjectFacade = connectedUserProjectFacade;
    }

    private ModifiableActionDefinition build() {
        return action("createConnectedUserWorkflowFromPrompt")
            .title("Create Workflow From Prompt")
            .description(
                "Generate a new workflow for the connected user from a natural language prompt. "
                    + "Returns the new workflow uuid.")
            .properties(
                string(PROMPT)
                    .label("Prompt")
                    .description("Natural language description of the workflow to build.")
                    .required(true))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        String externalUserId = inputParameters.getRequiredString(EmbeddedToolConstants.EXTERNAL_USER_ID);
        Environment environment = EmbeddedWorkflowBuilderUtils.resolveEnvironment(
            inputParameters.getString(EmbeddedToolConstants.ENVIRONMENT));
        String prompt = inputParameters.getRequiredString(PROMPT);

        return connectedUserProjectFacade.createProjectWorkflow(externalUserId, prompt, environment, true);
    }
}
