/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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

    @Test
    void testIsConnectedUser() {
        assertThat(embeddedCopilotAccessChecker.isConnectedUser(new ConnectedUserAuthenticationToken())).isTrue();
        assertThat(
            embeddedCopilotAccessChecker.isConnectedUser(
                new UsernamePasswordAuthenticationToken("someone@localhost.com", null, List.of())))
                    .isFalse();
        assertThat(embeddedCopilotAccessChecker.isConnectedUser(null)).isFalse();
    }

    private static final class ConnectedUserAuthenticationToken extends UsernamePasswordAuthenticationToken
        implements ConnectedUserAuthentication {

        private ConnectedUserAuthenticationToken() {
            super("someone@localhost.com", "", List.of());
        }

        @Override
        public long connectedUserId() {
            return 1L;
        }

        @Override
        public String externalUserId() {
            return (String) getPrincipal();
        }

        @Override
        public long environmentId() {
            return 0L;
        }
    }
}
