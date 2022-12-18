
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

package com.bytechef.atlas.service.impl;

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.priority.Prioritizable;
import com.bytechef.atlas.repository.JobRepository;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.service.JobService;
import com.bytechef.commons.utils.MapUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Transactional
public class JobServiceImpl implements JobService {

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final List<WorkflowRepository> workflowRepositories;

    @SuppressFBWarnings("EI2")
    public JobServiceImpl(JobRepository jobRepository, List<WorkflowRepository> workflowRepositories) {
        this.jobRepository = jobRepository;
        this.workflowRepositories = workflowRepositories;
    }

    @Override
    public Job create(JobParameters jobParameters) {
        Assert.notNull(jobParameters, "jobParameters cannot be null.");

        String workflowId = jobParameters.getWorkflowId();

        Workflow workflow = workflowRepositories.stream()
            .map(workflowRepository -> workflowRepository.findById(workflowId))
            .findFirst()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .orElseThrow();

        Assert.notNull(workflow, String.format("Unknown workflow: %s", workflowId));

        ExecutionError executionError = workflow.getError();

        Assert.isNull(
            executionError,
            executionError != null
                ? String.format("%s: %s", workflowId, executionError.getMessage())
                : "");

        validate(jobParameters, workflow);

        Job job = new Job();

        job.setId(jobParameters.getJobId() == null ? null : jobParameters.getJobId());
        job.setInputs(jobParameters.getInputs());
        job.setNew(true);
        job.setLabel(jobParameters.getLabel() == null ? workflow.getLabel() : jobParameters.getLabel());
        job.setParentTaskExecutionId(jobParameters.getParentTaskExecutionId());
        job.setPriority(
            jobParameters.getPriority() == null
                ? Prioritizable.DEFAULT_PRIORITY
                : jobParameters.getPriority());
        job.setStatus(JobStatus.CREATED);
        job.setWebhooks(jobParameters.getWebhooks());
        job.setWorkflowId(workflow.getId());

        log.debug("Job {} started", job.getId());

        jobRepository.save(job);

        return job;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getJobs() {
        return StreamSupport.stream(jobRepository.findAll()
            .spliterator(), false)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Job getJob(String id) {
        Assert.notNull(id, "id cannot be null.");

        return jobRepository.findById(id)
            .orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Job> getJobs(int pageNumber) {
        return jobRepository.findAll(PageRequest.of(pageNumber, JobRepository.DEFAULT_PAGE_SIZE));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> fetchLatestJob() {
        return jobRepository.findLatestJob();
    }

    @Override
    @Transactional(readOnly = true)
    public Job getTaskExecutionJob(String taskExecutionId) {
        Assert.notNull(taskExecutionId, "taskExecutionId cannot be null.");

        return jobRepository.findByTaskExecutionId(taskExecutionId);
    }

    @Override
    public Job resume(String id) {
        Assert.notNull(id, "id cannot be null.");

        log.debug("Resuming job {}", id);

        Job job = jobRepository.findById(id)
            .orElseThrow();

        Assert.notNull(job, String.format("Unknown job %s", id));
        Assert.isTrue(job.getParentTaskExecutionId() == null, "Can't resume a subflow");
        Assert.isTrue(isRestartable(job), "can't restart job " + id + " as it is " + job.getStatus());

        job.setStatus(JobStatus.STARTED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Job start(String id) {
        Assert.notNull(id, "id cannot be null.");

        Job job = jobRepository.findById(id)
            .orElseThrow();

        job.setCurrentTask(0);
        job.setStartTime(new Date());
        job.setStatus(JobStatus.STARTED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Job stop(String id) {
        Assert.notNull(id, "id cannot be null.");

        Job job = jobRepository.findById(id)
            .orElseThrow();

        Assert.notNull(job, "Unknown job: " + id);
        Assert.isTrue(
            job.getStatus() == JobStatus.STARTED,
            "Job " + id + " can not be stopped as it is " + job.getStatus());

        Job simpleJob = new Job(job);

        simpleJob.setStatus(JobStatus.STOPPED);

        jobRepository.save(simpleJob);

        return simpleJob;
    }

    @Override
    public Job update(Job job) {
        Assert.notNull(job, "job cannot be null.");

        return jobRepository.save(job);
    }

    private boolean isRestartable(Job job) {
        return job.getStatus() == JobStatus.STOPPED || job.getStatus() == JobStatus.FAILED;
    }

    private void validate(JobParameters workflowParameters, Workflow workflow) {
        // validate inputs

        Map<String, Object> inputs = workflowParameters.getInputs();

        for (Map<String, Object> input : workflow.getInputs()) {
            if (MapUtils.getBoolean(input, WorkflowConstants.REQUIRED, false)) {
                Assert.isTrue(
                    inputs.containsKey(input.get(WorkflowConstants.NAME)),
                    "Missing required param: " + input.get("name"));
            }
        }

        // validate webhooks

        for (Map<String, Object> webhook : workflowParameters.getWebhooks()) {
            Assert.notNull(webhook.get(WorkflowConstants.TYPE), "must define 'type' on webhook");
            Assert.notNull(webhook.get(WorkflowConstants.URL), "must define 'url' on webhook");
        }
    }
}
