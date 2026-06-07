/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.embeddedworkflowbuilder.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.execution.constant.EmbeddedToolConstants;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class UpdateConnectedUserWorkflowFromPromptActionTest {

    @Test
    void testPerformUpdatesWorkflowFromPrompt() {
        ConnectedUserProjectFacade facade = mock(ConnectedUserProjectFacade.class);

        when(facade.updateProjectWorkflow("user-1", "wf-1", "add a step", Environment.PRODUCTION, true))
            .thenReturn("wf-1");

        Parameters inputParameters = mock(Parameters.class);

        when(inputParameters.getRequiredString(EmbeddedToolConstants.EXTERNAL_USER_ID)).thenReturn("user-1");
        when(inputParameters.getString(EmbeddedToolConstants.ENVIRONMENT)).thenReturn("PRODUCTION");
        when(inputParameters.getRequiredString("workflowUuid")).thenReturn("wf-1");
        when(inputParameters.getRequiredString("prompt")).thenReturn("add a step");

        Object result = new UpdateConnectedUserWorkflowFromPromptAction(facade)
            .perform(inputParameters, mock(Parameters.class), mock(ActionContext.class));

        assertEquals("wf-1", result);
    }
}
