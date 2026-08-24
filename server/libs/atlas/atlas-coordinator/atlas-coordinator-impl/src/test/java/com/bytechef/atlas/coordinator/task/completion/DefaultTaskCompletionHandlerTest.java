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

package com.bytechef.atlas.coordinator.task.completion;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the transactional-completion contract: with a {@link TransactionTemplate} present, the whole
 * update-task/push-context/advance-job sequence runs inside one transaction (committed on success, rolled back on
 * failure), and without one — the in-memory sync-executor path — completion still works exactly as before.
 *
 * @author Ivica Cardic
 */
class DefaultTaskCompletionHandlerTest {

    static {
        MapUtils.setObjectMapper(JsonMapper.builder()
            .build());
    }

    private final ContextService contextService = mock(ContextService.class);
    private final JobExecutor jobExecutor = mock(JobExecutor.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final Job job = new Job(1L);
    private final TaskExecution taskExecution = TaskExecution.builder()
        .workflowTask(new WorkflowTask(Map.of("name", "task1", "type", "type/v1")))
        .build();

    @BeforeEach
    void beforeEach() {
        job.setWorkflowId("workflow1");
        job.setCurrentTask(0);

        taskExecution.setId(100L);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getTasks()).thenReturn(List.of(mock(WorkflowTask.class)));

        when(jobService.getTaskExecutionJob(100L)).thenReturn(job);
        when(jobService.update(any())).thenReturn(job);
        when(taskExecutionService.update(any())).thenReturn(taskExecution);
        when(workflowService.getWorkflow("workflow1")).thenReturn(workflow);
        when(contextService.peek(anyLong(), any())).thenReturn(new FileEntry("context", "/tmp/context.json"));
        when(taskFileStorage.readContextValue(any())).thenReturn(Map.of());
        when(taskFileStorage.storeContextValue(anyLong(), any(), any()))
            .thenReturn(new FileEntry("context", "/tmp/context.json"));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void testHandleCommitsTransactionWhenTemplatePresent() {
        DefaultTaskCompletionHandler taskCompletionHandler = getTaskCompletionHandler(
            new TransactionTemplate(transactionManager));

        taskCompletionHandler.handle(taskExecution);

        verify(transactionManager).getTransaction(any());
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
        verify(jobExecutor).completeJob(job);
    }

    @Test
    void testHandleRollsBackTransactionOnFailure() {
        doThrow(new IllegalStateException("persistence failure")).when(jobExecutor)
            .completeJob(any());

        DefaultTaskCompletionHandler taskCompletionHandler = getTaskCompletionHandler(
            new TransactionTemplate(transactionManager));

        assertThrows(IllegalStateException.class, () -> taskCompletionHandler.handle(taskExecution));

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void testHandleWithoutTransactionTemplateCompletesJob() {
        DefaultTaskCompletionHandler taskCompletionHandler = getTaskCompletionHandler(null);

        taskCompletionHandler.handle(taskExecution);

        verify(jobExecutor).completeJob(job);
        verify(transactionManager, never()).getTransaction(any());
    }

    private DefaultTaskCompletionHandler getTaskCompletionHandler(
        @Nullable TransactionTemplate transactionTemplate) {

        return new DefaultTaskCompletionHandler(
            contextService, jobExecutor, jobService, taskExecutionService, taskFileStorage, transactionTemplate,
            workflowService);
    }
}
