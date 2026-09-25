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

package com.bytechef.platform.workflow.worker.trigger.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAuthenticationResolver;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.worker.security.JobPrincipalAuthenticationRunner;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class AbstractTriggerHandlerTest {

    private final JobPrincipalAuthenticationResolver jobPrincipalAuthenticationResolver = mock(
        JobPrincipalAuthenticationResolver.class);
    private final TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);

    private final AbstractTriggerHandler triggerHandler = new AbstractTriggerHandler(
        "webhook", 1, "newRequest", triggerDefinitionFacade,
        new JobPrincipalAuthenticationRunner(List.of(jobPrincipalAuthenticationResolver))) {};

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testHandleForwardsTheTriggerExecutionIdSoItsLogsCanBeStored() throws Exception {
        triggerHandler.handle(triggerExecution());

        verify(triggerDefinitionFacade).executeTrigger(
            eq("webhook"), eq(1), eq("newRequest"), eq(5L), eq("workflow-uuid"), eq(77L), anyMap(), any(), any(),
            any(), any(), eq(PlatformType.AUTOMATION), anyBoolean());
    }

    @Test
    void testHandleRunsAnUnattendedTriggerAsTheDeploymentOwner() throws Exception {
        AtomicReference<Authentication> authenticationReference = new AtomicReference<>();

        when(jobPrincipalAuthenticationResolver.getType()).thenReturn(PlatformType.AUTOMATION);
        when(jobPrincipalAuthenticationResolver.isApplicable(5L)).thenReturn(true);
        when(jobPrincipalAuthenticationResolver.fetchAuthentication(5L))
            .thenReturn(
                Optional.of(
                    UsernamePasswordAuthenticationToken.authenticated(
                        "owner", null, List.of(new SimpleGrantedAuthority("ROLE_USER")))));
        when(
            triggerDefinitionFacade.executeTrigger(
                any(), eq(1), any(), eq(5L), any(), any(), anyMap(), any(), any(), any(), any(), any(),
                anyBoolean()))
                    .thenAnswer(invocation -> {
                        SecurityContext securityContext = SecurityContextHolder.getContext();

                        authenticationReference.set(securityContext.getAuthentication());

                        return new TriggerOutput(Map.of(), null, false);
                    });

        triggerHandler.handle(triggerExecution());

        Authentication authentication = authenticationReference.get();

        assertThat(authentication.getName()).isEqualTo("owner");
        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isNull();
    }

    private static TriggerExecution triggerExecution() {
        WorkflowExecutionId workflowExecutionId = mock(WorkflowExecutionId.class);
        TriggerExecution triggerExecution = mock(TriggerExecution.class);

        when(workflowExecutionId.getJobPrincipalId()).thenReturn(5L);
        when(workflowExecutionId.getWorkflowUuid()).thenReturn("workflow-uuid");
        when(workflowExecutionId.getType()).thenReturn(PlatformType.AUTOMATION);
        when(triggerExecution.getId()).thenReturn(77L);
        when(triggerExecution.getWorkflowExecutionId()).thenReturn(workflowExecutionId);
        when(triggerExecution.getMetadata()).thenReturn(Map.of());
        when(triggerExecution.getParameters()).thenReturn(Map.of());

        return triggerExecution;
    }
}
