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

package com.bytechef.platform.workflow.execution;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.execution.exception.JobErrorType;
import com.bytechef.platform.workflow.execution.exception.TaskExecutionErrorType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates a completed job's terminal state into an {@link ExecutionException}, mirroring the error check the
 * embedded {@code JobSyncExecutor} performed after awaiting a synchronous job. A {@code STOPPED} job (e.g. a suspended
 * workflow) is not an error.
 *
 * @author Ivica Cardic
 */
public final class JobExecutionErrors {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionErrors.class);

    private JobExecutionErrors() {
    }

    public static void checkForError(Job job, TaskExecutionService taskExecutionService) {
        TaskExecution taskExecution = taskExecutionService
            .fetchLastJobTaskExecution(Validate.notNull(job.getId(), "id"))
            .orElse(null);

        if (taskExecution != null && taskExecution.getStatus() == TaskExecution.Status.FAILED) {
            ExecutionError error = taskExecution.getError();

            if (error != null && error.getMessage() != null) {
                throw new ExecutionException(error.getMessage(), TaskExecutionErrorType.TASK_EXECUTION_FAILED);
            }

            String message = "Task execution failed for job " + job.getId() + " but no error details are available.";

            if (log.isWarnEnabled()) {
                log.warn(
                    "Detected FAILED task execution without error details for jobId={}, taskExecutionId={}",
                    job.getId(), taskExecution.getId());
            }

            throw new ExecutionException(message, TaskExecutionErrorType.TASK_EXECUTION_FAILED);
        }

        if (job.getStatus() == Job.Status.FAILED) {
            ExecutionError error = job.getError();

            if (error != null && error.getMessage() != null) {
                throw new ExecutionException(error.getMessage(), JobErrorType.JOB_FAILED);
            }

            String message = "Job " + job.getId() + " failed but no error details are available.";

            if (log.isWarnEnabled()) {
                log.warn("Detected FAILED job without error details for jobId={}", job.getId());
            }

            throw new ExecutionException(message, JobErrorType.JOB_FAILED);
        }
    }
}
