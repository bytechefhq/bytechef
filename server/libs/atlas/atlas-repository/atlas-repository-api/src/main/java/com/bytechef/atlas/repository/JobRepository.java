
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

package com.bytechef.atlas.repository;

import com.bytechef.atlas.domain.Job;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public interface JobRepository {
    int DEFAULT_PAGE_SIZE = 20;

    int countCompletedJobsToday();

    int countCompletedJobsYesterday();

    int countRunningJobs();

    void deleteById(Long id);

    Iterable<Job> findAll();

    Page<Job> findAll(Pageable pageable);

    Optional<Job> findById(Long id);

    Optional<Job> findLatestJob();

    Job findByTaskExecutionId(Long taskExecutionId);

    Job save(Job job);
}
