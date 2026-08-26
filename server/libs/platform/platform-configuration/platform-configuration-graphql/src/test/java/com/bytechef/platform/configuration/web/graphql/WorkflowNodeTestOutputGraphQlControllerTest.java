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

package com.bytechef.platform.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * The facade's own {@code hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')} gate is environment-agnostic, so the
 * caller-supplied {@code environmentId} argument is never checked. These tests pin the execution side: for a confined
 * (api-key) principal, the environment reaching the downstream facade call must be the principal's own, not the request
 * argument.
 *
 * @author Ivica Cardic
 */
class WorkflowNodeTestOutputGraphQlControllerTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;
    private WorkflowNodeTestOutputGraphQlController controller;

    @BeforeEach
    void setUp() {
        workflowNodeTestOutputFacade = mock(WorkflowNodeTestOutputFacade.class);
        controller = new WorkflowNodeTestOutputGraphQlController(workflowNodeTestOutputFacade);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveClusterElementTestOutputUsesConfinedPrincipalEnvironmentAtExecutionNotTheRequestedOne() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowNodeTestOutputFacade.saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(mock(WorkflowNodeTestOutput.class));

        // The connected-user token belongs to PRODUCTION; the request asks for DEVELOPMENT.
        controller.saveClusterElementTestOutput(
            "workflow-1", "node-1", "PROCESSOR", "script_1", DEVELOPMENT_ORDINAL, null);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveClusterElementTestOutput(
            eq("workflow-1"), eq("node-1"), eq("PROCESSOR"), eq("script_1"), environmentIdCaptor.capture());

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testSaveClusterElementTestOutputHonoursSessionPrincipalRequestedEnvironmentAtExecution() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowNodeTestOutputFacade.saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(mock(WorkflowNodeTestOutput.class));

        // A session principal has no environment of its own -- the requested DEVELOPMENT must reach execution
        // unchanged. This is the containment half: an ordinary platform user must not be confined by the fix above.
        controller.saveClusterElementTestOutput(
            "workflow-1", "node-1", "PROCESSOR", "script_1", DEVELOPMENT_ORDINAL, null);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveClusterElementTestOutput(
            eq("workflow-1"), eq("node-1"), eq("PROCESSOR"), eq("script_1"), environmentIdCaptor.capture());

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
