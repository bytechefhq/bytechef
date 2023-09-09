
/*
 * Copyright 2021 <your company/name>.
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
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.execution.event.JobStatusEvent;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.execution.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class JobFacadeImpl implements JobFacade {

    private static final Logger logger = LoggerFactory.getLogger(JobFacadeImpl.class);

    private final ContextService contextService;
    private final EventPublisher eventPublisher;
    private final JobService jobService;
    private final MessageBroker messageBroker;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public JobFacadeImpl(
        ContextService contextService, EventPublisher eventPublisher, JobService jobService,
        MessageBroker messageBroker, WorkflowFileStorageFacade workflowFileStorageFacade,
        WorkflowService workflowService) {

        this.contextService = contextService;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.messageBroker = messageBroker;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
        this.workflowService = workflowService;
    }

    // Propagation.NEVER is set because of sending job messages via queue in monolith mode, where it can happen
    // the case where a job is finished and the completion task executed, but the transaction is not yet committed and
    // the job id is missing.
    @Override
    @Transactional(propagation = Propagation.NEVER)
    @SuppressFBWarnings("NP")
    public long createJob(JobParameters jobParameters) {
        Job job = jobService.create(jobParameters, workflowService.getWorkflow(jobParameters.getWorkflowId()));

        Assert.notNull(job.getId(), "'job.id' must not be null");

        contextService.push(
            job.getId(), Context.Classname.JOB,
            workflowFileStorageFacade.storeContextValue(job.getId(), Context.Classname.JOB, job.getInputs()));

        eventPublisher.publishEvent(new JobStatusEvent(job.getId(), job.getStatus()));

        messageBroker.send(TaskMessageRoute.JOBS_START, job.getId());

        logger.debug("Job id={}, label='{}' created", job.getId(), job.getLabel());

        return job.getId();
    }

    @Override
    public void restartJob(Long id) {
        messageBroker.send(TaskMessageRoute.JOBS_RESUME, id);
    }

    @Override
    public void stopJob(Long id) {
        messageBroker.send(TaskMessageRoute.JOBS_STOP, id);
    }
}
