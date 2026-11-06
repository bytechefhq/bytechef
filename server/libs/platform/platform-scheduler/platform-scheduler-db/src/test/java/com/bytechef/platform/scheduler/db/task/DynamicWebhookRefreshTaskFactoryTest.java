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

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class DynamicWebhookRefreshTaskFactoryTest {

    private final DynamicWebhookRefresher refresher = Mockito.mock(DynamicWebhookRefresher.class);
    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 9L, "workflow-uuid", "trigger_1");

    @Test
    void testExecuteReschedulesToReturnedExpiry() {
        Instant newExpiry = Instant.parse("2031-01-01T00:00:00Z");

        Mockito.when(refresher.refresh(
            ArgumentMatchers.argThat(
                candidateWorkflowExecutionId -> candidateWorkflowExecutionId.toString()
                    .equals(workflowExecutionId.toString())),
            ArgumentMatchers.eq(11L)))
            .thenReturn(newExpiry);

        CompletionHandler<DynamicWebhookRefreshData> completionHandler = execute();

        Assertions.assertThat(completionHandler)
            .isEqualTo(new RescheduleAt<DynamicWebhookRefreshData>(newExpiry));
    }

    @Test
    void testExecuteRemovesWhenRefreshReturnsNoExpiry() {
        Mockito.when(refresher.refresh(
            ArgumentMatchers.argThat(
                candidateWorkflowExecutionId -> candidateWorkflowExecutionId.toString()
                    .equals(workflowExecutionId.toString())),
            ArgumentMatchers.eq(11L)))
            .thenReturn(null);

        CompletionHandler<DynamicWebhookRefreshData> completionHandler = execute();

        Assertions.assertThat(completionHandler)
            .isInstanceOf(CompletionHandler.OnCompleteRemove.class);
    }

    private CompletionHandler<DynamicWebhookRefreshData> execute() {
        CustomTask<DynamicWebhookRefreshData> task = DynamicWebhookRefreshTaskFactory.create(refresher);
        TaskInstance<DynamicWebhookRefreshData> taskInstance = DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH
            .instance(workflowExecutionId.toString())
            .data(new DynamicWebhookRefreshData(11L))
            .build();

        return task.execute(taskInstance, TaskTestSupport.executionContext(taskInstance, Instant.now()));
    }
}
