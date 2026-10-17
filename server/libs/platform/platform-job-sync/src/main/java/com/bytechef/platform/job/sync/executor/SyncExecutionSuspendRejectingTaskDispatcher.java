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

package com.bytechef.platform.job.sync.executor;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Fails a synchronously executed job before a step that can pause it runs, so the step never delivers anything, for
 * example an approval request nobody could act on.
 *
 * @author Ivica Cardic
 */
public class SyncExecutionSuspendRejectingTaskDispatcher
    implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final BiFunction<String, String, String> errorMessageFunction;
    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    /**
     * @param errorMessageFunction builds the failed step's error from the step's name and type
     */
    @SuppressFBWarnings("EI")
    public SyncExecutionSuspendRejectingTaskDispatcher(
        ApplicationEventPublisher eventPublisher, JobService jobService, TaskExecutionService taskExecutionService,
        BiFunction<String, String, String> errorMessageFunction) {

        this.errorMessageFunction = errorMessageFunction;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        SyncExecutionSuspendRejectingTaskCompletionHandler.failJob(
            taskExecution, errorMessageFunction, eventPublisher, jobService, taskExecutionService);
    }

    @Override
    public @Nullable TaskDispatcher<? extends Task> resolve(Task task) {
        if (task instanceof TaskExecution taskExecution && SuspendingTasks.isSuspending(
            taskExecution.getWorkflowTask())) {

            return this;
        }

        return null;
    }
}
