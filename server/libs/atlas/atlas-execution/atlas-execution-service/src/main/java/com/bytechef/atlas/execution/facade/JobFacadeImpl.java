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

package com.bytechef.atlas.execution.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.util.WorkflowTaskUtils;
import com.bytechef.atlas.coordinator.event.DeleteJobEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
public class JobFacadeImpl implements JobFacade {

    private static final Logger log = LoggerFactory.getLogger(JobFacadeImpl.class);

    private static final String APPROVALS = "approvals";

    private final ApplicationEventPublisher eventPublisher;
    private final ContextService contextService;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public JobFacadeImpl(
        ApplicationEventPublisher eventPublisher, ContextService contextService, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this.eventPublisher = eventPublisher;
        this.contextService = contextService;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    // Propagation.NEVER is set because of sending job messages via queue in monolith mode, where it can happen
    // the case where a job is finished and the completion task executed, but the transaction is not yet committed and
    // the job id is missing.
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long createJob(JobParametersDTO jobParametersDTO) {
        Workflow workflow = workflowService.getWorkflow(jobParametersDTO.getWorkflowId());

        Job job = jobService.create(jobParametersDTO, workflow);

        long jobId = Validate.notNull(job.getId(), "id");

        log.debug("Job id={}, label='{}' created", jobId, job.getLabel());

        // Disabled tasks never reach the engine, so nothing would ever put their names into the job context and
        // every reference to one would resolve to the raw expression string. Seeding them as null up front makes
        // '${disabledTask}' evaluate to null instead; a property path under it, '${disabledTask.field}', is still
        // left as the raw expression string. A workflow input sharing a name with a disabled task keeps its value:
        // seeding must never overwrite an input.
        Map<String, Object> initialContext = new HashMap<>(job.getInputs());

        for (String disabledTaskName : WorkflowTaskUtils.getDisabledTaskNames(workflow.getTasks())) {
            initialContext.putIfAbsent(disabledTaskName, null);
        }

        contextService.push(
            jobId, Context.Classname.JOB,
            taskFileStorage.storeContextValue(jobId, Context.Classname.JOB, initialContext));

        eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, job.getStatus()));
        eventPublisher.publishEvent(new StartJobEvent(jobId));

        return jobId;
    }

    @Override
    @Transactional
    public void deleteJob(long id) {
        for (long childJobId : jobService.getChildJobIds(id)) {
            deleteJob(childJobId);
        }

        deleteJobFiles(id);

        taskExecutionService.deleteJobTaskExecutions(id);

        // Give platform-layer listeners a chance to release resources keyed off the job (e.g. a suspended run's
        // task_state row, which otherwise leaks its rendered approval request on a user/retention delete) before the
        // job — and its metadata — is gone. Published with the metadata so a listener need not re-read the row.
        Job job = jobService.getJob(id);

        eventPublisher.publishEvent(new DeleteJobEvent(id, job.getMetadata()));

        jobService.deleteJob(id);
    }

    /**
     * Releases the file-storage blobs and context rows a job accumulated: task-execution outputs, job outputs, and
     * every context value pushed under the job's and its task executions' stacks. Cleanup is best-effort — a storage
     * failure must never block the row delete (an orphaned blob is preferable to a job that cannot be purged), so each
     * blob delete is individually guarded. In-memory and remote-client deployments without context enumeration skip the
     * context portion.
     */
    private void deleteJobFiles(long jobId) {
        Job job = jobService.getJob(jobId);

        if (job.getOutputs() != null) {
            deleteFileQuietly(() -> taskFileStorage.deleteJobOutputs(job.getOutputs()));
        }

        for (TaskExecution taskExecution : taskExecutionService.getJobTaskExecutions(jobId)) {
            if (taskExecution.getOutput() != null) {
                deleteFileQuietly(() -> taskFileStorage.deleteTaskExecutionOutput(taskExecution.getOutput()));
            }

            deleteStackFiles(Validate.notNull(taskExecution.getId(), "id"));
        }

        deleteStackFiles(jobId);
    }

    private void deleteStackFiles(long stackId) {
        try {
            for (FileEntry fileEntry : contextService.getStackFileEntries(stackId)) {
                deleteFileQuietly(() -> taskFileStorage.deleteContextValue(fileEntry));
            }

            contextService.deleteStackContexts(stackId);
        } catch (UnsupportedOperationException exception) {
            log.debug("Context enumeration unsupported in this deployment; skipping context cleanup");
        }
    }

    private static void deleteFileQuietly(Runnable deleteAction) {
        try {
            deleteAction.run();
        } catch (RuntimeException exception) {
            log.warn("Failed to delete job file-storage blob", exception);
        }
    }

    @Override
    public void resumeApproval(long jobId, String uuid, boolean approved) {
        Map<String, Object> jobContext = new HashMap<>(
            taskFileStorage.readContextValue(contextService.peek(jobId, Context.Classname.JOB)));

        @SuppressWarnings("unchecked")
        Map<String, Boolean> approvalMap = new HashMap<>(
            (Map<String, Boolean>) jobContext.computeIfAbsent(APPROVALS, k -> new HashMap<>()));

        if (approvalMap.containsKey(uuid)) {
            throw new IllegalArgumentException("Approval already processed");
        }

        approvalMap.put(uuid, approved);

        jobContext.put(APPROVALS, approvalMap);

        contextService.push(
            jobId, Context.Classname.JOB, taskFileStorage.storeContextValue(jobId, Context.Classname.JOB, jobContext));

        jobService.resumeToStatusStarted(jobId);

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        if (!taskExecutions.isEmpty()) {
            TaskExecution currentTaskExecution = taskExecutions.getLast();

            currentTaskExecution.setEndDate(Instant.now());

            currentTaskExecution.setOutput(
                taskFileStorage.storeTaskExecutionOutput(
                    jobId, Objects.requireNonNull(currentTaskExecution.getId()), Map.of("approved", approved)));

            eventPublisher.publishEvent(new TaskExecutionCompleteEvent(currentTaskExecution));
        }
    }

    @Override
    public void resumeJob(long id) {
        eventPublisher.publishEvent(new ResumeJobEvent(id));
    }

    @Override
    public void resumeJob(long id, long taskExecutionId, @Nullable Map<String, ?> data) {
        eventPublisher.publishEvent(new ResumeJobEvent(id, taskExecutionId, data));
    }

    @Override
    public void stopJob(long id) {
        eventPublisher.publishEvent(new StopJobEvent(id));
    }
}
