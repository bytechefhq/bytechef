/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.atlas.execution.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public interface JobService {

    Job create(JobParameters jobParameters, Workflow workflow);

    Optional<Job> fetchLastJob();

    Job getJob(long id);

    Page<Job> getJobsPage(int pageNumber);

    Page<Job> getJobsPage(
        String status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, int type,
        List<String> workflowIds, int pageNumber);

    Job getTaskExecutionJob(long taskExecutionId);

    Job resumeToStatusStarted(long id);

    Job setStatusToStarted(long id);

    Job setStatusToStopped(long id);

    Job update(Job job);
}
