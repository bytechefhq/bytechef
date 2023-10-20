/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.coordinator.job.SimpleJob;
import com.integri.atlas.engine.coordinator.job.executor.JobExecutor;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.coordinator.workflow.Workflow;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowRepository;
import com.integri.atlas.engine.core.Accessor;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.error.ErrorHandler;
import com.integri.atlas.engine.core.error.ErrorObject;
import com.integri.atlas.engine.core.error.Errorable;
import com.integri.atlas.engine.core.event.EventPublisher;
import com.integri.atlas.engine.core.event.Events;
import com.integri.atlas.engine.core.event.WorkflowEvent;
import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.message.broker.Queues;
import com.integri.atlas.engine.core.priority.Prioritizable;
import com.integri.atlas.engine.core.task.CancelTask;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * The central class responsible for coordinating
 * and executing jobs.
 *
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public class CoordinatorImpl implements Coordinator {

    private WorkflowRepository workflowRepository;
    private JobRepository jobRepository;
    private TaskExecutionRepository jobTaskRepository;
    private EventPublisher eventPublisher;
    private ContextRepository contextRepository;
    private TaskDispatcher taskDispatcher;
    private ErrorHandler errorHandler;
    private TaskCompletionHandler taskCompletionHandler;
    private JobExecutor jobExecutor;
    private MessageBroker messageBroker;

    private static final String WORKFLOW_ID = "workflowId";
    private static final String TAGS = "tags";
    private static final String INPUTS = "inputs";
    private static final String WEBHOOKS = "webhooks";

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Starts a job instance.
     *
     * @param aJobParams
     *          The Key-Value map representing the job
     *          parameters
     * @return Job
     *           The instance of the Job
     */
    @Override
    public Job create(Map<String, Object> aJobParams) {
        Assert.notNull(aJobParams, "request can't be null");
        MapObject jobParams = MapObject.of(aJobParams);
        String workflowId = jobParams.getRequiredString(WORKFLOW_ID);
        Workflow workflow = workflowRepository.findOne(workflowId);
        Assert.notNull(workflow, String.format("Unkown workflow: %s", workflowId));
        Assert.isNull(
            workflow.getError(),
            workflow.getError() != null ? String.format("%s: %s", workflowId, workflow.getError().getMessage()) : null
        );

        validate(jobParams, workflow);

        MapObject inputs = MapObject.of(jobParams.getMap(INPUTS, Collections.EMPTY_MAP));
        List<Accessor> webhooks = jobParams.getList(WEBHOOKS, MapObject.class, Collections.EMPTY_LIST);
        List<String> tags = (List<String>) aJobParams.get(TAGS);

        SimpleJob job = new SimpleJob();
        job.setId(UUIDGenerator.generate());
        job.setLabel(jobParams.getString(DSL.LABEL, workflow.getLabel()));
        job.setPriority(jobParams.getInteger(DSL.PRIORITY, Prioritizable.DEFAULT_PRIORITY));
        job.setWorkflowId(workflow.getId());
        job.setStatus(JobStatus.CREATED);
        job.setCreateTime(new Date());
        job.setParentTaskExecutionId((String) aJobParams.get(DSL.PARENT_TASK_EXECUTION_ID));
        job.setWebhooks(webhooks != null ? webhooks : Collections.EMPTY_LIST);
        job.setInputs(inputs);
        log.debug("Job {} started", job.getId());
        jobRepository.create(job);

        MapContext context = new MapContext(jobParams.getMap(INPUTS, Collections.EMPTY_MAP));
        contextRepository.push(job.getId(), context);

        eventPublisher.publishEvent(
            WorkflowEvent.of(Events.JOB_STATUS, "jobId", job.getId(), "status", job.getStatus())
        );

        messageBroker.send(Queues.JOBS, job);

        return job;
    }

    @Override
    public void start(Job aJob) {
        SimpleJob job = new SimpleJob(aJob);
        job.setStartTime(new Date());
        job.setStatus(JobStatus.STARTED);
        job.setCurrentTask(0);
        jobRepository.merge(job);
        jobExecutor.execute(job);
        eventPublisher.publishEvent(
            WorkflowEvent.of(Events.JOB_STATUS, "jobId", aJob.getId(), "status", job.getStatus())
        );
    }

    private void validate(MapObject aCreateJobParams, Workflow aWorkflow) {
        // validate inputs
        Map<String, Object> inputs = aCreateJobParams.getMap(DSL.INPUTS, Collections.EMPTY_MAP);
        List<Accessor> input = aWorkflow.getInputs();
        for (Accessor in : input) {
            if (in.getBoolean(DSL.REQUIRED, false)) {
                Assert.isTrue(inputs.containsKey(in.get(DSL.NAME)), "Missing required param: " + in.get("name"));
            }
        }
        // validate webhooks
        List<Accessor> webhooks = aCreateJobParams.getList(WEBHOOKS, MapObject.class, Collections.EMPTY_LIST);
        for (Accessor webhook : webhooks) {
            Assert.notNull(webhook.getString(DSL.TYPE), "must define 'type' on webhook");
            Assert.notNull(webhook.getString(DSL.URL), "must define 'url' on webhook");
        }
    }

    /**
     * Stop a running job.
     *
     * @param aJobId
     *          The id of the job to stop
     *
     * @return The stopped {@link Job}
     */
    public Job stop(String aJobId) {
        Job job = jobRepository.getById(aJobId);
        Assert.notNull(job, "Unknown job: " + aJobId);
        Assert.isTrue(
            job.getStatus() == JobStatus.STARTED,
            "Job " + aJobId + " can not be stopped as it is " + job.getStatus()
        );
        SimpleJob mjob = new SimpleJob(job);
        mjob.setStatus(JobStatus.STOPPED);
        jobRepository.merge(mjob);
        eventPublisher.publishEvent(
            WorkflowEvent.of(Events.JOB_STATUS, "jobId", job.getId(), "status", job.getStatus())
        );
        if (mjob.getExecution().size() > 0) {
            SimpleTaskExecution currentTask = SimpleTaskExecution.of(
                job.getExecution().get(job.getExecution().size() - 1)
            );
            currentTask.setStatus(TaskStatus.CANCELLED);
            currentTask.setEndTime(new Date());
            jobTaskRepository.merge(currentTask);
            taskDispatcher.dispatch(new CancelTask(currentTask.getJobId(), currentTask.getId()));
        }
        return mjob;
    }

    /**
     * Resume a stopped or failed job.
     *
     * @param aJobId
     *          The id of the job to resume.
     * @return The resumed job
     */
    @Override
    public Job resume(String aJobId) {
        log.debug("Resuming job {}", aJobId);
        Job job = jobRepository.getById(aJobId);
        Assert.notNull(job, String.format("Unknown job %s", aJobId));
        Assert.isTrue(job.getParentTaskExecutionId() == null, "Can't resume a subflow");
        Assert.isTrue(isRestartable(job), "can't restart job " + aJobId + " as it is " + job.getStatus());
        SimpleJob mjob = new SimpleJob(job);
        mjob.setStatus(JobStatus.STARTED);
        jobRepository.merge(mjob);
        jobExecutor.execute(mjob);
        return mjob;
    }

    private boolean isRestartable(Job aJob) {
        return aJob.getStatus() == JobStatus.STOPPED || aJob.getStatus() == JobStatus.FAILED;
    }

    /**
     * Complete a task of a given job.
     *
     * @param aTask
     *          The task to complete.
     */
    public void complete(TaskExecution aTask) {
        try {
            taskCompletionHandler.handle(aTask);
        } catch (Exception e) {
            SimpleTaskExecution exec = SimpleTaskExecution.of(aTask);
            exec.setError(new ErrorObject(e.getMessage(), ExceptionUtils.getStackFrames(e)));
            handleError(exec);
        }
    }

    /**
     * Handle an application error.
     *
     * @param aErrorable
     *          The erring message.
     */
    public void handleError(Errorable aErrorable) {
        errorHandler.handle(aErrorable);
    }

    public void setContextRepository(ContextRepository aContextRepository) {
        contextRepository = aContextRepository;
    }

    public void setEventPublisher(EventPublisher aEventPublisher) {
        eventPublisher = aEventPublisher;
    }

    public void setJobRepository(JobRepository aJobRepository) {
        jobRepository = aJobRepository;
    }

    public void setTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        taskDispatcher = aTaskDispatcher;
    }

    public void setWorkflowRepository(WorkflowRepository aWorkflowRepository) {
        workflowRepository = aWorkflowRepository;
    }

    public void setJobTaskRepository(TaskExecutionRepository aJobTaskRepository) {
        jobTaskRepository = aJobTaskRepository;
    }

    public void setErrorHandler(ErrorHandler aErrorHandler) {
        errorHandler = aErrorHandler;
    }

    public void setTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
        taskCompletionHandler = aTaskCompletionHandler;
    }

    public void setJobExecutor(JobExecutor aJobExecutor) {
        jobExecutor = aJobExecutor;
    }

    public void setMessageBroker(MessageBroker aMessageBroker) {
        messageBroker = aMessageBroker;
    }
}
