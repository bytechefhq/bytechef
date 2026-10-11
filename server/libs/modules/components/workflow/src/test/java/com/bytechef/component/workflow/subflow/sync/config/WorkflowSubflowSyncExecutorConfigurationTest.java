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

package com.bytechef.component.workflow.subflow.sync.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.component.workflow.subflow.sync.SubflowSyncExecutor;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.job.sync.executor.SyncExecutionSuspendRejectingTaskCompletionHandler;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowSubflowSyncExecutorConfigurationTest {

    private final JobService jobService = mock(JobService.class);
    private final WorkflowSubflowSyncExecutorConfiguration workflowSubflowSyncExecutorConfiguration =
        new WorkflowSubflowSyncExecutorConfiguration();

    @Test
    void testIsSyncJobReadsTheSyncExecutionMetadata() {
        when(jobService.fetchJob(1L)).thenReturn(Optional.of(job(Map.of(MetadataConstants.SYNC_EXECUTION, true))));
        when(jobService.fetchJob(2L)).thenReturn(Optional.of(job(Map.of())));
        when(jobService.fetchJob(3L)).thenReturn(Optional.empty());

        SubflowSyncExecutor subflowSyncExecutor = createSubflowSyncExecutor();

        assertThat(subflowSyncExecutor.isSyncJob(1L)).isTrue();
        assertThat(subflowSyncExecutor.isSyncJob(2L)).isFalse();
        assertThat(subflowSyncExecutor.isSyncJob(3L)).isFalse();
    }

    @Test
    void testTheFirstTaskCompletionHandlerRejectsASuspendingStep() {
        List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories =
            workflowSubflowSyncExecutorConfiguration.getTaskCompletionHandlerFactories(
                mock(ContextService.class), mock(ApplicationEventPublisher.class), mock(CounterService.class),
                mock(Evaluator.class), jobService, mock(TaskExecutionService.class), mock(TaskFileStorage.class));

        TaskCompletionHandlerFactory taskCompletionHandlerFactory = taskCompletionHandlerFactories.getFirst();

        TaskCompletionHandler taskCompletionHandler = taskCompletionHandlerFactory.createTaskCompletionHandler(
            mock(TaskCompletionHandler.class), null);

        assertThat(taskCompletionHandler).isInstanceOf(SyncExecutionSuspendRejectingTaskCompletionHandler.class);

        TaskExecution suspendedTaskExecution = new TaskExecution();

        suspendedTaskExecution.putMetadata(MetadataConstants.JOB_RESUME_ID, "resume-id");

        assertThat(taskCompletionHandler.canHandle(suspendedTaskExecution)).isTrue();
    }

    private SubflowSyncExecutor createSubflowSyncExecutor() {
        return workflowSubflowSyncExecutorConfiguration.subflowSyncExecutor(
            mock(ChildJobPrincipalFactory.class), mock(ContextService.class), mock(CounterService.class),
            mock(TaskFileStorage.class), new MockEnvironment(), mock(Evaluator.class), jobService,
            mock(SubflowResolver.class), List.of(), mock(TaskExecutionService.class), mock(TaskHandlerRegistry.class),
            mock(WorkflowService.class));
    }

    private static Job job(Map<String, ?> metadata) {
        Job job = new Job();

        job.setMetadata(metadata);

        return job;
    }
}
