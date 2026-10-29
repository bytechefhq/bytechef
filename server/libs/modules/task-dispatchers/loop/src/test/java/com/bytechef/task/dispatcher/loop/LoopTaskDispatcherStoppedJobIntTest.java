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

package com.bytechef.task.dispatcher.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.StoppedJobTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

/**
 * Reproduces a loop whose job a user stops while one of its children is in flight.
 *
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
class LoopTaskDispatcherStoppedJobIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private final List<Task> workerTasks = new ArrayList<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskFileStorage taskFileStorage;

    private ContextService contextService;
    private JobService jobService;
    private LoopTaskCompletionHandler loopTaskCompletionHandler;
    private TaskDispatcherChain taskDispatcherChain;
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void beforeEach() {
        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        contextService = new ContextServiceImpl(new InMemoryContextRepository());
        jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        ApplicationEventPublisher eventPublisher = event -> {};

        taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            List.of(
                new StoppedJobTaskDispatcher(jobService, taskExecutionService),
                new LoopTaskDispatcher(
                    contextService, EVALUATOR, eventPublisher, taskDispatcherChain, taskExecutionService,
                    taskFileStorage),
                new WorkerTaskDispatcher()));

        loopTaskCompletionHandler = new LoopTaskCompletionHandler(
            contextService, EVALUATOR, new UnexpectedTaskCompletionHandler(), taskDispatcherChain,
            taskExecutionService, taskFileStorage);

        workerTasks.clear();
    }

    @Test
    void testChildCompletingAfterStopDoesNotLeaveNextChildUnfinished() {
        long jobId = createStartedJob();

        TaskExecution loopTaskExecution = createLoopTaskExecution(jobId);

        taskDispatcherChain.dispatch(loopTaskExecution);

        TaskExecution stopperTaskExecution = (TaskExecution) workerTasks.getFirst();

        jobService.setStatusToStopped(jobId);

        stopperTaskExecution.setStatus(TaskExecution.Status.COMPLETED);

        loopTaskCompletionHandler.handle(stopperTaskExecution);

        List<TaskExecution> childTaskExecutions = taskExecutionService.getParentTaskExecutions(
            Objects.requireNonNull(loopTaskExecution.getId()));

        assertThat(childTaskExecutions)
            .extracting(TaskExecution::getName, TaskExecution::getStatus)
            .contains(tuple("next", TaskExecution.Status.CANCELLED));
        assertThat(workerTasks).containsExactly(stopperTaskExecution);
    }

    @Test
    void testLoopDispatchedAfterStopIsCancelledWithoutChildren() {
        long jobId = createStartedJob();

        jobService.setStatusToStopped(jobId);

        TaskExecution loopTaskExecution = createLoopTaskExecution(jobId);

        taskDispatcherChain.dispatch(loopTaskExecution);

        TaskExecution persistedLoopTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(loopTaskExecution.getId()));

        assertThat(persistedLoopTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(taskExecutionService.getParentTaskExecutions(Objects.requireNonNull(loopTaskExecution.getId())))
            .isEmpty();
        assertThat(workerTasks).isEmpty();
    }

    private long createStartedJob() {
        Job job = new Job();

        job.setStatus(Job.Status.CREATED);
        job.setWorkflowId("loopWorkflow");

        job = jobService.update(job);

        long jobId = Objects.requireNonNull(job.getId());

        jobService.setStatusToStarted(jobId);

        return jobId;
    }

    private TaskExecution createLoopTaskExecution(long jobId) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .taskNumber(0)
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        "name", "loop_1",
                        "type", "loop/v1",
                        "parameters", Map.of(
                            "items", List.of(1, 2),
                            "iteratee", List.of(
                                Map.of("name", "stopper", "type", "var/v1/set", "parameters", Map.of("value", 1)),
                                Map.of("name", "next", "type", "var/v1/set", "parameters", Map.of("value", 2)))))))
            .build();

        taskExecution = taskExecutionService.create(taskExecution);

        long taskExecutionId = Objects.requireNonNull(taskExecution.getId());

        contextService.push(
            taskExecutionId, Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(taskExecutionId, Context.Classname.TASK_EXECUTION, Map.of()));

        return taskExecution;
    }

    private class WorkerTaskDispatcher implements TaskDispatcher<Task>, TaskDispatcherResolver {

        @Override
        public void dispatch(Task task) {
            workerTasks.add(task);
        }

        @Override
        public TaskDispatcher<? extends Task> resolve(Task task) {
            return this;
        }
    }

    private static class UnexpectedTaskCompletionHandler implements TaskCompletionHandler {

        @Override
        public boolean canHandle(TaskExecution taskExecution) {
            return true;
        }

        @Override
        public void handle(TaskExecution taskExecution) {
            throw new IllegalStateException("The loop must not complete: " + taskExecution);
        }
    }
}
