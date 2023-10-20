
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

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.dto.JobParametersDTO;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.repository.JobRepository;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapValueUtils;
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
    public Job create(@NonNull JobParametersDTO jobParametersDTO) {
        Assert.notNull(jobParametersDTO, "'jobParameters' must not be null");

        String workflowId = jobParametersDTO.getWorkflowId();

        Workflow workflow = workflowRepositories.stream()
            .flatMap(workflowRepository -> CollectionUtils.stream(workflowRepository.findAll()))
            .filter(curWorkflow -> Objects.equals(workflowId, curWorkflow.getId()))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);

        Assert.notNull(workflow, String.format("Unknown workflow: %s", workflowId));

        ExecutionError executionError = workflow.getError();

        Assert.isNull(
            executionError,
            executionError == null
                ? ""
                : String.format("%s: %s", workflowId, executionError.getMessage()));

        validate(jobParametersDTO, workflow);

        Job job = new Job();

        job.setInputs(jobParametersDTO.getInputs());
        job.setLabel(jobParametersDTO.getLabel() == null ? workflow.getLabel() : jobParametersDTO.getLabel());
        job.setParentTaskExecutionId(jobParametersDTO.getParentTaskExecutionId());
        job.setPriority(jobParametersDTO.getPriority());
        job.setStatus(Job.Status.CREATED);
        job.setWebhooks(jobParametersDTO.getWebhooks());
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
    public Job resume(long id) {
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
//        return jobRepository.findAll(PageRequest.of(pageNumber, JobRepository.DEFAULT_PAGE_SIZE));
    }

    @Override
    public Job start(long id) {
        Job job = OptionalUtils.get(jobRepository.findById(id));

        job.setCurrentTask(0);
        job.setStartDate(LocalDateTime.now());
        job.setStatus(Job.Status.STARTED);

        jobRepository.save(job);

        return job;
    }

    @Override
    public Job stop(long id) {
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

    private void validate(JobParametersDTO workflowParameters, Workflow workflow) {
        // validate inputs

        Map<String, Object> inputs = workflowParameters.getInputs();

        for (Map<String, Object> input : workflow.getInputs()) {
            if (MapValueUtils.getBoolean(input, WorkflowConstants.REQUIRED, false)) {
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
