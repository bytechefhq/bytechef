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

package com.bytechef.atlas.execution.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    @SuppressFBWarnings("EI2")
    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public Job create(@NonNull JobParameters jobParameters, Workflow workflow) {
        Validate.notNull(jobParameters, "'jobParameters' must not be null");

        String workflowId = jobParameters.getWorkflowId();

        Validate.notNull(workflow, String.format("Unknown workflow: %s", workflowId));

        validate(jobParameters, workflow);

        Job job = getJob(jobParameters, workflow);

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
    public Job getJob(long id) {
        return OptionalUtils.get(jobRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Job> getJobsPage(int pageNumber) {
        return jobRepository.findAll(PageRequest.of(pageNumber, JobRepository.DEFAULT_PAGE_SIZE));
    }

    @Override
    public Page<Job> getJobsPage(
        String status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, int type,
        List<String> workflowIds, int pageNumber) {

        PageRequest pageRequest = PageRequest.of(pageNumber, JobRepository.DEFAULT_PAGE_SIZE);

        return jobRepository.findAll(status, startDate, endDate, instanceId, type, workflowIds, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Job getTaskExecutionJob(long taskExecutionId) {
        return jobRepository.findByTaskExecutionId(taskExecutionId);
    }

    @Override
    public Job resumeToStatusStarted(long id) {
        Job job = OptionalUtils.get(jobRepository.findById(id));

        Validate.notNull(job, String.format("Unknown job %s", id));
        Validate.isTrue(job.getParentTaskExecutionId() == null, "Can't resume a subflow");
        Validate.isTrue(isRestartable(job), "can't resume job " + id + " as it is " + job.getStatus());

        job.setStatus(Job.Status.STARTED);

        jobRepository.save(job);

        return job;
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

        Validate.isTrue(
            job.getStatus() == Job.Status.STARTED,
            "Job id=" + id + " can not be stopped as it is " + job.getStatus());

        job.setStatus(Job.Status.STOPPED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Job update(@NonNull Job job) {
        Validate.notNull(job, "'job' must not be null");

        return jobRepository.save(job);
    }

    private static boolean isRestartable(Job job) {
        return job.getStatus() == Job.Status.STOPPED || job.getStatus() == Job.Status.FAILED;
    }

    private static Job getJob(JobParameters jobParameters, Workflow workflow) {
        Job job = new Job();

        job.setInputs(jobParameters.getInputs());
        job.setLabel(jobParameters.getLabel() == null ? workflow.getLabel() : jobParameters.getLabel());
        job.setMetadata(jobParameters.getMetadata());
        job.setParentTaskExecutionId(jobParameters.getParentTaskExecutionId());
        job.setPriority(jobParameters.getPriority());
        job.setStatus(Job.Status.CREATED);
        job.setWebhooks(jobParameters.getWebhooks());
        job.setWorkflowId(workflow.getId());

        return job;
    }

    private static void validate(JobParameters jobParameters, Workflow workflow) {
        // validate inputs

        Map<String, Object> inputs = jobParameters.getInputs();

        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Validate.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
            }
        }

        // validate webhooks

        for (Job.Webhook webhook : jobParameters.getWebhooks()) {
            Validate.notNull(webhook.type(), "must define 'type' on webhook");
            Validate.notNull(webhook.url(), "must define 'url' on webhook");
        }
    }
}
