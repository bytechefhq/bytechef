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

package com.bytechef.atlas.execution.repository;

import com.bytechef.atlas.execution.domain.Job;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public interface JobRepository {

    long count(String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds);

    int countCompletedJobsToday();

    int countCompletedJobsYesterday();

    int countRunningJobs();

    void deleteById(Long id);

    Iterable<Job> findAll();

    Page<Job> findAll(Pageable pageable);

    List<Job> findAll(
        String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds);

    Page<Job> findAll(
        String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds,
        Pageable pageable);

    Optional<Job> findById(Long id);

    Job findByTaskExecutionId(Long taskExecutionId);

    Optional<Job> findLatestJob();

    Job save(Job job);
}
