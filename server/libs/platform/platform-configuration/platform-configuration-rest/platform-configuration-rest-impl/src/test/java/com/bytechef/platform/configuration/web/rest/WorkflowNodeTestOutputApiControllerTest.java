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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
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
 * Both the facade's and the service's own hasPermission(#workflowId, 'Workflow', ...) gates are environment-agnostic,
 * so the caller-supplied environmentId is never checked. These tests pin the execution side: for a confined (api-key)
 * principal, the environment reaching the downstream call must be the principal's own, not the request argument. This
 * is the REST twin of WorkflowNodeTestOutputGraphQlControllerTest -- reaches the same facade, but was missed by the
 * original review because it only enumerated GraphQL controllers.
 *
 * @author Ivica Cardic
 */
class WorkflowNodeTestOutputApiControllerTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private WorkflowNodeTestOutputApiController controller;
    private WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @BeforeEach
    void setUp() {
        ConversionService conversionService = mock(ConversionService.class);

        workflowNodeTestOutputFacade = mock(WorkflowNodeTestOutputFacade.class);
        workflowNodeTestOutputService = mock(WorkflowNodeTestOutputService.class);
        controller = new WorkflowNodeTestOutputApiController(
            conversionService, workflowNodeTestOutputFacade, workflowNodeTestOutputService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteWorkflowNodeTestOutputUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        controller.deleteWorkflowNodeTestOutput("workflow-1", "node-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputService).deleteWorkflowNodeTestOutput(
            eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testDeleteWorkflowNodeTestOutputHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        controller.deleteWorkflowNodeTestOutput("workflow-1", "node-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputService).deleteWorkflowNodeTestOutput(
            eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
    }

    @Test
    void testSaveWorkflowNodeTestOutputUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(anyString(), anyString(), anyLong()))
            .thenReturn(mock(WorkflowNodeTestOutput.class));

        controller.saveWorkflowNodeTestOutput("workflow-1", "node-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeTestOutput(
            eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testSaveWorkflowNodeTestOutputHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(anyString(), anyString(), anyLong()))
            .thenReturn(mock(WorkflowNodeTestOutput.class));

        controller.saveWorkflowNodeTestOutput("workflow-1", "node-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeTestOutput(
            eq("workflow-1"), eq("node-1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
    }

    @Test
    void testUploadWorkflowNodeSampleOutputUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowNodeTestOutputFacade.saveWorkflowNodeSampleOutput(
            anyString(), anyString(), any(), anyLong()))
                .thenReturn(mock(WorkflowNodeTestOutput.class));

        controller.uploadWorkflowNodeSampleOutput("workflow-1", "node-1", DEVELOPMENT_ORDINAL, "sample");

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeSampleOutput(
            eq("workflow-1"), eq("node-1"), eq("sample"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testUploadWorkflowNodeSampleOutputHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowNodeTestOutputFacade.saveWorkflowNodeSampleOutput(
            anyString(), anyString(), any(), anyLong()))
                .thenReturn(mock(WorkflowNodeTestOutput.class));

        controller.uploadWorkflowNodeSampleOutput("workflow-1", "node-1", DEVELOPMENT_ORDINAL, "sample");

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeSampleOutput(
            eq("workflow-1"), eq("node-1"), eq("sample"), environmentIdCaptor.capture());

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
