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

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.SuspendedJobs;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cancels, instead of dispatching, every task execution of a job that a user stopped. It goes first in the dispatcher
 * chain so that flow-control dispatchers, which never reach the pre-send processors, and the tasks they create while
 * the stop is in progress do not start.
 *
 * @author Ivica Cardic
 */
public class StoppedJobTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private static final Logger log = LoggerFactory.getLogger(StoppedJobTaskDispatcher.class);

    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public StoppedJobTaskDispatcher(JobService jobService, TaskExecutionService taskExecutionService) {
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        long taskExecutionId = Validate.notNull(taskExecution.getId(), "id");

        taskExecutionService.cancelIfUnfinished(taskExecutionId);

        if (log.isDebugEnabled()) {
            log.debug("Task id={} of stopped job id={} cancelled", taskExecutionId, taskExecution.getJobId());
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (!(task instanceof TaskExecution taskExecution) || taskExecution.getJobId() == null) {
            return null;
        }

        Job job = jobService.getJob(taskExecution.getJobId());

        return SuspendedJobs.isStoppedByUser(job) ? this : null;
    }
}
