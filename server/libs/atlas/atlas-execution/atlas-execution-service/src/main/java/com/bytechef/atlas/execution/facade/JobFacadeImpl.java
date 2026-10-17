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

import com.bytechef.atlas.configuration.service.WorkflowService;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
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
    private static final AtomicBoolean CONTEXT_CLEANUP_UNSUPPORTED_LOGGED = new AtomicBoolean();
    private static final String WAIT_FOR_APPROVAL_TYPE_PREFIX = "waitForApproval/";

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
        Job job = jobService.create(jobParametersDTO, workflowService.getWorkflow(jobParametersDTO.getWorkflowId()));

        return startJob(job);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long createJob(JobParametersDTO jobParametersDTO, LongConsumer jobCreatedCallback) {
        Job job = jobService.create(
            jobParametersDTO, workflowService.getWorkflow(jobParametersDTO.getWorkflowId()), jobCreatedCallback);

        return startJob(job);
    }

    private long startJob(Job job) {
        long jobId = Validate.notNull(job.getId(), "id");

        log.debug("Job id={}, label='{}' created", jobId, job.getLabel());

        contextService.push(
            jobId, Context.Classname.JOB,
            taskFileStorage.storeContextValue(jobId, Context.Classname.JOB, job.getInputs()));

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

        Job job = jobService.getJob(id);

        eventPublisher.publishEvent(new DeleteJobEvent(id, job.getMetadata()));

        jobService.deleteJob(id);
    }

    private void deleteJobFiles(long jobId) {
        Job job = jobService.getJob(jobId);

        if (job.getOutputs() != null) {
            deleteFileQuietly(() -> taskFileStorage.deleteJobOutputs(job.getOutputs()));
        }

        for (TaskExecution taskExecution : taskExecutionService.getJobTaskExecutions(jobId)) {
            if (taskExecution.getOutput() != null) {
                deleteFileQuietly(() -> taskFileStorage.deleteTaskExecutionOutput(taskExecution.getOutput()));
            }

            deleteStackFiles(Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION);
        }

        deleteStackFiles(jobId, Context.Classname.JOB);
    }

    private void deleteStackFiles(long stackId, Context.Classname classname) {
        try {
            for (FileEntry fileEntry : contextService.getStackFileEntries(stackId, classname)) {
                deleteFileQuietly(() -> taskFileStorage.deleteContextValue(fileEntry));
            }

            contextService.deleteStackContexts(stackId, classname);
        } catch (UnsupportedOperationException exception) {
            if (CONTEXT_CLEANUP_UNSUPPORTED_LOGGED.compareAndSet(false, true)) {
                log.warn(
                    "Context enumeration is unsupported by the configured context repository; job context values " +
                        "are not deleted with their jobs");
            }
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
        Job job = jobService.getJob(jobId);

        if (job.getStatus() != Job.Status.STOPPED) {
            throw new IllegalStateException("Job %s is not waiting for an approval".formatted(jobId));
        }

        TaskExecution waitingTaskExecution = findWaitingApprovalTaskExecution(jobId)
            .orElseThrow(() -> new IllegalStateException("Job %s has no pending approval".formatted(jobId)));

        if (!jobService.tryClaimResume(job)) {
            throw new IllegalStateException("Job %s approval was already resolved".formatted(jobId));
        }

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

        waitingTaskExecution.setEndDate(Instant.now());

        waitingTaskExecution.setOutput(
            taskFileStorage.storeTaskExecutionOutput(
                jobId, Objects.requireNonNull(waitingTaskExecution.getId()), Map.of("approved", approved)));

        eventPublisher.publishEvent(new TaskExecutionCompleteEvent(waitingTaskExecution));
    }

    private Optional<TaskExecution> findWaitingApprovalTaskExecution(long jobId) {
        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        if (taskExecutions.isEmpty()) {
            return Optional.empty();
        }

        TaskExecution lastTaskExecution = taskExecutions.getLast();

        String type = lastTaskExecution.getType();

        if (lastTaskExecution.getStatus() != TaskExecution.Status.STARTED || type == null ||
            !type.startsWith(WAIT_FOR_APPROVAL_TYPE_PREFIX)) {

            return Optional.empty();
        }

        return Optional.of(lastTaskExecution);
    }

    @Override
    public void resumeJob(long id) {
        jobService.resumeToStatusStarted(id);

        eventPublisher.publishEvent(ResumeJobEvent.ofStartedJob(id));
    }

    @Override
    public void resumeJob(long id, long taskExecutionId, @Nullable Map<String, ?> data) {
        eventPublisher.publishEvent(new ResumeJobEvent(id, taskExecutionId, data));
    }

    @Override
    public void resumeClaimedJob(long id, @Nullable Long taskExecutionId, @Nullable Map<String, ?> data) {
        eventPublisher.publishEvent(ResumeJobEvent.ofStartedJob(id, taskExecutionId, data));
    }

    @Override
    public void stopJob(long id) {
        eventPublisher.publishEvent(new StopJobEvent(id));
    }
}
