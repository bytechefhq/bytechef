
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

package com.bytechef.atlas.repository.memory;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.repository.JobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Ivica Cardic
 */
public class InMemoryJobRepository implements JobRepository {

    private static final Random RANDOM = new Random();

    private final InMemoryTaskExecutionRepository inMemoryTaskExecutionRepository;
    private final Map<Long, Job> jobs = new HashMap<>();
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
        throw new UnsupportedOperationException();
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
    public Page<Job> findAll(
        String status, LocalDateTime startTime, LocalDateTime endTime, Long workflowId, Pageable pageable) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> findById(Long id) {
        Job job = jobs.get(id);

        return Optional.ofNullable(job);
    }

    @Override
    public Optional<Job> findLatestJob() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job findByTaskExecutionId(Long taskExecutionId) {
        TaskExecution taskExecution = inMemoryTaskExecutionRepository.findById(taskExecutionId)
            .orElseThrow(IllegalArgumentException::new);

        return jobs.values()
            .stream()
            .filter(job -> Objects.equals(job.getId(), taskExecution.getJobId()))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public Job save(Job job) {
        if (job.isNew()) {
            job.setId(RANDOM.nextLong());
        }

        try {
            // Emulate identical behaviour when storing in db by serialization and deserialization

            jobs.put(job.getId(), objectMapper.readValue(objectMapper.writeValueAsString(job), Job.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return job;
    }
}
