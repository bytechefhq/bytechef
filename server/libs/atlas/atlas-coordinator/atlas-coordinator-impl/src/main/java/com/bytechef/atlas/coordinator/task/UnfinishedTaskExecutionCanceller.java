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

package com.bytechef.atlas.coordinator.task;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cancels every unfinished task execution of a job and tells the workers to stop the ones still running.
 *
 * @author Ivica Cardic
 */
public class UnfinishedTaskExecutionCanceller {

    private static final Logger log = LoggerFactory.getLogger(UnfinishedTaskExecutionCanceller.class);

    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public UnfinishedTaskExecutionCanceller(
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService) {

        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    public void cancel(long jobId) {
        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        Long cancelledTaskExecutionId = null;

        for (TaskExecution taskExecution : taskExecutions) {
            TaskExecution.Status status = taskExecution.getStatus();

            if (status != null && status.isTerminated()) {
                continue;
            }

            long taskExecutionId = Validate.notNull(taskExecution.getId(), "id");

            try {
                if (taskExecutionService.cancelIfUnfinished(taskExecutionId) && cancelledTaskExecutionId == null) {
                    cancelledTaskExecutionId = taskExecutionId;
                }
            } catch (RuntimeException runtimeException) {
                log.error(
                    "Unable to cancel task execution id={} of job id={}", taskExecutionId, jobId, runtimeException);
            }
        }

        if (cancelledTaskExecutionId != null) {
            taskDispatcher.dispatch(new CancelControlTask(jobId, cancelledTaskExecutionId));
        }
    }
}
