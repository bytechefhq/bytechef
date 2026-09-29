/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.PublishConnectedUserProjectWorkflowRequestModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserProjectWorkflowApiControllerTest {

    private static final String EXTERNAL_USER_ID = "user@localhost.com";
    private static final String WORKFLOW_UUID = "workflow-uuid";

    private final ConnectedUserProjectFacade connectedUserProjectFacade = mock(ConnectedUserProjectFacade.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);

    private final ConnectedUserProjectWorkflowApiController connectedUserProjectWorkflowApiController =
        new ConnectedUserProjectWorkflowApiController(
            connectedUserProjectFacade, mock(ConversionService.class), environmentService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAPlatformUserWhoseLoginIsAnExternalIdIsRefused() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(EXTERNAL_USER_ID, "", List.of()));

        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);

        PublishConnectedUserProjectWorkflowRequestModel publishConnectedUserProjectWorkflowRequestModel =
            new PublishConnectedUserProjectWorkflowRequestModel();

        assertThatThrownBy(
            () -> connectedUserProjectWorkflowApiController.enableConnectedUserProjectWorkflow(
                WORKFLOW_UUID, true, null))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserProjectWorkflowApiController.getConnectedUserProjectWorkflow(WORKFLOW_UUID, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserProjectWorkflowApiController.publishConnectedUserProjectWorkflow(
                WORKFLOW_UUID, publishConnectedUserProjectWorkflowRequestModel, null))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(connectedUserProjectFacade);
    }

    @Test
    void testAConnectedUserActsAsThemselves() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of(EXTERNAL_USER_ID));

        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);

        connectedUserProjectWorkflowApiController.enableConnectedUserProjectWorkflow(WORKFLOW_UUID, true, null);

        verify(connectedUserProjectFacade).enableProjectWorkflow(
            EXTERNAL_USER_ID, WORKFLOW_UUID, true, (long) Environment.PRODUCTION.ordinal());
    }
}
