/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.execution.repository.memory;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.util.comparator.Comparators;

/**
 * @author Arik Cohen
 * @author Igor Beslic
 * @since Feb, 21 2020
 */
public class InMemoryTaskExecutionRepository implements TaskExecutionRepository {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Cache<Long, TaskExecution> TASK_EXECUTIONS =
        Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private static final Cache<Long, List<TaskExecution>> TASK_EXECUTIONS_BY_JOB_ID =
        Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Override
    public List<TaskExecution> findAllByJobIdOrderByTaskNumber(Long jobId) {
        Comparator<Object> comparable = Comparators.comparable();

        return TASK_EXECUTIONS_BY_JOB_ID.get(jobId, s -> new ArrayList<>())
            .stream()
            .sorted((o1, o2) -> comparable.compare(o1.getTaskNumber(), o2.getTaskNumber()))
            .toList();
    }

    @Override
    public List<TaskExecution> findAllByParentId(Long parentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByCreatedDate(Long jobId) {
        return TASK_EXECUTIONS_BY_JOB_ID.get(jobId, s -> new ArrayList<>())
            .stream()
            .toList();
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByIdDesc(Long jobId) {
        return TASK_EXECUTIONS_BY_JOB_ID.get(jobId, s -> new ArrayList<>())
            .stream()
            .sorted((taskExecution1, taskExecution2) -> {
                long diff =
                    Objects.requireNonNull(taskExecution1.getId()) - Objects.requireNonNull(taskExecution2.getId());

                return diff > 0 ? 1 : diff < 0 ? -1 : 0;
            })
            .toList();
    }

    @Override
    public Optional<TaskExecution> findById(long id) {
        TaskExecution taskExecution = TASK_EXECUTIONS.getIfPresent(id);

        return Optional.ofNullable(taskExecution);
    }

    @Override
    public Optional<TaskExecution> findByIdForUpdate(long id) {
        return findById(id);
    }

    @Override
    public TaskExecution save(TaskExecution taskExecution) {
        if (taskExecution.isNew()) {
            taskExecution.setId(Math.abs(Math.max(RANDOM.nextLong(), Long.MIN_VALUE + 1)));
        }

        try {
            TaskExecution clonedTaskExecution = taskExecution.clone();

            TASK_EXECUTIONS.put(taskExecution.getId(), clonedTaskExecution);

            synchronized (TASK_EXECUTIONS_BY_JOB_ID) {
                List<TaskExecution> taskExecutions = TASK_EXECUTIONS_BY_JOB_ID.get(
                    taskExecution.getJobId(), s -> new ArrayList<>());

                int index = taskExecutions.indexOf(clonedTaskExecution);

                if (index == -1) {
                    taskExecutions.add(clonedTaskExecution);
                } else {
                    taskExecutions.set(index, clonedTaskExecution);
                }

                TASK_EXECUTIONS_BY_JOB_ID.put(taskExecution.getJobId(), taskExecutions);
            }
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        return taskExecution;
    }
}
