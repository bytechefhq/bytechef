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

package com.integri.atlas.engine.job.service;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.engine.DSL;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.job.JobStatus;
import com.integri.atlas.engine.job.SimpleJob;
import com.integri.atlas.engine.job.repository.JobRepository;
import com.integri.atlas.engine.priority.Prioritizable;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.engine.workflow.Workflow;
import com.integri.atlas.engine.workflow.repository.WorkflowRepository;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Component
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private static final String TAGS = "tags";
    private static final String WORKFLOW_ID = "workflowId";

    private final JobRepository jobRepository;
    private final WorkflowRepository workflowRepository;

    public JobService(JobRepository jobRepository, WorkflowRepository workflowRepository) {
        this.jobRepository = jobRepository;
        this.workflowRepository = workflowRepository;
    }

    public Job create(MapObject jobParams) {
        String workflowId = jobParams.getRequiredString(WORKFLOW_ID);

        Workflow workflow = workflowRepository.findOne(workflowId);

        Assert.notNull(workflow, String.format("Unkown workflow: %s", workflowId));
        Assert.isNull(
            workflow.getError(),
            workflow.getError() != null ? String.format("%s: %s", workflowId, workflow.getError().getMessage()) : null
        );

        validate(jobParams, workflow);

        MapObject inputs = MapObject.of(jobParams.getMap(DSL.INPUTS, Collections.EMPTY_MAP));
        List<Accessor> webhooks = jobParams.getList(DSL.WEBHOOKS, MapObject.class, Collections.EMPTY_LIST);
        List<String> tags = (List<String>) jobParams.get(TAGS);

        SimpleJob job = new SimpleJob();

        job.setId(UUIDGenerator.generate());
        job.setLabel(jobParams.getString(DSL.LABEL, workflow.getLabel()));
        job.setPriority(jobParams.getInteger(DSL.PRIORITY, Prioritizable.DEFAULT_PRIORITY));
        job.setWorkflowId(workflow.getId());
        job.setStatus(JobStatus.CREATED);
        job.setCreateTime(new Date());
        job.setParentTaskExecutionId((String) job.get(DSL.PARENT_TASK_EXECUTION_ID));
        job.setWebhooks(webhooks != null ? webhooks : Collections.EMPTY_LIST);
        job.setInputs(inputs);

        log.debug("Job {} started", job.getId());

        jobRepository.create(job);

        return job;
    }

    public Job start(Job job) {
        SimpleJob simpleJob = new SimpleJob(job);

        simpleJob.setStartTime(new Date());
        simpleJob.setStatus(JobStatus.STARTED);
        simpleJob.setCurrentTask(0);

        jobRepository.merge(simpleJob);

        return simpleJob;
    }

    public Job stop(String jobId) {
        Job job = jobRepository.getById(jobId);

        Assert.notNull(job, "Unknown job: " + jobId);
        Assert.isTrue(
            job.getStatus() == JobStatus.STARTED,
            "Job " + jobId + " can not be stopped as it is " + job.getStatus()
        );

        SimpleJob simpleJob = new SimpleJob(job);

        simpleJob.setStatus(JobStatus.STOPPED);

        jobRepository.merge(simpleJob);

        return simpleJob;
    }

    public Job resume(String jobId) {
        log.debug("Resuming job {}", jobId);

        Job job = jobRepository.getById(jobId);

        Assert.notNull(job, String.format("Unknown job %s", jobId));
        Assert.isTrue(job.getParentTaskExecutionId() == null, "Can't resume a subflow");
        Assert.isTrue(isRestartable(job), "can't restart job " + jobId + " as it is " + job.getStatus());

        SimpleJob simpleJob = new SimpleJob(job);

        simpleJob.setStatus(JobStatus.STARTED);

        jobRepository.merge(simpleJob);

        return simpleJob;
    }

    private boolean isRestartable(Job aJob) {
        return aJob.getStatus() == JobStatus.STOPPED || aJob.getStatus() == JobStatus.FAILED;
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
        List<Accessor> webhooks = aCreateJobParams.getList(DSL.WEBHOOKS, MapObject.class, Collections.EMPTY_LIST);

        for (Accessor webhook : webhooks) {
            Assert.notNull(webhook.getString(DSL.TYPE), "must define 'type' on webhook");
            Assert.notNull(webhook.getString(DSL.URL), "must define 'url' on webhook");
        }
    }
}
