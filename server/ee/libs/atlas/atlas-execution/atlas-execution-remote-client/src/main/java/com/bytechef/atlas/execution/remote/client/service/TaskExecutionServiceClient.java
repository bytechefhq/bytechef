
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
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class TaskExecutionServiceClient implements TaskExecutionService {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public TaskExecutionServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public TaskExecution create(TaskExecution taskExecution) {
        return loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/task-execution-service/create")
                .build(),
            taskExecution, TaskExecution.class);
    }

    @Override
    public TaskExecution getTaskExecution(long id) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/task-execution-service/get-task-execution/{id}")
                .build(id),
            TaskExecution.class);
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getJobsTaskExecutions(List<Long> jobIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getParentTaskExecutions(long parentId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/task-execution-service/get-parent-task-executions/{parentId}")
                .build(parentId),
            new ParameterizedTypeReference<List<TaskExecution>>() {});
    }

    @Override
    public TaskExecution update(TaskExecution taskExecution) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/task-execution-service/update")
                .build(),
            taskExecution, TaskExecution.class);
    }
}
