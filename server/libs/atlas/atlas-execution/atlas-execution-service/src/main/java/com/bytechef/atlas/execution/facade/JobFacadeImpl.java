/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private static final Logger logger = LoggerFactory.getLogger(JobFacadeImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ContextService contextService;
    private final JobService jobService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public JobFacadeImpl(
        ApplicationEventPublisher eventPublisher, ContextService contextService, JobService jobService,
        TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this.eventPublisher = eventPublisher;
        this.contextService = contextService;
        this.jobService = jobService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    // Propagation.NEVER is set because of sending job messages via queue in monolith mode, where it can happen
    // the case where a job is finished and the completion task executed, but the transaction is not yet committed and
    // the job id is missing.
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long createJob(JobParameters jobParameters) {
        Job job = jobService.create(jobParameters, workflowService.getWorkflow(jobParameters.getWorkflowId()));

        long jobId = Validate.notNull(job.getId(), "id");

        logger.debug("Job id={}, label='{}' created", jobId, job.getLabel());

        contextService.push(
            jobId, Context.Classname.JOB,
            taskFileStorage.storeContextValue(jobId, Context.Classname.JOB, job.getInputs()));

        eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, job.getStatus()));
        eventPublisher.publishEvent(new StartJobEvent(jobId));

        return jobId;
    }

    @Override
    public void restartJob(Long id) {
        eventPublisher.publishEvent(new ResumeJobEvent(id));
    }

    @Override
    public void stopJob(Long id) {
        eventPublisher.publishEvent(new StopJobEvent(id));
    }
}
