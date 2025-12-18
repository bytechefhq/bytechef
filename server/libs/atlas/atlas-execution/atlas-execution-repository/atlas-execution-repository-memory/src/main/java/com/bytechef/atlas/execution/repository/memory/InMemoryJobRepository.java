/*
 * Copyright 2025 ByteChef
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

package com.bytechef.atlas.execution.repository.memory;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Ivica Cardic
 */
public class InMemoryJobRepository implements JobRepository {

    private final ConcurrentHashMap<String, Job> cache = new ConcurrentHashMap<>();
    private final InMemoryTaskExecutionRepository inMemoryTaskExecutionRepository;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public InMemoryJobRepository(
        InMemoryTaskExecutionRepository inMemoryTaskExecutionRepository, ObjectMapper objectMapper) {

        this.inMemoryTaskExecutionRepository = inMemoryTaskExecutionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public int countCompletedJobsToday() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int countCompletedJobsYesterday() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int countRunningJobs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteById(Long id) {
        cache.remove(TenantCacheKeyUtils.getKey(id));
    }

    @Override
    public Iterable<Job> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Job> findAll(Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Job> findAllByWorkflowId(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> findById(Long id) {
        return Optional.ofNullable(cache.get(TenantCacheKeyUtils.getKey(id)));
    }

    @Override
    public Optional<Job> findLastJob() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> findTop1ByWorkflowIdOrderByIdDesc(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> findTop1ByWorkflowIdInOrderByIdDesc(List<String> workflowIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> findByTaskExecutionId(Long taskExecutionId) {
        TaskExecution taskExecution = inMemoryTaskExecutionRepository.findById(taskExecutionId)
            .orElseThrow(() -> new IllegalArgumentException("TaskExecution not found: " + taskExecutionId));

        return Optional.ofNullable(cache.get(TenantCacheKeyUtils.getKey(taskExecution.getJobId())));
    }

    @Override
    public Job save(Job job) {
        if (job.isNew()) {
            job.setId(Math.abs(Math.max(RandomUtils.nextLong(), Long.MIN_VALUE + 1)));
        }

        try {
            // Emulate identical behaviour when storing in db by serialization and deserialization

            cache.put(
                TenantCacheKeyUtils.getKey(job.getId()),
                objectMapper.readValue(objectMapper.writeValueAsString(job), Job.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return job;
    }
}
