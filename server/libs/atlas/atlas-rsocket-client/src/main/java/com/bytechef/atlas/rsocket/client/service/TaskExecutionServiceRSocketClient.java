
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

package com.bytechef.atlas.rsocket.client.service;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.TaskExecutionService;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TaskExecutionServiceRSocketClient implements TaskExecutionService {

    private final RSocketRequester rSocketRequester;

    public TaskExecutionServiceRSocketClient(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public TaskExecution create(TaskExecution taskExecution) {
        return rSocketRequester
            .route("createTaskExecution")
            .data(taskExecution)
            .retrieveMono(TaskExecution.class)
            .block();
    }

    @Override
    public TaskExecution getTaskExecution(long id) {
        return rSocketRequester
            .route("getTaskExecution")
            .data(id)
            .retrieveMono(TaskExecution.class)
            .block();
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(long jobId) {
        return rSocketRequester
            .route("getJobTaskExecutions")
            .data(jobId)
            .retrieveMono(new ParameterizedTypeReference<List<TaskExecution>>() {})
            .block();
    }

    @Override
    public List<TaskExecution> getJobsTaskExecutions(List<Long> jobIds) {
        return rSocketRequester
            .route("getJobsTaskExecutions")
            .data(jobIds)
            .retrieveMono(new ParameterizedTypeReference<List<TaskExecution>>() {})
            .block();
    }

    @Override
    public List<TaskExecution> getParentTaskExecutions(long parentId) {
        return rSocketRequester
            .route("getParentTaskExecutions")
            .data(parentId)
            .retrieveMono(new ParameterizedTypeReference<List<TaskExecution>>() {})
            .block();
    }

    @Override
    public TaskExecution update(TaskExecution taskExecution) {
        return rSocketRequester
            .route("updateTaskExecution")
            .data(taskExecution)
            .retrieveMono(TaskExecution.class)
            .block();
    }
}
