/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.task.execution.repository.memory;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.task.execution.repository.TaskExecutionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryTaskExecutionRepository implements TaskExecutionRepository {

    private final Map<String, TaskExecution> taskExecutions = new HashMap<>();

    @Override
    public void create(TaskExecution taskExecution) {
        Assert.isTrue(
                taskExecutions.get(taskExecution.getId()) == null,
                "task execution " + taskExecution.getId() + " already exists");

        taskExecutions.put(taskExecution.getId(), taskExecution);
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByTaskNumber(String jobId) {
        return taskExecutions.values().stream()
                .filter(taskExecution -> Objects.equals(taskExecution.getJobId(), jobId))
                .toList();
    }

    @Override
    public List<TaskExecution> findAllByParentId(String parentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskExecution merge(TaskExecution taskExecution) {
        taskExecutions.put(taskExecution.getId(), taskExecution);
        return taskExecution;
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByCreateTime(String jobId) {
        return Collections.unmodifiableList(new ArrayList<>(taskExecutions.values()));
    }

    @Override
    public List<TaskExecution> findAllByJobIdsOrderByCreateTime(List<String> jobIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskExecution findOne(String id) {
        TaskExecution taskExecution = taskExecutions.get(id);

        Assert.notNull(taskExecution, "unknown task execution: " + id);

        return taskExecution;
    }
}
