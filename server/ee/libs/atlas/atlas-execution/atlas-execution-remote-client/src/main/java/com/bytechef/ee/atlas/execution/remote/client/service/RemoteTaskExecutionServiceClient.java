/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskExecutionServiceClient implements TaskExecutionService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String TASK_EXECUTION_SERVICE = "/remote/task-execution-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteTaskExecutionServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public TaskExecution create(TaskExecution taskExecution) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/create")
                .build(),
            taskExecution, TaskExecution.class);
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteJobTaskExecutions(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskExecution> fetchLastJobTaskExecution(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(long jobId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-job-task-executions/{jobId}")
                .build(jobId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskExecution> getParentTaskExecutions(long parentId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-parent-task-executions/{parentId}")
                .build(parentId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public TaskExecution getTaskExecution(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-task-execution/{id}")
                .build(id),
            TaskExecution.class);
    }

    @Override
    public TaskExecution getTaskExecutionForUpdate(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-task-execution-for-update/{id}")
                .build(id),
            TaskExecution.class);
    }

    @Override
    public TaskExecution update(TaskExecution taskExecution) {
        return loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/update")
                .build(),
            taskExecution, TaskExecution.class);
    }
}
