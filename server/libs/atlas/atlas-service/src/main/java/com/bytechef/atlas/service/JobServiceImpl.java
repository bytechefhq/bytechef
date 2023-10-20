
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

package com.bytechef.atlas.service;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.error.ExecutionError;
import com.bytechef.atlas.repository.JobRepository;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final List<WorkflowRepository> workflowRepositories;

    @SuppressFBWarnings("EI2")
    public JobServiceImpl(JobRepository jobRepository, List<WorkflowRepository> workflowRepositories) {
        this.jobRepository = jobRepository;
        this.workflowRepositories = workflowRepositories;
    }

    @Override
    public Job create(@NonNull JobParameters jobParameters) {
        Assert.notNull(jobParameters, "'jobParameters' must not be null");

        String workflowId = jobParameters.getWorkflowId();

        Workflow workflow = CollectionUtils.getFirstFlat(
            workflowRepositories,
            workflowRepository -> CollectionUtils.stream(workflowRepository.findAll()),
            curWorkflow -> Objects.equals(workflowId, curWorkflow.getId()));

        Assert.notNull(workflow, String.format("Unknown workflow: %s", workflowId));

        ExecutionError executionError = workflow.getError();

        Assert.isNull(
            executionError,
            executionError == null
                ? ""
                : String.format("%s: %s", workflowId, executionError.getMessage()));

        validate(jobParameters, workflow);

        Job job = new Job();

        job.setInputs(jobParameters.getInputs());
        job.setLabel(jobParameters.getLabel() == null ? workflow.getLabel() : jobParameters.getLabel());
        job.setParentTaskExecutionId(jobParameters.getParentTaskExecutionId());
        job.setPriority(jobParameters.getPriority());
        job.setStatus(Job.Status.CREATED);
        job.setWebhooks(jobParameters.getWebhooks());
        job.setWorkflowId(workflow.getId());

        job = jobRepository.save(job);

        return job;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> fetchLatestJob() {
        return jobRepository.findLatestJob();
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
    public Job getJob(@NonNull long id) {
        return OptionalUtils.get(jobRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Job> getJobs(int pageNumber) {
        return jobRepository.findAll(PageRequest.of(pageNumber, JobRepository.DEFAULT_PAGE_SIZE));
    }

    @Override
    @Transactional(readOnly = true)
    public Job getTaskExecutionJob(long taskExecutionId) {
        return jobRepository.findByTaskExecutionId(taskExecutionId);
    }

    @Override
    public Job resumeToStatusStarted(long id) {
        Job job = OptionalUtils.get(jobRepository.findById(id));

        Assert.notNull(job, String.format("Unknown job %s", id));
        Assert.isTrue(job.getParentTaskExecutionId() == null, "Can't resume a subflow");
        Assert.isTrue(isRestartable(job), "can't restart job " + id + " as it is " + job.getStatus());

        job.setStatus(Job.Status.STARTED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Page<Job> searchJobs(
        String status, LocalDateTime startDate, LocalDateTime endDate, String workflowId, List<String> workflowIds,
        Integer pageNumber) {

        return jobRepository.findAll(
            status, startDate, endDate, workflowId, workflowIds,
            PageRequest.of(pageNumber, JobRepository.DEFAULT_PAGE_SIZE));
    }

    @Override
    public Job setStatusToStarted(long id) {
        Job job = OptionalUtils.get(jobRepository.findById(id));

        job.setCurrentTask(0);
        job.setStartDate(LocalDateTime.now());
        job.setStatus(Job.Status.STARTED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Job setStatusToStopped(long id) {
        Job job = OptionalUtils.get(jobRepository.findById(id));

        Assert.isTrue(
            job.getStatus() == Job.Status.STARTED,
            "Job id=" + id + " can not be stopped as it is " + job.getStatus());

        job.setStatus(Job.Status.STOPPED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Job update(@NonNull Job job) {
        Assert.notNull(job, "'job' must not be null");

        return jobRepository.save(job);
    }

    private boolean isRestartable(Job job) {
        return job.getStatus() == Job.Status.STOPPED || job.getStatus() == Job.Status.FAILED;
    }

    private void validate(JobParameters jobParameters, Workflow workflow) {
        // validate inputs

        Map<String, Object> inputs = jobParameters.getInputs();

        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Assert.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
            }
        }

        // validate webhooks

        for (Job.Webhook webhook : jobParameters.getWebhooks()) {
            Assert.notNull(webhook.type(), "must define 'type' on webhook");
            Assert.notNull(webhook.url(), "must define 'url' on webhook");
        }
    }
}
