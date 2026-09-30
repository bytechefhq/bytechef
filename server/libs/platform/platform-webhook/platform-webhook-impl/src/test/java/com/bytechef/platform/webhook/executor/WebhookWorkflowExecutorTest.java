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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class WebhookWorkflowExecutorTest {

    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger_1");
    private final WebhookRequest webhookRequest = new WebhookRequest(
        Map.of(), Map.of(), null, WebhookMethod.POST);

    private ApplicationEventPublisher eventPublisher;
    private JobSyncExecutor jobSyncExecutor;
    private WebhookWorkflowExecutor webhookWorkflowExecutor;
    private WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor;

    @BeforeEach
    void beforeEach() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        jobSyncExecutor = mock(JobSyncExecutor.class);
        webhookWorkflowSyncExecutor = mock(WebhookWorkflowSyncExecutor.class);

        webhookWorkflowExecutor = new WebhookWorkflowExecutorImpl(
            eventPublisher, mock(JobPrincipalAccessorRegistry.class), jobSyncExecutor, mock(PrincipalJobFacade.class),
            mock(SseStreamBridgeRegistry.class), webhookWorkflowSyncExecutor, mock(TaskFileStorage.class),
            mock(TriggerDefinitionService.class), mock(WorkflowService.class));
    }

    @Test
    @Disabled
    public void testExecuteAsync() {
        // TODO
    }

    @Test
    @Disabled
    public void testExecuteAsyncSync() {
        // TODO
    }

    @Test
    public void testValidateReturnsTriggerValidationResponseWithoutExecutingWorkflow() {
        WebhookValidateResponse badRequestResponse = WebhookValidateResponse.badRequest();

        when(webhookWorkflowSyncExecutor.validate(any(), any())).thenReturn(badRequestResponse);

        assertThat(webhookWorkflowExecutor.validate(workflowExecutionId, webhookRequest))
            .isSameAs(badRequestResponse);

        verify(webhookWorkflowSyncExecutor).validate(workflowExecutionId, webhookRequest);
        verifyNoInteractions(eventPublisher, jobSyncExecutor);
    }

    @Test
    @Disabled
    public void testValidateAndExecuteAsync() {
        // TODO
    }
}
