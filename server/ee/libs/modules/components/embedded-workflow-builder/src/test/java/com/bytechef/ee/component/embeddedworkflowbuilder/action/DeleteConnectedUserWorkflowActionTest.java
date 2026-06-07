/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.embeddedworkflowbuilder.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
class DeleteConnectedUserWorkflowActionTest {

    @Test
    void testPerformDeletesWorkflow() {
        ConnectedUserProjectFacade facade = mock(ConnectedUserProjectFacade.class);

        Parameters inputParameters = mock(Parameters.class);

        when(inputParameters.getRequiredString(EmbeddedToolConstants.EXTERNAL_USER_ID)).thenReturn("user-1");
        when(inputParameters.getString(EmbeddedToolConstants.ENVIRONMENT)).thenReturn("PRODUCTION");
        when(inputParameters.getRequiredString("workflowUuid")).thenReturn("wf-1");

        Object result = new DeleteConnectedUserWorkflowAction(facade)
            .perform(inputParameters, mock(Parameters.class), mock(ActionContext.class));

        verify(facade).deleteProjectWorkflow("user-1", "wf-1", Environment.PRODUCTION);
        assertEquals("Workflow 'wf-1' has been successfully deleted.", result);
    }
}
