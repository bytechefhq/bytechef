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

import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

/**
 * Defines the contract for executing and managing webhook-based workflow executions. Provides methods to execute
 * workflows asynchronously, synchronously, or with SSE streaming support. Additionally, includes validation
 * capabilities for webhook requests during execution or enablement processes.
 *
 * @author Ivica Cardic
 */
public interface WebhookWorkflowExecutor {

    /**
     * Executes a webhook workflow asynchronously based on the provided workflow execution identifier and webhook
     * request.
     *
     * @param workflowExecutionId the unique identifier of the workflow execution, providing details such as tenant,
     *                            type, and trigger
     * @param webhookRequest      the webhook request containing headers, parameters, and body relevant to the execution
     */
    void executeAsync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);

    /**
     * Executes a webhook workflow asynchronously based on the provided workflow execution identifier, webhook request,
     * and server-sent events (SSE) stream bridge.
     *
     * @param workflowExecutionId the unique identifier of the workflow execution, including tenant, type, workflow
     *                            UUID, and trigger details
     * @param webhookRequest      the webhook request containing headers, parameters, and body pertinent to the workflow
     *                            execution
     * @param sseStreamBridge     the server-sent events (SSE) stream bridge used to facilitate real-time event
     *                            handling, such as events, errors, and stream completion
     * @return a {@code CompletableFuture<Void>} that completes when the workflow execution is finished or encounters an
     *         error
     */
    CompletableFuture<Void> executeAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, SseStreamBridge sseStreamBridge);

    /**
     * Executes a webhook workflow synchronously based on the provided workflow execution identifier and webhook
     * request. Used for the non-streaming chat reply path: the workflow runs to completion, the
     * {@code chat/responseToRequest} step's {@code WebhookResponse} is collected via the task-execution-complete
     * callback, and the final outputs map is returned.
     *
     * @param workflowExecutionId the unique identifier of the workflow execution, including details such as tenant,
     *                            type, and trigger
     * @param webhookRequest      the webhook request containing headers, parameters, and body relevant to the execution
     * @return the result of the synchronous execution, or {@code null} if no result is returned
     */
    @Nullable
    Object executeSync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);

    /**
     * Returns the trigger flags associated with the workflow's webhook trigger. Cached lookup — the underlying
     * {@code TriggerDefinitionService} call is the same one the controller already uses to decide between sync and
     * async dispatch on the HTTP path.
     */
    WebhookTriggerFlags getWebhookTriggerFlags(WorkflowExecutionId workflowExecutionId);

    /**
     * Returns {@code true} when the workflow contains at least one task whose action streams output (e.g.
     * {@code openAi/v1/streamAsk}, {@code aiAgent/v1/streamChat}).
     */
    boolean hasStreamingTask(WorkflowExecutionId workflowExecutionId);

    /**
     * Checks whether the workflow associated with the given execution id is currently disabled. Both transports use
     * this to short-circuit the dispatch path before execution is invoked.
     */
    boolean isWorkflowDisabled(WorkflowExecutionId workflowExecutionId);

    /**
     * Validates the provided webhook request and executes the associated workflow asynchronously based on the given
     * workflow execution identifier.
     *
     * @param workflowExecutionId the unique identifier for the workflow execution containing details such as tenant,
     *                            type, and trigger
     * @param webhookRequest      the webhook request with headers, parameters, and body relevant to the execution
     * @return a {@link WebhookValidateResponse} instance encapsulating the status, headers, and body of the validation
     *         and execution process
     */
    WebhookValidateResponse validateAndExecuteAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);

    /**
     * Validates the provided webhook request during the enablement process of a workflow execution.
     *
     * @param workflowExecutionId the unique identifier for the workflow execution, containing details such as tenant,
     *                            type, trigger, and workflow specifics
     * @param webhookRequest      the webhook request containing headers, parameters, and body relevant to the
     *                            enablement process
     * @return a {@link WebhookValidateResponse} containing the status, headers, and response body of the validation
     *         process
     */
    WebhookValidateResponse validateOnEnable(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);
}
