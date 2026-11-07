/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import com.bytechef.web.rest.error.GlobalResponseEntityExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(
    classes = WorkflowExecutionApiControllerIntTest.WorkflowExecutionApiControllerIntTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(WorkflowExecutionApiController.class)
@WithMockUser(username = WorkflowExecutionApiControllerIntTest.EXTERNAL_USER_ID)
class WorkflowExecutionApiControllerIntTest {

    static final String EXTERNAL_USER_ID = "external-user-1";

    private static final String OTHER_EXTERNAL_USER_ID = "external-user-2";

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetWorkflowExecutionsPageOfAnotherConnectedUserIsForbidden() throws Exception {
        mockMvc
            .perform(get("/v1/{externalUserId}/workflow-executions", OTHER_EXTERNAL_USER_ID))
            .andExpect(status().isForbidden());

        verifyNoInteractions(integrationWorkflowExecutionFacade);
    }

    @Test
    void testGetWorkflowExecutionOfAnotherConnectedUserIsForbidden() throws Exception {
        mockMvc
            .perform(get("/v1/{externalUserId}/workflow-executions/{id}", OTHER_EXTERNAL_USER_ID, 7L))
            .andExpect(status().isForbidden());

        verifyNoInteractions(integrationWorkflowExecutionFacade);
    }

    @Test
    void testGetWorkflowExecutionOwnedByAnotherConnectedUserIsForbidden() throws Exception {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecution(EXTERNAL_USER_ID,
            Environment.PRODUCTION.ordinal(), 7L))
                .thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc
            .perform(get("/v1/{externalUserId}/workflow-executions/{id}", EXTERNAL_USER_ID, 7L))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetWorkflowExecutionsPageAcceptsAnyCaseOfTheEnvironmentHeader() throws Exception {
        when(environmentService.getEnvironment("DEVELOPMENT")).thenReturn(Environment.DEVELOPMENT);
        when(
            integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecutions(
                eq(EXTERNAL_USER_ID), eq((long) Environment.DEVELOPMENT.ordinal()), isNull(), isNull(), isNull(),
                isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc
            .perform(
                get("/v1/{externalUserId}/workflow-executions", EXTERNAL_USER_ID)
                    .header("X-Environment", "development"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetWorkflowExecutionsPageUsesTheEnvironmentTheApiKeyAuthenticatedFor() throws Exception {
        when(
            integrationWorkflowExecutionFacade.getConnectedUserWorkflowExecutions(
                eq(EXTERNAL_USER_ID), anyLong(), isNull(), isNull(), isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        TestSecurityContextHolder.setAuthentication(new TestApiKeyAuthenticationToken(Environment.STAGING.ordinal()));

        mockMvc
            .perform(
                get("/v1/{externalUserId}/workflow-executions", EXTERNAL_USER_ID)
                    .header("X-Environment", "PRODUCTION"))
            .andExpect(status().isOk());

        verify(integrationWorkflowExecutionFacade).getConnectedUserWorkflowExecutions(
            eq(EXTERNAL_USER_ID), eq((long) Environment.STAGING.ordinal()), isNull(), isNull(), isNull(), isNull(),
            eq(0));
        verifyNoInteractions(environmentService);
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        TestApiKeyAuthenticationToken(long environmentId) {
            super(
                environmentId,
                new User(EXTERNAL_USER_ID, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))), 1L);
        }

        @Override
        public CredentialType getCredentialType() {
            return CredentialType.EMBEDDED_API_KEY;
        }

        @Override
        public PlatformType getPlatformType() {
            return PlatformType.EMBEDDED;
        }
    }

    @Configuration
    @Import({
        GlobalResponseEntityExceptionHandler.class, JacksonConfiguration.class, WorkflowExecutionApiController.class
    })
    static class WorkflowExecutionApiControllerIntTestConfiguration {
    }
}
