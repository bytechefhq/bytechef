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
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface WebhookWorkflowExecutor {

    void execute(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);

    @Nullable
    Object executeSync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);

    WebhookValidateResponse validateAndExecuteAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);

    WebhookValidateResponse validateOnEnable(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest);

    /**
     * Initiates a workflow execution with Server-Sent Events (SSE) streaming support. Returns a list of job IDs
     * representing the started jobs and registers the provided SSE stream bridge to receive workflow execution events.
     *
     * @param workflowExecutionId the identifier of the workflow execution for which the SSE stream is initiated
     * @param webhookRequest      the webhook request containing headers, parameters, and body related to the event
     * @param sseStreamBridge     the bridge that will receive SSE events (start, stream, error, etc.)
     * @return a list of job IDs that were started for this workflow execution
     */
    List<Long> executeSseStream(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, SseStreamBridge sseStreamBridge);
}
