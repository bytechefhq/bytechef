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

package com.bytechef.platform.scheduler.db.task;

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH;

import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.time.Duration;
import java.time.Instant;

/**
 * @author Ivica Cardic
 */
public final class DynamicWebhookRefreshTaskFactory {

    static final Duration RETRY_DELAY = Duration.ofMinutes(5);
    static final int MAX_RETRIES = 3;

    private DynamicWebhookRefreshTaskFactory() {
    }

    public static CustomTask<DynamicWebhookRefreshData> create(DynamicWebhookRefresher refresher) {
        return Tasks.custom(DYNAMIC_WEBHOOK_REFRESH)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                MAX_RETRIES, new FailureHandler.OnFailureRetryLater<>(RETRY_DELAY)))
            .execute((taskInstance, executionContext) -> {
                DynamicWebhookRefreshData data = taskInstance.getData();
                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(taskInstance.getId());

                Instant newExpiry = ContextBinding.call(
                    workflowExecutionId.getTenantId(),
                    () -> refresher.refresh(workflowExecutionId, data.connectionId()));

                if (newExpiry == null) {
                    return new CompletionHandler.OnCompleteRemove<>();
                }

                return new RescheduleAt<>(newExpiry);
            });
    }
}
