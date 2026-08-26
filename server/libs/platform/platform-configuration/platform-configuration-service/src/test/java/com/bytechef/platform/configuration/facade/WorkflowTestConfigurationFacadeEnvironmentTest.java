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

package com.bytechef.platform.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Covers the facade methods reached ONLY through WorkflowTestConfigurationApiController (REST) --
 * deleteWorkflowTestConfigurationConnection, saveWorkflowTestConfigurationInputs, fetchWorkflowTestConfiguration,
 * getWorkflowTestConfigurationConnections -- plus saveWorkflowTestConfiguration, whose ordinal is carried in the
 * request body rather than a parameter. hasPermission(...) is environment-agnostic on all of them, so the
 * caller-supplied environmentId is never checked by the gate.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkflowTestConfigurationFacadeEnvironmentTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private static final Workflow WORKFLOW_NO_REQUIRED_INPUTS = new Workflow("workflow-1", "{}", Format.JSON);

    @Mock
    private ComponentConnectionFacade componentConnectionFacade;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WorkflowTestConfigurationFacadeImpl workflowTestConfigurationFacade;

    @BeforeEach
    void setUp() {
        workflowTestConfigurationFacade = new WorkflowTestConfigurationFacadeImpl(
            connectionService, componentConnectionFacade, workflowService, workflowTestConfigurationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteWorkflowTestConfigurationConnectionUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        workflowTestConfigurationFacade.deleteWorkflowTestConfigurationConnection(
            "workflow-1", "node-1", "connectionKey", 5L, DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationService).deleteWorkflowTestConfigurationConnection(
            eq("workflow-1"), eq("node-1"), eq("connectionKey"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testDeleteWorkflowTestConfigurationConnectionHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        workflowTestConfigurationFacade.deleteWorkflowTestConfigurationConnection(
            "workflow-1", "node-1", "connectionKey", 5L, DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationService).deleteWorkflowTestConfigurationConnection(
            eq("workflow-1"), eq("node-1"), eq("connectionKey"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testSaveWorkflowTestConfigurationInputsUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationInputs(
            "workflow-1", "key", "value", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationInputs(
            eq("workflow-1"), eq("key"), eq("value"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testSaveWorkflowTestConfigurationInputsHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationInputs(
            "workflow-1", "key", "value", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationInputs(
            eq("workflow-1"), eq("key"), eq("value"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    /**
     * The critical case: environmentId is not a parameter here, it's WorkflowTestConfiguration.environmentId, carried
     * in the request body. Asserts the value reaching save() -- not just the field on the object this test builds,
     * since the whole point of the fix is that the facade overwrites it before save() sees it.
     */
    @Test
    void testSaveWorkflowTestConfigurationUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_NO_REQUIRED_INPUTS);
        when(workflowTestConfigurationService.saveWorkflowTestConfiguration(any(WorkflowTestConfiguration.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowTestConfiguration requested = new WorkflowTestConfiguration(
            DEVELOPMENT_ORDINAL, Map.of(), "workflow-1", List.of());

        workflowTestConfigurationFacade.saveWorkflowTestConfiguration(requested);

        ArgumentCaptor<WorkflowTestConfiguration> savedCaptor =
            ArgumentCaptor.forClass(WorkflowTestConfiguration.class);

        verify(workflowTestConfigurationService).saveWorkflowTestConfiguration(savedCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, savedCaptor.getValue()
            .getEnvironmentId());
    }

    @Test
    void testSaveWorkflowTestConfigurationHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_NO_REQUIRED_INPUTS);
        when(workflowTestConfigurationService.saveWorkflowTestConfiguration(any(WorkflowTestConfiguration.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowTestConfiguration requested = new WorkflowTestConfiguration(
            DEVELOPMENT_ORDINAL, Map.of(), "workflow-1", List.of());

        workflowTestConfigurationFacade.saveWorkflowTestConfiguration(requested);

        ArgumentCaptor<WorkflowTestConfiguration> savedCaptor =
            ArgumentCaptor.forClass(WorkflowTestConfiguration.class);

        verify(workflowTestConfigurationService).saveWorkflowTestConfiguration(savedCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, savedCaptor.getValue()
            .getEnvironmentId());
    }

    /**
     * fetchWorkflowTestConfiguration and getWorkflowTestConfigurationConnections previously had no gate at all --
     * WorkflowTestConfigurationApiController called WorkflowTestConfigurationService directly. Now routed through this
     * facade so a gate exists; these tests cover the environment half of that fix.
     */
    @Test
    void testFetchWorkflowTestConfigurationUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(anyString(), anyLong()))
            .thenReturn(Optional.empty());

        workflowTestConfigurationFacade.fetchWorkflowTestConfiguration("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationService).fetchWorkflowTestConfiguration(
            eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testFetchWorkflowTestConfigurationHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(anyString(), anyLong()))
            .thenReturn(Optional.empty());

        workflowTestConfigurationFacade.fetchWorkflowTestConfiguration("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowTestConfigurationService).fetchWorkflowTestConfiguration(
            eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
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
