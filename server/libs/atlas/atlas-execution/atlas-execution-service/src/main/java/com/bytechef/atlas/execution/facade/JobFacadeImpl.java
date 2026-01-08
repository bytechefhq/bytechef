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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
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
        Job job = jobService.create(jobParametersDTO, workflowService.getWorkflow(jobParametersDTO.getWorkflowId()));

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
        taskExecutionService.deleteJobTaskExecutions(id);

        jobService.deleteJob(id);
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
    public void stopJob(long id) {
        eventPublisher.publishEvent(new StopJobEvent(id));
    }
}
