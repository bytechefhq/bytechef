
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
import com.bytechef.atlas.dto.JobParametersDTO;
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

    private static final String WORKFLOW_ID = "workflowId";

    private final JobRepository jobRepository;
    private final WorkflowRepository workflowRepository;

    @SuppressFBWarnings("EI2")
    public JobServiceImpl(JobRepository jobRepository, WorkflowRepository workflowRepository) {
        this.jobRepository = jobRepository;
        this.workflowRepository = workflowRepository;
    }

    @Override
        String workflowId = jobParametersDTO.getWorkflowId();
    public Job create(JobParameters jobParameters) {

        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow();

        Assert.notNull(workflow, String.format("Unknown workflow: %s", workflowId));
        Assert.isNull(
            workflow.getError(),
            workflow.getError() != null
                ? String.format(
                    "%s: %s", workflowId, workflow.getError()
                        .getMessage())
                : "");

        validate(jobParametersDTO, workflow);

        Job job = new Job();

        job.setId(jobParametersDTO.getJobId() == null ? null : jobParametersDTO.getJobId());
        job.setInputs(jobParametersDTO.getInputs());
        job.setNew(true);
        job.setLabel(jobParametersDTO.getLabel() == null ? workflow.getLabel() : jobParametersDTO.getLabel());
        job.setParentTaskExecutionId(jobParametersDTO.getParentTaskExecutionId());
        job.setPriority(
            jobParametersDTO.getPriority() == null
                ? Prioritizable.DEFAULT_PRIORITY
                : jobParametersDTO.getPriority());
        job.setStatus(JobStatus.CREATED);
        job.setWebhooks(jobParametersDTO.getWebhooks());
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
        return jobRepository.findByTaskExecutionId(taskExecutionId);
    }

    @Override
    public Job resume(String jobId) {
        log.debug("Resuming job {}", jobId);

        Job job = jobRepository.findById(jobId)
            .orElseThrow();

        Assert.notNull(job, String.format("Unknown job %s", jobId));
        Assert.isTrue(job.getParentTaskExecutionId() == null, "Can't resume a subflow");
        Assert.isTrue(isRestartable(job), "can't restart job " + jobId + " as it is " + job.getStatus());

        job.setStatus(JobStatus.STARTED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Job start(String jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow();

        job.setCurrentTask(0);
        job.setStartTime(new Date());
        job.setStatus(JobStatus.STARTED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Job stop(String jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow();

        Assert.notNull(job, "Unknown job: " + jobId);
        Assert.isTrue(
            job.getStatus() == JobStatus.STARTED,
            "Job " + jobId + " can not be stopped as it is " + job.getStatus());

        Job simpleJob = new Job(job);

        simpleJob.setStatus(JobStatus.STOPPED);

        jobRepository.save(simpleJob);

        return simpleJob;
    }

    @Override
    public Job update(Job job) {
        return jobRepository.save(job);
    }

    private boolean isRestartable(Job aJob) {
        return aJob.getStatus() == JobStatus.STOPPED || aJob.getStatus() == JobStatus.FAILED;
    }

    private void validate(JobParametersDTO workflowParameters, Workflow workflow) {
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
