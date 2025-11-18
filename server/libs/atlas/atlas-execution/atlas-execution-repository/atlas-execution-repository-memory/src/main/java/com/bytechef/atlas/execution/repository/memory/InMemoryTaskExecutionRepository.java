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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.comparator.Comparators;

/**
 * @author Arik Cohen
 * @author Igor Beslic
 * @since Feb, 21 2020
 */
public class InMemoryTaskExecutionRepository implements TaskExecutionRepository {

    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private static final String TASK_EXECUTION_CACHE = InMemoryTaskExecutionRepository.class.getName() +
        ".taskExecution";

    private final CacheManager cacheManager;

    public InMemoryTaskExecutionRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void deleteById(long id) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));

        Store store = Objects.requireNonNull(cache.get(getStoreKey(), Store::new));

        TaskExecution existingTaskExecution = store.byId.get(id);

        if (existingTaskExecution == null) {
            return;
        }

        String jobKey = getJobLockKey(Objects.requireNonNull(existingTaskExecution.getJobId()));
        String parentKey = getParentLockKey(existingTaskExecution.getParentId());

        // Acquire locks in deterministic order to avoid deadlocks
        String firstKey = jobKey;
        String secondKey = parentKey;

        if (secondKey != null && firstKey.compareTo(secondKey) > 0) {
            firstKey = parentKey;
            secondKey = jobKey;
        }

        ReentrantLock firstLock = obtainLock(firstKey);
        ReentrantLock secondLock = secondKey != null ? obtainLock(secondKey) : null;

        try {
            firstLock.lock();

            if (secondLock != null) {
                secondLock.lock();
            }

            // Work with the latest store instance under locks
            Store lockedStore = cache.get(getStoreKey(), Store::new);

            TaskExecution removedTaskExecution = Objects.requireNonNull(lockedStore).byId.remove(id);

            if (removedTaskExecution != null) {
                // Remove from the job list
                List<TaskExecution> jobLTaskExecution = lockedStore.getJobTaskExecutions(
                    existingTaskExecution.getJobId());

                jobLTaskExecution.removeIf(taskExecution -> Objects.equals(taskExecution.getId(), id));

                // Remove from the parent list if applicable
                if (existingTaskExecution.getParentId() != null) {
                    List<TaskExecution> parentTaskExecutions = lockedStore.getParentTaskExecutions(
                        existingTaskExecution.getParentId());

                    parentTaskExecutions.removeIf(te -> Objects.equals(te.getId(), id));
                }

                // Persist back to cache for explicitness
                cache.put(getStoreKey(), lockedStore);
            }
        } finally {
            if (secondLock != null) {
                releaseLock(secondKey, secondLock);
            }

            releaseLock(firstKey, firstLock);
        }
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByTaskNumber(long jobId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));

        Store store = Objects.requireNonNull(cache.get(getStoreKey(), Store::new));

        String key = getJobLockKey(jobId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            Comparator<Object> comparable = Comparators.comparable();

            return new ArrayList<>(store.getJobTaskExecutions(jobId))
                .stream()
                .sorted((o1, o2) -> comparable.compare(o1.getTaskNumber(), o2.getTaskNumber()))
                .toList();
        } finally {
            releaseLock(key, lock);
        }
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByCreatedDate(long jobId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));

        Store store = Objects.requireNonNull(cache.get(getStoreKey(), Store::new));

        String key = getJobLockKey(jobId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            // Return in insertion order (created date order), same semantics as before
            return new ArrayList<>(store.getJobTaskExecutions(jobId));
        } finally {
            releaseLock(key, lock);
        }
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByIdDesc(long jobId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));

        Store store = Objects.requireNonNull(cache.get(getStoreKey(), Store::new));

        String key = getJobLockKey(jobId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            return new ArrayList<>(store.getJobTaskExecutions(jobId))
                .stream()
                .sorted((t1, t2) -> {
                    Long id1 = Objects.requireNonNull(t1.getId());
                    Long id2 = Objects.requireNonNull(t2.getId());

                    return Long.compare(id2, id1);
                })
                .toList();
        } finally {
            releaseLock(key, lock);
        }
    }

    @Override
    public List<TaskExecution> findAllByParentIdOrderByTaskNumber(long parentId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));

        Store store = Objects.requireNonNull(cache.get(getStoreKey(), Store::new));

        String key = getParentLockKey(parentId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            Comparator<Object> comparable = Comparators.comparable();

            return new ArrayList<>(store.getParentTaskExecutions(parentId))
                .stream()
                .sorted((o1, o2) -> comparable.compare(o1.getTaskNumber(), o2.getTaskNumber()))
                .toList();
        } finally {
            releaseLock(key, lock);
        }
    }

    @Override
    public Optional<TaskExecution> findById(long id) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));
        Store store = cache.get(getStoreKey(), Store::new);

        return Optional.ofNullable(Objects.requireNonNull(store).byId.get(id));
    }

    @Override
    public Optional<TaskExecution> findByIdForUpdate(long id) {
        return findById(id);
    }

    @Override
    public Optional<TaskExecution> findLastByJobId(long jobId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(TASK_EXECUTION_CACHE));

        Store store = Objects.requireNonNull(cache.get(getStoreKey(), Store::new));

        String key = getJobLockKey(jobId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            List<TaskExecution> taskExecutions = new ArrayList<>(store.getJobTaskExecutions(jobId));

            if (taskExecutions.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(taskExecutions.getLast());
            }
        } finally {
            releaseLock(key, lock);
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

            Store store = Objects.requireNonNull(cache.get(getStoreKey(), Store::new));

            String jobKey = getJobLockKey(Objects.requireNonNull(taskExecution.getJobId()));
            String parentKey = getParentLockKey(taskExecution.getParentId());

            // Acquire locks in a deterministic order to avoid deadlocks
            String firstKey = jobKey;
            String secondKey = parentKey;

            if (secondKey != null && firstKey.compareTo(secondKey) > 0) {
                firstKey = parentKey;
                secondKey = jobKey;
            }

            ReentrantLock firstLock = obtainLock(firstKey);
            ReentrantLock secondLock = secondKey != null ? obtainLock(secondKey) : null;

            try {
                firstLock.lock();

                if (secondLock != null) {
                    secondLock.lock();
                }

                // by id
                store.byId.put(Objects.requireNonNull(clonedTaskExecution.getId()), clonedTaskExecution);

                // by job id (preserve insertion order for newly created entries; replace on update by id)
                List<TaskExecution> jobTaskExecutions = store.getJobTaskExecutions(taskExecution.getJobId());

                int index = -1;

                for (int i = 0; i < jobTaskExecutions.size(); i++) {
                    TaskExecution existingTaskExecution = jobTaskExecutions.get(i);

                    if (Objects.equals(existingTaskExecution.getId(), clonedTaskExecution.getId())) {
                        index = i;

                        break;
                    }
                }
                if (index >= 0) {
                    jobTaskExecutions.set(index, clonedTaskExecution);
                } else {
                    jobTaskExecutions.add(clonedTaskExecution);
                }

                // by parent id (same semantics)
                if (taskExecution.getParentId() != null) {
                    List<TaskExecution> parentTaskExecutions = store.getParentTaskExecutions(
                        taskExecution.getParentId());

                    int pIndex = -1;

                    for (int i = 0; i < parentTaskExecutions.size(); i++) {
                        TaskExecution existingTaskExecution = parentTaskExecutions.get(i);

                        if (Objects.equals(existingTaskExecution.getId(), clonedTaskExecution.getId())) {
                            pIndex = i;

                            break;
                        }
                    }

                    if (pIndex >= 0) {
                        parentTaskExecutions.set(pIndex, clonedTaskExecution);
                    } else {
                        parentTaskExecutions.add(clonedTaskExecution);
                    }
                }

                // persist the updated store back into cache (not strictly necessary, but keeps semantics explicit)
                cache.put(getStoreKey(), store);
            } finally {
                if (secondLock != null) {
                    releaseLock(secondKey, secondLock);
                }

                releaseLock(firstKey, firstLock);
            }
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        return taskExecution;
    }

    private static String getJobLockKey(long jobId) {
        return getStoreKey() + ":job:" + jobId;
    }

    private static String getStoreKey() {
        return TenantCacheKeyUtils.getKey("store");
    }

    private static String getParentLockKey(Long parentId) {
        return parentId == null ? null : getStoreKey() + ":parent:" + parentId;
    }

    private static ReentrantLock obtainLock(String key) {
        return LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
    }

    private static void releaseLock(String key, ReentrantLock lock) {
        lock.unlock();

        if (!lock.isLocked() && !lock.hasQueuedThreads()) {
            LOCKS.remove(key, lock);
        }
    }

    private static final class Store {

        // by id
        private final ConcurrentHashMap<Long, TaskExecution> byId = new ConcurrentHashMap<>();
        // by job id, maintains insertion order (created date order)
        private final ConcurrentHashMap<Long, List<TaskExecution>> byJobId = new ConcurrentHashMap<>();
        // by parent id, maintains insertion order (created date order)
        private final ConcurrentHashMap<Long, List<TaskExecution>> byParentId = new ConcurrentHashMap<>();

        List<TaskExecution> getJobTaskExecutions(long jobId) {
            return byJobId.computeIfAbsent(jobId, k -> new ArrayList<>());
        }

        List<TaskExecution> getParentTaskExecutions(long parentId) {
            return byParentId.computeIfAbsent(parentId, k -> new ArrayList<>());
        }
    }
}
