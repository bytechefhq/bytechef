
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

package com.bytechef.atlas.execution.repository.memory;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryTaskExecutionRepository implements TaskExecutionRepository {

    private static final Random RANDOM = new Random();

    private final Map<Long, TaskExecution> taskExecutions = new HashMap<>();

    @Override
    public List<TaskExecution> findAllByJobIdOrderByTaskNumber(Long jobId) {
        return taskExecutions.values()
            .stream()
            .filter(taskExecution -> Objects.equals(taskExecution.getJobId(), jobId))
            .toList();
    }

    @Override
    public List<TaskExecution> findAllByParentId(Long parentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByCreatedDate(Long jobId) {
        return taskExecutions.values()
            .stream()
            .filter(taskExecution -> Objects.equals(taskExecution.getJobId(), jobId))
            .toList();
    }

    @Override
    public List<TaskExecution> findAllByJobIdInOrderByCreatedDate(List<Long> jobIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskExecution> findById(long id) {
        TaskExecution taskExecution = taskExecutions.get(id);

        return Optional.ofNullable(taskExecution);
    }

    @Override
    public Optional<TaskExecution> findByIdForUpdate(long id) {
        return findById(id);
    }

    @Override
    public TaskExecution save(TaskExecution taskExecution) {
        if (taskExecution.isNew()) {
            taskExecution.setId(RANDOM.nextLong());
        }

        taskExecutions.put(taskExecution.getId(), taskExecution);

        return taskExecution;
    }
}
