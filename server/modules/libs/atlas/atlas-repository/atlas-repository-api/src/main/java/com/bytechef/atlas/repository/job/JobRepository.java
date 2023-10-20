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

package com.bytechef.atlas.repository.job;

import com.bytechef.atlas.data.Page;
import com.bytechef.atlas.job.JobSummary;
import com.bytechef.atlas.job.domain.Job;
import java.util.List;
import java.util.Optional;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public interface JobRepository {
    int countCompletedJobsToday();

    int countCompletedJobsYesterday();

    int countRunningJobs();

    void create(Job aJob);

    void delete(String id);

    List<Job> findAll();

    Page<JobSummary> findAllJobSummaries(int pageNumber);

    Job findById(String aId);

    Optional<Job> findLatestJob();

    Job findByTaskExecutionId(String taskId);

    Job merge(Job job);
}
