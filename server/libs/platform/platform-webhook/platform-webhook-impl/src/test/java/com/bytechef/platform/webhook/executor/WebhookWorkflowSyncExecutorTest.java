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

package com.bytechef.platform.webhook.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAuthenticationResolver;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.platform.workflow.worker.security.JobPrincipalAuthenticationRunner;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebhookWorkflowSyncExecutorTest {

    private static final long PROJECT_DEPLOYMENT_ID = 4L;
    private static final String WORKFLOW_ID = "workflow-id";
    private static final String WORKFLOW_UUID = "workflow-uuid";

    private final Evaluator evaluator = mock(Evaluator.class);
    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
    private final JobPrincipalAuthenticationResolver jobPrincipalAuthenticationResolver = mock(
        JobPrincipalAuthenticationResolver.class);
    private final TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);
    private final TriggerExecutionService triggerExecutionService = mock(TriggerExecutionService.class);
    private final TriggerFileStorage triggerFileStorage = mock(TriggerFileStorage.class);
    private final TriggerStateService triggerStateService = mock(TriggerStateService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor = new WebhookWorkflowSyncExecutor(
        evaluator, jobPrincipalAccessorRegistry,
        new JobPrincipalAuthenticationRunner(List.of(jobPrincipalAuthenticationResolver)), triggerDefinitionFacade,
        triggerExecutionService, List.of(), triggerFileStorage, triggerStateService, workflowService);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testExecuteRunsTheWebhookTriggerAsTheDeploymentOwner() {
        AtomicReference<Authentication> authenticationReference = new AtomicReference<>();
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID, "trigger_1");

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);
        when(jobPrincipalAccessor.getInputMap(PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID)).thenReturn(Map.of());
        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(eq(WorkflowExtConstants.TRIGGERS), eq(WorkflowTrigger.class), anyList()))
            .thenReturn(
                List.of(new WorkflowTrigger(Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200"))));
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(evaluator.evaluate(anyMap(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        when(triggerExecutionService.create(any())).thenAnswer(invocation -> {
            TriggerExecution triggerExecution = invocation.getArgument(0);

            triggerExecution.setId(21L);

            return triggerExecution;
        });
        when(triggerStateService.fetchValue(workflowExecutionId)).thenReturn(Optional.empty());
        when(jobPrincipalAuthenticationResolver.getType()).thenReturn(PlatformType.AUTOMATION);
        when(jobPrincipalAuthenticationResolver.isApplicable(anyLong())).thenReturn(true);
        when(jobPrincipalAuthenticationResolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                Optional.of(
                    UsernamePasswordAuthenticationToken.authenticated(
                        "owner", null, List.of(new SimpleGrantedAuthority("ROLE_USER")))));
        when(
            triggerDefinitionFacade.executeTrigger(
                anyString(), eq(1), anyString(), eq(PROJECT_DEPLOYMENT_ID), eq(WORKFLOW_UUID), eq(21L), anyMap(),
                any(), any(), any(), any(), eq(PlatformType.AUTOMATION), anyBoolean()))
                    .thenAnswer(invocation -> {
                        SecurityContext securityContext = SecurityContextHolder.getContext();

                        authenticationReference.set(securityContext.getAuthentication());

                        return new TriggerOutput(Map.of(), null, false);
                    });

        webhookWorkflowSyncExecutor.execute(workflowExecutionId, mock(WebhookRequest.class));

        Authentication authentication = authenticationReference.get();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("owner");
        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isNull();
    }
}
