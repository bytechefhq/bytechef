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

package com.bytechef.platform.job.sync.simulation;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult.Outcome;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Drives {@link JobSyncExecutor} in dry-run mode and maps the terminal {@link Job} onto a
 * {@link WorkflowSimulationResult}.
 *
 * @author Ivica Cardic
 */
public class WorkflowSimulationFacadeImpl implements WorkflowSimulationFacade {

    private final JobSyncExecutor jobSyncExecutor;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public WorkflowSimulationFacadeImpl(JobSyncExecutor jobSyncExecutor, TaskExecutionService taskExecutionService) {
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public WorkflowSimulationResult simulate(String workflowId, Map<String, ?> inputs) {
        // checkForError=false so a failed task returns an inspectable Job rather than throwing.
        Job job = jobSyncExecutor.execute(
            new JobParametersDTO(
                workflowId, inputs,
                Map.of(MetadataConstants.DRY_RUN, true, MetadataConstants.EDITOR_ENVIRONMENT, true)),
            false);

        if (job.getStatus() == Job.Status.COMPLETED) {
            return new WorkflowSimulationResult(Outcome.COMPLETED, null, null, null, List.of());
        }

        long jobId = Validate.notNull(job.getId(), "job id must not be null");

        TaskExecution failedTaskExecution = taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> taskExecution.getStatus() == TaskExecution.Status.FAILED)
            .findFirst()
            .orElse(null);

        String reason = resolveReason(job, failedTaskExecution);

        if (reason == null) {
            // A timeout or cancel yields a non-COMPLETED job with no FAILED task and no job error; still report a
            // generic, actionable reason rather than a null one.
            reason = "Workflow did not complete (status=" + job.getStatus() + ")";
        }

        return new WorkflowSimulationResult(
            Outcome.FAILED,
            failedTaskExecution == null ? null : failedTaskExecution.getName(),
            failedTaskExecution == null ? null : failedTaskExecution.getType(),
            reason,
            List.of());
    }

    private static @Nullable String resolveReason(Job job, @Nullable TaskExecution failedTaskExecution) {
        ExecutionError jobError = job.getError();

        if (jobError != null && jobError.getMessage() != null) {
            return jobError.getMessage();
        }

        if (failedTaskExecution != null) {
            ExecutionError taskError = failedTaskExecution.getError();

            if (taskError != null) {
                return taskError.getMessage();
            }
        }

        return null;
    }
}
