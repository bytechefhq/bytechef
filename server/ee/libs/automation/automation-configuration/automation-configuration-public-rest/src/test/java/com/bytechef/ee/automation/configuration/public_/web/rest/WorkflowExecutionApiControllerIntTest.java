/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.configuration.service.WorkspaceApiKeyService;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import com.bytechef.web.rest.error.GlobalResponseEntityExceptionHandler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
class WorkflowExecutionApiControllerIntTest {

    private static final long API_KEY_ID = 1L;
    private static final long OTHER_WORKSPACE_ID = 200L;
    private static final long WORKSPACE_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @MockitoBean
    private WorkspaceApiKeyService workspaceApiKeyService;

    @BeforeEach
    void beforeEach() {
        when(workspaceApiKeyService.fetchApiKeyWorkspaceId(API_KEY_ID)).thenReturn(Optional.of(WORKSPACE_ID));
    }

    @Test
    void testGetWorkflowExecutionsPageOfAnotherWorkspaceIsForbidden() throws Exception {
        authenticate(PlatformType.AUTOMATION, Environment.PRODUCTION);

        mockMvc
            .perform(get("/v1/workflow-executions").param("workspaceId", String.valueOf(OTHER_WORKSPACE_ID)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(projectWorkflowExecutionFacade);
    }

    @Test
    void testGetWorkflowExecutionsPageIsScopedToTheApiKeyWorkspaceAndEnvironment() throws Exception {
        authenticate(PlatformType.AUTOMATION, Environment.STAGING);

        when(
            projectWorkflowExecutionFacade.getWorkspaceWorkflowExecutions(
                WORKSPACE_ID, Environment.STAGING.ordinal(), null, null, null, null, null, null, 0))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc
            .perform(
                get("/v1/workflow-executions")
                    .param("workspaceId", String.valueOf(WORKSPACE_ID))
                    .header("X-Environment", "production"))
            .andExpect(status().isOk());

        verify(projectWorkflowExecutionFacade).getWorkspaceWorkflowExecutions(
            WORKSPACE_ID, Environment.STAGING.ordinal(), null, null, null, null, null, null, 0);
    }

    @Test
    void testGetWorkflowExecutionsPageRejectsAnUnknownEnvironmentHeader() throws Exception {
        authenticate(PlatformType.AUTOMATION, Environment.PRODUCTION);

        mockMvc
            .perform(
                get("/v1/workflow-executions")
                    .param("workspaceId", String.valueOf(WORKSPACE_ID))
                    .header("X-Environment", "moon"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(projectWorkflowExecutionFacade);
    }

    @Test
    void testGetWorkflowExecutionIsScopedToTheApiKeyWorkspaceAndEnvironment() throws Exception {
        authenticate(PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        when(
            projectWorkflowExecutionFacade.getWorkspaceWorkflowExecution(
                WORKSPACE_ID, Environment.DEVELOPMENT.ordinal(), 7L))
                    .thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc
            .perform(get("/v1/workflow-executions/{id}", 7L))
            .andExpect(status().isForbidden());

        verify(projectWorkflowExecutionFacade).getWorkspaceWorkflowExecution(
            WORKSPACE_ID, Environment.DEVELOPMENT.ordinal(), 7L);
    }

    @Test
    void testGetWorkflowExecutionWithAnApiKeyWithoutAWorkspaceIsForbidden() throws Exception {
        authenticate(PlatformType.AUTOMATION, Environment.PRODUCTION);

        when(workspaceApiKeyService.fetchApiKeyWorkspaceId(API_KEY_ID)).thenReturn(Optional.empty());

        mockMvc
            .perform(get("/v1/workflow-executions/{id}", 7L))
            .andExpect(status().isForbidden());

        verifyNoInteractions(projectWorkflowExecutionFacade);
    }

    @Test
    void testGetWorkflowExecutionWithAnEmbeddedApiKeyIsForbidden() throws Exception {
        authenticate(PlatformType.EMBEDDED, Environment.PRODUCTION);

        mockMvc
            .perform(get("/v1/workflow-executions/{id}", 7L))
            .andExpect(status().isForbidden());

        verifyNoInteractions(projectWorkflowExecutionFacade, workspaceApiKeyService);
    }

    @Test
    @WithMockUser(username = "user@localhost.com")
    void testGetWorkflowExecutionsPageWithoutAnApiKeyIsForbidden() throws Exception {
        mockMvc
            .perform(get("/v1/workflow-executions").param("workspaceId", String.valueOf(WORKSPACE_ID)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(projectWorkflowExecutionFacade);
    }

    private static void authenticate(PlatformType platformType, Environment environment) {
        TestSecurityContextHolder.setAuthentication(
            new TestApiKeyAuthenticationToken(platformType, environment.ordinal()));
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private final PlatformType platformType;

        TestApiKeyAuthenticationToken(PlatformType platformType, long environmentId) {
            super(
                environmentId, new User("user@localhost.com", "", List.of(new SimpleGrantedAuthority("ROLE_USER"))),
                API_KEY_ID);

            this.platformType = platformType;
        }

        @Override
        public CredentialType getCredentialType() {
            return platformType == PlatformType.AUTOMATION
                ? CredentialType.AUTOMATION_API_KEY : CredentialType.EMBEDDED_API_KEY;
        }

        @Override
        public PlatformType getPlatformType() {
            return platformType;
        }
    }

    @Configuration
    @Import({
        GlobalResponseEntityExceptionHandler.class, JacksonConfiguration.class, WorkflowExecutionApiController.class
    })
    static class WorkflowExecutionApiControllerIntTestConfiguration {
    }
}
