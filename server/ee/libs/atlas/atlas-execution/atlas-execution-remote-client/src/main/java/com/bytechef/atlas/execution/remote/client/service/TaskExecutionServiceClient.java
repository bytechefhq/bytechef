/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class TaskExecutionServiceClient implements TaskExecutionService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String TASK_EXECUTION_SERVICE = "/remote/task-execution-service";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public TaskExecutionServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public TaskExecution create(TaskExecution taskExecution) {
        return loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/create")
                .build(),
            taskExecution, TaskExecution.class);
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(long jobId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-job-task-executions/{jobId}")
                .build(jobId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskExecution> getParentTaskExecutions(long parentId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-parent-task-executions/{parentId}")
                .build(parentId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public TaskExecution getTaskExecution(long id) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-task-execution/{id}")
                .build(id),
            TaskExecution.class);
    }

    @Override
    public TaskExecution update(TaskExecution taskExecution) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/update")
                .build(),
            taskExecution, TaskExecution.class);
    }
}
