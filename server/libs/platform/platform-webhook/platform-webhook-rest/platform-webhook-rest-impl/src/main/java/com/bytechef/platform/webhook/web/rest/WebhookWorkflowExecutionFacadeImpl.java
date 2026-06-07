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

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.executor.WebhookExecutionResult;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutionFacade;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnCoordinator
public class WebhookWorkflowExecutionFacadeImpl implements WebhookWorkflowExecutionFacade {

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WebhookWorkflowExecutor webhookWorkflowExecutor;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WebhookWorkflowExecutionFacadeImpl(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerDefinitionService triggerDefinitionService,
        WebhookWorkflowExecutor webhookWorkflowExecutor, WorkflowService workflowService) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
        this.workflowService = workflowService;
    }

    @Override
    public WebhookExecutionResult executeSync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        if (isWorkflowDisabled(workflowExecutionId)) {
            return new WebhookExecutionResult.Disabled("Workflow is disabled.");
        }

        Object outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, webhookRequest);

        return new WebhookExecutionResult.Ok(outputs);
    }

    @Override
    public CompletableFuture<Void> executeStreaming(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, SseStreamBridge sseStreamBridge) {

        if (isWorkflowDisabled(workflowExecutionId)) {
            sseStreamBridge.onError(new IllegalStateException("Workflow is disabled."));

            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = webhookWorkflowExecutor.executeAsync(
            workflowExecutionId, webhookRequest, sseStreamBridge);

        future.whenComplete((unused, throwable) -> {
            if (throwable != null) {
                sseStreamBridge.onError(throwable);
            } else {
                sseStreamBridge.onComplete();
            }
        });

        return future;
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(WorkflowExecutionId workflowExecutionId) {
        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId);

        return triggerDefinitionService.getWebhookTriggerFlags(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());
    }

    @Override
    public boolean hasStreamingTask(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(getWorkflowId(workflowExecutionId));

        return workflow.getTasks(true)
            .stream()
            .map(com.bytechef.atlas.configuration.domain.WorkflowTask::getType)
            .filter(java.util.Objects::nonNull)
            .anyMatch(type -> type.toLowerCase()
                .contains("stream"));
    }

    @Override
    public boolean isWorkflowDisabled(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return !jobPrincipalAccessor.isWorkflowEnabled(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(getWorkflowId(workflowExecutionId));

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        if (workflowExecutionId.getJobPrincipalId() == -1) {
            return jobPrincipalAccessor.getLastWorkflowId(workflowExecutionId.getWorkflowUuid());
        }

        return jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }
}
