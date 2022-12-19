
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

package com.bytechef.atlas.service;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.dto.JobParameters;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public interface JobService {

    Job create(JobParameters jobParameters);

    List<Job> getJobs();

    Page<Job> getJobs(int pageNumber);

    Job getJob(String id);

    Optional<Job> fetchLatestJob();

    Job getTaskExecutionJob(String taskExecutionId);

    Job resume(String jobId);

    Job start(String jobId);

    Job stop(String jobId);

    Job update(Job job);
}
