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
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface JobService {

    Job create(@NonNull JobParametersDTO jobParametersDTO, @NonNull Workflow workflow);

    void deleteJob(long id);

    Optional<Job> fetchLastJob();

    Optional<Job> fetchLastWorkflowJob(@NonNull String workflowId);

    Job getJob(long id);

    Page<Job> getJobsPage(int pageNumber);

    Job getTaskExecutionJob(long taskExecutionId);

    Job resumeToStatusStarted(long id);

    Job setStatusToCompleted(long id);

    Job setStatusToStarted(long id);

    Job setStatusToStopped(long id);

    Job update(@NonNull Job job);
}
