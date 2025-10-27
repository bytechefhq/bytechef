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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.execution.repository.memory;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.comparator.Comparators;

/**
 * @author Arik Cohen
 * @author Igor Beslic
 * @since Feb, 21 2020
 */
public class InMemoryTaskExecutionRepository implements TaskExecutionRepository {

    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final String JOB_TASK_EXECUTIONS_CACHE =
        InMemoryTaskExecutionRepository.class.getName() + ".jobTaskExecutions";
    private static final String TASK_EXECUTION_CACHE =
        InMemoryTaskExecutionRepository.class.getName() + ".taskExecution";
    private static final String PARENT_TASK_EXECUTIONS_CACHE =
        InMemoryTaskExecutionRepository.class.getName() + ".parentTaskExecutions";

    private final CacheManager cacheManager;

    public InMemoryTaskExecutionRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void deleteById(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByTaskNumber(long jobId) {
        Comparator<Object> comparable = Comparators.comparable();

        Cache cache = Objects.requireNonNull(cacheManager.getCache(JOB_TASK_EXECUTIONS_CACHE));

        return Objects
            .requireNonNull(cache.get(TenantCacheKeyUtils.getKey(jobId), () -> new ArrayList<TaskExecution>()))
            .stream()
            .sorted((o1, o2) -> comparable.compare(o1.getTaskNumber(), o2.getTaskNumber()))
            .toList();
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByCreatedDate(long jobId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(JOB_TASK_EXECUTIONS_CACHE));

        return cache.get(TenantCacheKeyUtils.getKey(jobId), ArrayList::new);
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByIdDesc(long jobId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(JOB_TASK_EXECUTIONS_CACHE));

        return Objects
            .requireNonNull(cache.get(TenantCacheKeyUtils.getKey(jobId), () -> new ArrayList<TaskExecution>()))
            .stream()
            .sorted((taskExecution1, taskExecution2) -> {
                long diff =
                    Objects.requireNonNull(taskExecution1.getId()) - Objects.requireNonNull(taskExecution2.getId());

                return diff > 0 ? 1 : diff < 0 ? -1 : 0;
            })
            .toList();
    }

    @Override
    public List<TaskExecution> findAllByParentIdOrderByTaskNumber(long parentId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(PARENT_TASK_EXECUTIONS_CACHE));

        return Objects
            .requireNonNull(cache.get(TenantCacheKeyUtils.getKey(parentId), () -> new ArrayList<TaskExecution>()))
            .stream()
            .sorted((o1, o2) -> Comparators.comparable()
                .compare(o1.getTaskNumber(), o2.getTaskNumber()))
            .toList();
    }

    @Override
    public Optional<TaskExecution> findById(long id) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));

        TaskExecution taskExecution = cache.get(TenantCacheKeyUtils.getKey(id), TaskExecution.class);

        return Optional.ofNullable(taskExecution);
    }

    @Override
    public Optional<TaskExecution> findByIdForUpdate(long id) {
        return findById(id);
    }

    @Override
    public Optional<TaskExecution> findLastByJobId(long jobId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(JOB_TASK_EXECUTIONS_CACHE));

        List<TaskExecution> taskExecutions = Validate.notNull(
            cache.get(TenantCacheKeyUtils.getKey(jobId), ArrayList::new), "taskExecutions");

        if (taskExecutions.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(taskExecutions.getLast());
        }
    }

    @Override
    public TaskExecution save(TaskExecution taskExecution) {
        if (taskExecution.isNew()) {
            taskExecution.setId(Math.abs(Math.max(RandomUtils.nextLong(), Long.MIN_VALUE + 1)));
        }

        try {
            TaskExecution clonedTaskExecution = taskExecution.clone();

            Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));

            cache.put(TenantCacheKeyUtils.getKey(taskExecution.getId()), clonedTaskExecution);

            try {
                LOCK.lock();

                cache = Objects.requireNonNull(cacheManager.getCache(JOB_TASK_EXECUTIONS_CACHE));
                String key = TenantCacheKeyUtils.getKey(taskExecution.getJobId());

                List<TaskExecution> taskExecutions = Objects.requireNonNull(cache.get(key, ArrayList::new));

                int index = taskExecutions.indexOf(clonedTaskExecution);

                if (index == -1) {
                    taskExecutions.add(clonedTaskExecution);
                } else {
                    taskExecutions.set(index, clonedTaskExecution);
                }

                cache.put(key, taskExecutions);

                if (taskExecution.getParentId() != null) {
                    cache = Objects.requireNonNull(cacheManager.getCache(PARENT_TASK_EXECUTIONS_CACHE));
                    key = TenantCacheKeyUtils.getKey(taskExecution.getParentId());

                    taskExecutions = Objects.requireNonNull(cache.get(key, ArrayList::new));

                    index = taskExecutions.indexOf(clonedTaskExecution);

                    if (index == -1) {
                        taskExecutions.add(clonedTaskExecution);
                    } else {
                        taskExecutions.set(index, clonedTaskExecution);
                    }

                    cache.put(key, taskExecutions);
                }
            } finally {
                LOCK.unlock();
            }
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        return taskExecution;
    }
}
