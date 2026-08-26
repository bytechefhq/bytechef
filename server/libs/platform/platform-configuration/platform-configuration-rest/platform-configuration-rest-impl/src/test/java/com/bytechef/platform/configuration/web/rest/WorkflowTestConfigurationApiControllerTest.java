/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * getWorkflowTestConfiguration and getWorkflowTestConfigurationConnections previously called
 * WorkflowTestConfigurationService directly -- no @PreAuthorize at all, so any authenticated caller could read any
 * workflow's test configuration or connections. Now routed through the facade so a gate exists; these tests cover the
 * environment half of that fix, which the facade's own hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW') gate
 * does not check.
 *
 * @author Ivica Cardic
 */
class WorkflowTestConfigurationApiControllerTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private WorkflowTestConfigurationApiController controller;
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @BeforeEach
    void setUp() {
        ConversionService conversionService = mock(ConversionService.class);

        workflowTestConfigurationFacade = mock(WorkflowTestConfigurationFacade.class);
        controller = new WorkflowTestConfigurationApiController(workflowTestConfigurationFacade, conversionService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetWorkflowTestConfigurationUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowTestConfigurationFacade.fetchWorkflowTestConfiguration(anyString(), anyLong()))
            .thenReturn(Optional.empty());

        controller.getWorkflowTestConfiguration("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationFacade).fetchWorkflowTestConfiguration(
            eq("workflow-1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testGetWorkflowTestConfigurationHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowTestConfigurationFacade.fetchWorkflowTestConfiguration(anyString(), anyLong()))
            .thenReturn(Optional.empty());

        controller.getWorkflowTestConfiguration("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationFacade).fetchWorkflowTestConfiguration(
            eq("workflow-1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
    }

    @Test
    void testGetWorkflowTestConfigurationConnectionsUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowTestConfigurationFacade.getWorkflowTestConfigurationConnections(
            anyString(), anyString(), anyLong())).thenReturn(List.of());

        controller.getWorkflowTestConfigurationConnections("workflow-1", "node-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationFacade).getWorkflowTestConfigurationConnections(
            eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testGetWorkflowTestConfigurationConnectionsHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowTestConfigurationFacade.getWorkflowTestConfigurationConnections(
            anyString(), anyString(), anyLong())).thenReturn(List.of());

        controller.getWorkflowTestConfigurationConnections("workflow-1", "node-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationFacade).getWorkflowTestConfigurationConnections(
            eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
