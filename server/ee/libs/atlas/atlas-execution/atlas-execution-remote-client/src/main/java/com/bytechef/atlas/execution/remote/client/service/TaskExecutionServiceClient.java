
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

package com.bytechef.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class TaskExecutionServiceClient implements RemoteTaskExecutionService {

    private static final String EXECUTION_SERVICE_APP = "execution-service-app";
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
                .host(EXECUTION_SERVICE_APP)
                .path(TASK_EXECUTION_SERVICE + "/create")
                .build(),
            taskExecution, TaskExecution.class);
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(long jobId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_SERVICE_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-job-task-executions/{jobId}")
                .build(jobId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskExecution> getParentTaskExecutions(long parentId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_SERVICE_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-parent-task-executions/{parentId}")
                .build(parentId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public TaskExecution getTaskExecution(long id) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_SERVICE_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-task-execution/{id}")
                .build(id),
            TaskExecution.class);
    }

    @Override
    public TaskExecution update(TaskExecution taskExecution) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_SERVICE_APP)
                .path(TASK_EXECUTION_SERVICE + "/update")
                .build(),
            taskExecution, TaskExecution.class);
    }
}
