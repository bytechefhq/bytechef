/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.client.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteJobServiceClient implements JobService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String JOB_SERVICE = "/remote/job-service";
    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteJobServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public long countJobs(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, List<String> projectWorkflowIds) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Job create(JobParameters jobParameters, Workflow workflow) {
        return loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/create")
                .build(),
            new JobCreateRequest(jobParameters, workflow), Job.class);
    }

    @Override
    public Optional<Job> fetchLastJob() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job getJob(long id) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/get-job/{id}")
                .build(id),
            Job.class);
    }

    @Override
    public Page<Job> getJobsPage(int pageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job getTaskExecutionJob(long taskExecutionId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/get-task-execution-job/{taskExecutionId}")
                .build(taskExecutionId),
            Job.class);
    }

    @Override
    public List<Job> getJobs(
        String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Job> getJobsPage(
        String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds,
        Integer pageNumber) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Job resumeToStatusStarted(long id) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/resume-to-status-started/{id}")
                .build(id),
            null, Job.class);
    }

    @Override
    public Job setStatusToStarted(long id) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/set-status-to-started/{id}")
                .build(id),
            null, Job.class);
    }

    @Override
    public Job setStatusToStopped(long id) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/set-status-to-stopped/{id}")
                .build(id),
            null, Job.class);
    }

    @Override
    public Job update(Job job) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/update")
                .build(),
            job, Job.class);
    }

    private record JobCreateRequest(JobParameters jobParameters, Workflow workflow) {
    }
}
