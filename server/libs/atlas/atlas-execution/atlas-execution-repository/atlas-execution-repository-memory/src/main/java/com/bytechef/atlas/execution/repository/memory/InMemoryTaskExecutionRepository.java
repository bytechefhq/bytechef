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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import org.springframework.util.comparator.Comparators;

/**
 * @author Arik Cohen
 * @author Igor Beslic
 * @since Feb, 21 2020
 */
public class InMemoryTaskExecutionRepository implements TaskExecutionRepository {

    private final ConcurrentHashMap<String, Store> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public void deleteById(long id) {
        String storeKey = getStoreKey();

        Store store = Objects.requireNonNull(cache.computeIfAbsent(storeKey, (key1) -> new Store()));

        TaskExecution existingTaskExecution = store.byId.get(id);

        if (existingTaskExecution == null) {
            return;
        }

        String taskExecutionKey = getTaskExecutionLockKey(id);
        String jobKey = getJobLockKey(Objects.requireNonNull(existingTaskExecution.getJobId()));
        String parentKey = getParentLockKey(existingTaskExecution.getParentId());

        List<String> keys = new ArrayList<>();

        keys.add(taskExecutionKey);

        List<String> otherKeys = new ArrayList<>();

        otherKeys.add(jobKey);

        if (parentKey != null) {
            otherKeys.add(parentKey);
        }

        otherKeys.sort(String::compareTo);

        keys.addAll(otherKeys);

        List<String> acquiredKeys = new ArrayList<>();

        for (String key : keys) {
            ReentrantLock lock = obtainLock(key);

            if (!lock.isHeldByCurrentThread()) {
                lock.lock();

                acquiredKeys.add(key);
            }
        }

        try {
            // Work with the latest store instance under locks
            Store lockedStore = cache.computeIfAbsent(storeKey, (key1) -> new Store());

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
            }
        } finally {
            for (int i = acquiredKeys.size() - 1; i >= 0; i--) {
                String key = acquiredKeys.get(i);
                ReentrantLock lock = locks.get(key);

                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    @Override
    public List<TaskExecution> findAll() {
        return new ArrayList<>(cache.values())
            .stream()
            .flatMap(store -> Stream.of(store.byId.values()))
            .flatMap(Collection::stream)
            .toList();
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByTaskNumber(long jobId) {
        Store store = Objects.requireNonNull(cache.computeIfAbsent(getStoreKey(), (key1) -> new Store()));

        String key = getJobLockKey(jobId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            Comparator<Object> comparable = Comparators.comparable();

            return new ArrayList<>(store.getJobTaskExecutions(jobId))
                .stream()
                .map(this::clone)
                .sorted((o1, o2) -> comparable.compare(o1.getTaskNumber(), o2.getTaskNumber()))
                .toList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByCreatedDate(long jobId) {
        Store store = Objects.requireNonNull(cache.computeIfAbsent(getStoreKey(), (key1) -> new Store()));

        String key = getJobLockKey(jobId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            // Return in insertion order (created date order), same semantics as before
            return new ArrayList<>(store.getJobTaskExecutions(jobId))
                .stream()
                .map(this::clone)
                .toList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByIdDesc(long jobId) {
        Store store = Objects.requireNonNull(cache.computeIfAbsent(getStoreKey(), (key1) -> new Store()));

        String key = getJobLockKey(jobId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            return new ArrayList<>(store.getJobTaskExecutions(jobId))
                .stream()
                .map(this::clone)
                .sorted((t1, t2) -> {
                    Long id1 = Objects.requireNonNull(t1.getId());
                    Long id2 = Objects.requireNonNull(t2.getId());

                    return Long.compare(id2, id1);
                })
                .toList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TaskExecution> findAllByParentIdOrderByTaskNumber(long parentId) {
        Store store = Objects.requireNonNull(cache.computeIfAbsent(getStoreKey(), (key1) -> new Store()));

        String key = getParentLockKey(parentId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            Comparator<Object> comparable = Comparators.comparable();

            return new ArrayList<>(store.getParentTaskExecutions(parentId))
                .stream()
                .map(this::clone)
                .sorted((o1, o2) -> comparable.compare(o1.getTaskNumber(), o2.getTaskNumber()))
                .toList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<TaskExecution> findById(long id) {
        Store store = cache.computeIfAbsent(getStoreKey(), (key1) -> new Store());

        return Optional.ofNullable(Objects.requireNonNull(store).byId.get(id))
            .map(this::clone);
    }

    @Override
    public Optional<TaskExecution> findByIdForUpdate(long id) {
        String key = getTaskExecutionLockKey(id);

        ReentrantLock lock = obtainLock(key);

        if (!lock.isHeldByCurrentThread()) {
            lock.lock();
        }

        return findById(id);
    }

    @Override
    public void unlockForUpdate(long id) {
        String key = getTaskExecutionLockKey(id);

        ReentrantLock lock = locks.get(key);

        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public Optional<TaskExecution> findLastByJobId(long jobId) {
        Store store = Objects.requireNonNull(cache.computeIfAbsent(getStoreKey(), (key1) -> new Store()));

        String key = getJobLockKey(jobId);

        ReentrantLock lock = obtainLock(key);

        try {
            lock.lock();

            List<TaskExecution> taskExecutions = new ArrayList<>(store.getJobTaskExecutions(jobId));

            if (taskExecutions.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(clone(taskExecutions.getLast()));
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TaskExecution save(TaskExecution taskExecution) {
        if (taskExecution.isNew()) {
            taskExecution.setId(Math.abs(Math.max(RandomUtils.nextLong(), Long.MIN_VALUE + 1)));
        }

        TaskExecution clonedTaskExecution = clone(taskExecution);

        String storeKey = getStoreKey();

        Store store = Objects.requireNonNull(cache.computeIfAbsent(storeKey, (key1) -> new Store()));

        String taskExecutionKey = getTaskExecutionLockKey(Objects.requireNonNull(taskExecution.getId()));
        String jobKey = getJobLockKey(Objects.requireNonNull(taskExecution.getJobId()));
        String parentKey = getParentLockKey(taskExecution.getParentId());

        // Acquire locks in a deterministic order to avoid deadlocks
        List<String> keys = new ArrayList<>();

        keys.add(taskExecutionKey);

        List<String> otherKeys = new ArrayList<>();

        otherKeys.add(jobKey);

        if (parentKey != null) {
            otherKeys.add(parentKey);
        }

        otherKeys.sort(String::compareTo);

        keys.addAll(otherKeys);

        List<String> acquiredKeys = new ArrayList<>();

        for (String key : keys) {
            ReentrantLock lock = obtainLock(key);

            if (!lock.isHeldByCurrentThread()) {
                lock.lock();

                acquiredKeys.add(key);
            }
        }

        try {
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
        } finally {
            for (int i = acquiredKeys.size() - 1; i >= 0; i--) {
                String key = acquiredKeys.get(i);

                ReentrantLock lock = locks.get(key);

                if (lock != null) {
                    lock.unlock();
                }
            }
        }

        return taskExecution;
    }

    private TaskExecution clone(TaskExecution taskExecution) {
        try {
            return taskExecution.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
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

    private static String getTaskExecutionLockKey(long id) {
        return getStoreKey() + ":taskExecution:" + id;
    }

    private ReentrantLock obtainLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
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
