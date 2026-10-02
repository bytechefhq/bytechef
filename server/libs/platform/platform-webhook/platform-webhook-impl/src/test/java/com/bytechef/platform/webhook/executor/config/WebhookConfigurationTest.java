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

package com.bytechef.platform.webhook.executor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.job.sync.executor.SyncExecutionSuspendRejectingTaskCompletionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class WebhookConfigurationTest {

    @Test
    void testTheFirstSyncTaskCompletionHandlerRejectsASuspendingStep() {
        WebhookConfiguration webhookConfiguration = new WebhookConfiguration();

        List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories =
            webhookConfiguration.getSyncTaskCompletionHandlerFactories(
                mock(ContextService.class), mock(ApplicationEventPublisher.class), mock(CounterService.class),
                mock(Evaluator.class), mock(JobService.class), mock(TaskExecutionService.class),
                mock(TaskFileStorage.class));

        TaskCompletionHandlerFactory taskCompletionHandlerFactory = taskCompletionHandlerFactories.getFirst();

        TaskCompletionHandler taskCompletionHandler = taskCompletionHandlerFactory.createTaskCompletionHandler(
            mock(TaskCompletionHandler.class), null);

        assertThat(taskCompletionHandler).isInstanceOf(SyncExecutionSuspendRejectingTaskCompletionHandler.class);

        TaskExecution suspendedTaskExecution = new TaskExecution();

        suspendedTaskExecution.putMetadata(MetadataConstants.JOB_RESUME_ID, "resume-id");

        assertThat(taskCompletionHandler.canHandle(suspendedTaskExecution)).isTrue();
    }
}
