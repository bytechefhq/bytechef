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
import com.bytechef.commons.restclient.LoadBalancedRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteJobServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public Job create(JobParameters jobParameters, Workflow workflow) {
        return loadBalancedRestClient.post(
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
    public Optional<Job> fetchLastWorkflowJob(String workflowId) {
        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(JOB_SERVICE + "/fetch-last-workflow-job/{workflowId}")
                    .build(workflowId),
                Job.class));
    }

    @Override
    public Job getJob(long id) {
        return loadBalancedRestClient.get(
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
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/get-task-execution-job/{taskExecutionId}")
                .build(taskExecutionId),
            Job.class);
    }

    @Override
    public Job resumeToStatusStarted(long id) {
        return loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/resume-to-status-started/{id}")
                .build(id),
            null, Job.class);
    }

    @Override
    public Job setStatusToStarted(long id) {
        return loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/set-status-to-started/{id}")
                .build(id),
            null, Job.class);
    }

    @Override
    public Job setStatusToStopped(long id) {
        return loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/set-status-to-stopped/{id}")
                .build(id),
            null, Job.class);
    }

    @Override
    public Job update(Job job) {
        return loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_SERVICE + "/update")
                .build(),
            job, Job.class);
    }

    private record JobCreateRequest(JobParameters jobParameters, Workflow workflow) {
    }
}
