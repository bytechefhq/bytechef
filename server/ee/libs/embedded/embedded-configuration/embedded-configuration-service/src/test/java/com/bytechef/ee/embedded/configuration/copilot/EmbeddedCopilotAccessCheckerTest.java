/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.copilot;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedCopilotAccessCheckerTest {

    private final IntegrationWorkflowFacade integrationWorkflowFacade = mock(IntegrationWorkflowFacade.class);
    private final EmbeddedCopilotAccessChecker embeddedCopilotAccessChecker =
        new EmbeddedCopilotAccessChecker(integrationWorkflowFacade);

    @Test
    void testCheckIntegrationWorkflowAccessReadsTheWorkflowThroughTheFacade() {
        embeddedCopilotAccessChecker.checkIntegrationWorkflowAccess("wf1");

        verify(integrationWorkflowFacade).getIntegrationWorkflow("wf1");
    }

    @Test
    void testCheckIntegrationWorkflowAccessPropagatesADenial() {
        when(integrationWorkflowFacade.getIntegrationWorkflow("wf1")).thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> embeddedCopilotAccessChecker.checkIntegrationWorkflowAccess("wf1"))
            .isInstanceOf(AccessDeniedException.class);
    }
}
